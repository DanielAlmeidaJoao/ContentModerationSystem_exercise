package org.contentModeration;

import com.opencsv.CSVReader;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ContentModeratiion {

    public static void main(String [] args) throws Exception{
        ContentModeratiion contentModeratiion = new ContentModeratiion(4,"src/main/resources/MOCK_DATA.csv");
        contentModeratiion.startThreadWorkers();
    }

    private Map<String,UserStats> commentsPerUser;
    private Set<Integer> processedMessages;
    private int numberOfWorkers;
    private ScoringService scoringService;
    private TranslationService translationService;
    private String fileName;
    private ReentrantReadWriteLock writeLock;
    public ContentModeratiion(int numberOfWorkers, String fileName){
        commentsPerUser = new ConcurrentHashMap<>();
        processedMessages = new HashSet<>();
        this.numberOfWorkers = numberOfWorkers;
        scoringService = new ScoringService();
        translationService = new TranslationService();
        this.fileName = fileName;
        writeLock = new ReentrantReadWriteLock();
    }
    public void startThreadWorkers() throws Exception{
        long startTime = System.currentTimeMillis();
        final File file = new File(fileName);
        long chunkSize = file.length() / numberOfWorkers;
        final ExecutorService executorService = Executors.newCachedThreadPool();
        BlockingQueue<Long> blockingQueue = new LinkedBlockingQueue<>();
        for (int i = 0; i < numberOfWorkers; i++) {
            final long offset = i * chunkSize;
            final long endOffSet = i == numberOfWorkers - 1 ? file.length() : offset + chunkSize;
            final int workerId = i;
            executorService.submit(()->{
                readFiles(executorService, fileName,offset,endOffSet, blockingQueue, workerId);
            });
        }
        int threadsFinished = 0;
        long threadId;
        while ( (threadId = blockingQueue.take()) > 0){
            threadsFinished++;
            if (threadsFinished == numberOfWorkers){
                break;
            }
        }
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
    public void readFiles(final ExecutorService executorService, final String filePath,final long offset, final long endOffset, BlockingQueue<Long> waitingThreads, final int workerId) {
        System.out.println("Starting thread "+Thread.currentThread().getId());

        BlockingQueue<Integer> localBlockingQueue = new LinkedBlockingQueue<>();

        try (RandomAccessFile reader = new RandomAccessFile(filePath,"r")) {
            String line;
            if (offset > 0) {
                reader.seek(offset-1);
            }

            int totalProcessed = 0;
            long localOffset = offset;
            while (localOffset < endOffset && (line = reader.readLine() ) != null ) {
                localOffset += line.length();
                String [] columns = line.replaceFirst(",","|").split("\\|");
                if (columns.length != 2){
                    continue;
                }
                final String userName = columns[0];
                final String comment = columns[1];
                writeLock.writeLock().lock();
                boolean contains = processedMessages.add(comment.hashCode());
                writeLock.writeLock().unlock();
                if (contains){
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
            System.out.println("Lines read "+totalProcessed);
            if (totalProcessed == 0){
                waitingThreads.add(Thread.currentThread().getId());
            } else {
                while (localBlockingQueue.take() > 0 ){
                    totalProcessed--;
                    if (totalProcessed==0){
                        waitingThreads.add(Thread.currentThread().getId());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
