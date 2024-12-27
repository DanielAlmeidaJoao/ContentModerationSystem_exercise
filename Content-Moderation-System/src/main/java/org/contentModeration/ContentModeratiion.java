package org.contentModeration;

import com.opencsv.CSVReader;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ContentModeratiion {

    public static void main(String [] args) throws Exception{
        ContentModeratiion contentModeratiion = new ContentModeratiion(4,"src/main/resources/input.csv");
        contentModeratiion.startThreadWorkers();
    }

    private Map<String,UserStats> commentsPerUser;
    private Set<Integer> processedMessages;
    private int numberOfWorkers;
    private ScoringService scoringService;
    private TranslationService translationService;
    private String fileName;
    public ContentModeratiion(int numberOfWorkers, String fileName){
        commentsPerUser = new ConcurrentHashMap<>();
        processedMessages = new HashSet<>();
        this.numberOfWorkers = numberOfWorkers;
        scoringService = new ScoringService();
        translationService = new TranslationService();
        this.fileName = fileName;
    }
    public void startThreadWorkers() throws Exception{
        long startTime = System.currentTimeMillis();
        final File file = new File(fileName);
        final ExecutorService executorService = Executors.newCachedThreadPool();
        BlockingQueue<Long> blockingQueue = new LinkedBlockingQueue<>(1);
        executorService.submit(()->{
            readFiles(executorService, fileName, blockingQueue,1);
        });

        blockingQueue.take();

        System.out.println("Finished");
        File output = new File("resultado_"+System.currentTimeMillis()+".csv");
        try (FileWriter fileWriter = new FileWriter(output)) {
            for (Map.Entry<String, UserStats> stringUserStatsEntry : commentsPerUser.entrySet()) {
                UserStats stats = stringUserStatsEntry.getValue();
                fileWriter.write(String.format("%S,%d,%f\n", stringUserStatsEntry.getKey(), stats.getTotalMessages(), stats.getAverageScore()));
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Finished! Elapsed: "+(endTime - startTime));
            executorService.shutdownNow();
        }
    }
    public void readFiles(final ExecutorService executorService, final String filePath, BlockingQueue<Long> waitingThreads, final int workerId) {
        System.out.println("Starting thread "+Thread.currentThread().getId());

        BlockingQueue<Integer> localBlockingQueue = new LinkedBlockingQueue<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int totalProcessed = 0;
            while ((line = reader.readLine() ) != null ) {
                String [] columns = line.split(",");
                final String userName = columns[0];
                final String comment = columns[1];
                if (processedMessages.add(comment.hashCode())){
                    System.out.println(workerId+" ||||| "+comment);
                    UserStats userStats = commentsPerUser.computeIfAbsent(userName, key -> new UserStats());
                    totalProcessed++;

                    executorService.execute(()->{
                        String translatedText = translationService.TranslateToEnglish(comment);

                        executorService.execute(()->{
                            float score = scoringService.WhatIsTheScore(translatedText);
                            userStats.addScore(score);
                            localBlockingQueue.add(1);
                        });
                    });
                }
            }
            System.out.println(workerId + " 88 -- 99 "+totalProcessed);
            if (totalProcessed == 0){
                waitingThreads.add(Thread.currentThread().getId());
            } else {
                while (localBlockingQueue.take() > 0 ){
                    System.out.println(workerId+" > 4444 id ");
                    totalProcessed--;
                    if (totalProcessed==0){
                        waitingThreads.add(Thread.currentThread().getId());
                        break;
                    }
                }
            }
            System.out.println(workerId+" > END id ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
