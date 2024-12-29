package org.contentModeration;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ContentModeration {
    public static int MAX_NUMBER_THREADS_PER_PROCESS = 30499;
    private Map<String,UserStats> commentsPerUser;
    private ReentrantReadWriteLock editUserStatsLock;
    private ReentrantReadWriteLock createUserLock;

    private ScoringService scoringService;
    private TranslationService translationService;

    private String inputPath;
    private String outputPath;

    private int numberOfWorkers;
    private int numberOfThreads;



    public ContentModeration(TranslationService translationService, ScoringService scoringService, int numberOfWorkers, int numberOfThreads, String inputPath, String outputPath){
        commentsPerUser = new HashMap<>();
        editUserStatsLock = new ReentrantReadWriteLock();
        createUserLock = new ReentrantReadWriteLock();

        this.scoringService = scoringService;
        this.translationService = translationService;

        this.numberOfWorkers = numberOfWorkers;
        this.numberOfThreads = numberOfThreads;

        this.inputPath = inputPath;
        this.outputPath = outputPath;

    }
    public static long getBeginningOfNextLineOffset(long offset, RandomAccessFile randomAccessFile) throws Exception{
        long mark = randomAccessFile.getFilePointer();
        randomAccessFile.seek(offset);
        randomAccessFile.readLine();
        long result = randomAccessFile.getFilePointer();
        randomAccessFile.seek(mark);
        return result;
    }
    public void startThreadWorkers() throws Exception{
        long startTime = System.currentTimeMillis();
        RandomAccessFile randomAccessFile = new RandomAccessFile(inputPath,"r");

        long chunkSize = randomAccessFile.length() / numberOfWorkers;
        final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        BlockingQueue<Long> blockingQueue = new LinkedBlockingQueue<>();
        long nextStart = 0;
        for (int i = 0; i < numberOfWorkers; i++) {
            long currentStart = nextStart;
            long endOffSet;
            if (i == numberOfWorkers - 1){
                endOffSet = randomAccessFile.length();
            } else {
                long pseudoEndOffSet = currentStart + chunkSize;
                endOffSet = getBeginningOfNextLineOffset(pseudoEndOffSet,randomAccessFile);
            }

            nextStart = endOffSet;

            executorService.submit(()->{
                readFiles(executorService,currentStart,endOffSet, blockingQueue);
            });
        }
        randomAccessFile.close();
        int threadsFinished = 0;
        while ( blockingQueue.take() > 0){
            threadsFinished++;
            if (threadsFinished == numberOfWorkers){
                break;
            }
        }

        File output = new File(outputPath);
        try (FileWriter fileWriter = new FileWriter(output)) {
            for (Map.Entry<String, UserStats> stringUserStatsEntry : commentsPerUser.entrySet()) {
                UserStats stats = stringUserStatsEntry.getValue();
                fileWriter.write(String.format("%s,%d,%f\n", stringUserStatsEntry.getKey(), stats.getTotalMessages(), stats.getAverageScore()));
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Finished! Elapsed: "+(endTime - startTime)+" ... "+commentsPerUser.size());
            executorService.shutdownNow();
        }
    }
    public void readFiles(final ExecutorService executorService,final long offset, final long endOffset, BlockingQueue<Long> waitingThreads) {
        BlockingQueue<Integer> localBlockingQueue = new LinkedBlockingQueue<>();

        try (RandomAccessFile reader = new RandomAccessFile(inputPath,"r")) {
            String line;
            reader.seek(offset);
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
