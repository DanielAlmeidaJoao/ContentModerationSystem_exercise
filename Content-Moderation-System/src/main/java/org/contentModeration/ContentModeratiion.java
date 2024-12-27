package org.contentModeration;

import com.opencsv.CSVReader;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ContentModeratiion {

    Map<String,UserStats> commentsPerUser;
    Set<Integer> processedMessages;
    int numberOfWorkers;
    List<Thread> threadList;
    ScoringService scoringService;
    TranslationService translationService;
    private String fileName;
    public ContentModeratiion(int numberOfWorkers, String fileName){
        commentsPerUser = new ConcurrentHashMap<>();
        processedMessages = new HashSet<>();
        this.numberOfWorkers = numberOfWorkers;
        threadList = new ArrayList<>(numberOfWorkers);
        scoringService = new ScoringService();
        translationService = new TranslationService();
        this.fileName = fileName;
    }
    public void startThreadWorkers() throws Exception{
        final File file = new File(fileName);
        long chunkSize = file.length() / numberOfWorkers;
        final ExecutorService executorService = Executors.newCachedThreadPool();
        BlockingQueue<Long> blockingQueue = new LinkedBlockingQueue<>();
        for (int i = 0; i < numberOfWorkers; i++) {
            final int index = i;
            final long offset = i * chunkSize;
            final long endOffSet = i == numberOfWorkers - 1 ? file.length() : offset + chunkSize;
            executorService.submit(()->{
                readFiles(executorService, fileName,offset,endOffSet, blockingQueue);
            });
        }
        int threadsFinished = 0;
        while (blockingQueue.remove() > 0){
            threadsFinished++;
            if (threadsFinished == numberOfWorkers){
                break;
            }
        }
        File output = new File("resultado_"+System.currentTimeMillis()+".csv");
        try (FileWriter fileWriter = new FileWriter(output)) {
            for (Map.Entry<String, UserStats> stringUserStatsEntry : commentsPerUser.entrySet()) {
                UserStats stats = stringUserStatsEntry.getValue();
                fileWriter.write(String.format("%S,%d,%f\n", stringUserStatsEntry.getKey(), stats.getTotalMessages(), stats.getAverageScore()));
            }
        }
    }
    public void readFiles(final ExecutorService executorService, final String filePath,final long offset, final long endOffset, BlockingQueue<Long> waitingThreads) {

        try (RandomAccessFile reader = new RandomAccessFile(filePath,"r")) {
            String line;
            if (offset > 0) {
                reader.seek(offset-1);
                line = reader.readLine();
                if(line.charAt(line.length()-1) != '\n'){
                    reader.readLine();
                }
            }

            AtomicLong bytesToScored = new AtomicLong(endOffset-offset);

            while (reader.getFilePointer() < endOffset && (line = reader.readLine() ) != null ) {
                String [] columns = line.split(",");
                final String userName = columns[0];
                final String comment = columns[1];
                if (processedMessages.add(comment.hashCode())){
                    UserStats userStats = commentsPerUser.computeIfAbsent(userName, key -> new UserStats());

                    String finalLine = line;
                    executorService.execute(()->{
                        String translatedText = translationService.TranslateToEnglish(comment);

                        executorService.execute(()->{
                            float score = scoringService.WhatIsTheScore(translatedText);
                            userStats.addScore(score);

                            if ( bytesToScored.addAndGet(-finalLine.length()) == 0){
                                waitingThreads.add(Thread.currentThread().getId());
                            }
                        });
                    });
                } else {
                    bytesToScored.addAndGet(-line.length());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
