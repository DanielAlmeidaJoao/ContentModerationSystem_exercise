package org.contentModeration;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ContentModeration {
    public static int MAX_NUMBER_THREADS_PER_PROCESS = 30499;
    private Map<String,UserStats> commentsPerUser;
    private int numberOfWorkers;
    private ScoringService scoringService;
    private TranslationService translationService;
    private String fileName;
    private ReentrantReadWriteLock editUserStatsLock;
    private ReentrantReadWriteLock createUserLock;

    public ContentModeration(int numberOfWorkers, String fileName){
        commentsPerUser = new HashMap<>();
        this.numberOfWorkers = numberOfWorkers;
        scoringService = new ScoringService();
        translationService = new TranslationService();
        this.fileName = fileName;
        editUserStatsLock = new ReentrantReadWriteLock();
        createUserLock = new ReentrantReadWriteLock();

    }
    public void startThreadWorkers() throws Exception{
        long startTime = System.currentTimeMillis();
        final File file = new File(fileName);
        long chunkSize = file.length() / numberOfWorkers;
        final ExecutorService executorService = Executors.newFixedThreadPool(9000);

        BlockingQueue<Long> blockingQueue = new LinkedBlockingQueue<>();
        for (int i = 0; i < numberOfWorkers; i++) {
            final long offset = i * chunkSize;
            final long endOffSet = i == numberOfWorkers - 1 ? file.length() : offset + chunkSize;
            executorService.submit(()->{
                readFiles(executorService, fileName,offset,endOffSet, blockingQueue);
            });
        }
        int threadsFinished = 0;
        while ( blockingQueue.take() > 0){
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
            long endTime = System.currentTimeMillis();
            System.out.println("Finished! Elapsed: "+(endTime - startTime)+" ... "+commentsPerUser.size());
            executorService.shutdownNow();
        }
    }
    public void readFiles(final ExecutorService executorService, final String filePath,final long offset, final long endOffset, BlockingQueue<Long> waitingThreads) {
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
                if (columns.length == 2){
                    final String userName = columns[0];
                    final String comment = columns[1];

                    createUserLock.writeLock().lock();
                    UserStats userStats = commentsPerUser.computeIfAbsent(userName, key -> new UserStats());
                    boolean unprocessedComment = userStats.messages.add(comment);
                    createUserLock.writeLock().unlock();

                    if (unprocessedComment){
                        totalProcessed++;

                        executorService.execute(()->{
                            String translatedText = translationService.TranslateToEnglish(comment);

                            executorService.execute(()->{
                                float score = scoringService.WhatIsTheScore(translatedText);
                                editUserStatsLock.writeLock().lock();
                                userStats.addScore(score);
                                editUserStatsLock.writeLock().unlock();
                                localBlockingQueue.add(1);
                            });
                        });
                    }
                }
            }

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
