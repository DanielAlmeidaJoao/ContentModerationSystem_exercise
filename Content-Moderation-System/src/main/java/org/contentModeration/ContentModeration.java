package org.contentModeration;

import com.opencsv.CSVParser;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ContentModeration {
    public static int MAX_NUMBER_THREADS_PER_PROCESS = 30499;
    private Map<String, UserStats> commentsPerUser;
    private ReentrantReadWriteLock editUserStatsLock;
    private ReentrantReadWriteLock createUserLock;
    private CSVParser csvParser;

    private ScoringService scoringService;
    private TranslationService translationService;

    private String inputPath;
    private String outputPath;

    private int numberOfWorkers;
    private int numberOfThreads;



    public ContentModeration(TranslationService translationService, ScoringService scoringService, int numberOfWorkers, int numberOfThreads, String inputPath, String outputPath) {
        commentsPerUser = new HashMap<>();
        editUserStatsLock = new ReentrantReadWriteLock();
        createUserLock = new ReentrantReadWriteLock();
        csvParser = new CSVParser();

        this.scoringService = scoringService;
        this.translationService = translationService;

        this.numberOfWorkers = numberOfWorkers;
        this.numberOfThreads = numberOfThreads;

        if (numberOfThreads < (numberOfWorkers + 1)) {
            throw new RuntimeException("Number of threads must be greater than number of workers");
        }

        this.inputPath = inputPath;
        this.outputPath = outputPath;

    }

    public void startThreadWorkers() throws Exception {
        long startTime = System.currentTimeMillis();

        final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        BlockingQueue<Long> blockingQueue = new LinkedBlockingQueue<>();

        RandomAccessFile randomAccessFile = new RandomAccessFile(inputPath, "r");
        long chunkSize = randomAccessFile.length() / numberOfWorkers;
        long nextStart = 0;
        for (int i = 0; i < numberOfWorkers; i++) {
            long currentStart = nextStart;
            long endOffSet;
            if (i == numberOfWorkers - 1) {
                endOffSet = randomAccessFile.length();
            } else {
                long pseudoEndOffSet = currentStart + chunkSize;
                endOffSet = getBeginningOfNextLineOffset(pseudoEndOffSet, randomAccessFile);
            }

            nextStart = endOffSet;

            executorService.submit(() ->
                readCsvAndCalculateScores(executorService, currentStart, endOffSet, blockingQueue)
            );
        }
        randomAccessFile.close();

        waitForScoresToBeCalculated(blockingQueue);
        writeFinalResultsToFile(startTime);
        executorService.shutdownNow();

    }

    private void readCsvAndCalculateScores(final ExecutorService executorService, final long offset, final long endOffset, BlockingQueue<Long> waitingThreads) {

        try (RandomAccessFile reader = new RandomAccessFile(inputPath, "r")) {
            String line;
            reader.seek(offset);
            boolean processedAtLeastOne = false;
            final AtomicInteger totalProcessed = new AtomicInteger(0);
            final AtomicBoolean finishedProcessing = new AtomicBoolean(false);

            while (reader.getFilePointer() < endOffset && (line = reader.readLine()) != null) {
                String[] columns = csvParser.parseLine(line);
                final String userName = columns[0];
                final String comment = columns[1];

                createUserLock.writeLock().lock();
                UserStats userStats = commentsPerUser.computeIfAbsent(userName, key -> new UserStats());
                boolean unprocessedComment = userStats.messages.add(comment);
                createUserLock.writeLock().unlock();

                if (unprocessedComment) {
                    processedAtLeastOne = true;
                    totalProcessed.incrementAndGet();

                    executorService.execute(() -> {
                        String translatedText = translationService.TranslateToEnglish(comment);

                        executorService.execute(() -> {
                            float score = scoringService.WhatIsTheScore(translatedText);

                            editUserStatsLock.writeLock().lock();
                            userStats.addScore(score);
                            editUserStatsLock.writeLock().unlock();

                            if (totalProcessed.decrementAndGet() == 0 && finishedProcessing.get()) {
                                waitingThreads.add(Thread.currentThread().getId());
                            }
                        });
                    });
                }
            }
            finishedProcessing.set(true);

            if (totalProcessed.get() == 0 && !processedAtLeastOne) {
                waitingThreads.add(Thread.currentThread().getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void waitForScoresToBeCalculated(BlockingQueue<Long> blockingQueue) throws Exception{
        int threadsFinished = 0;
        while (blockingQueue.take() > 0) {
            threadsFinished++;
            if (threadsFinished == numberOfWorkers) {
                break;
            }
        }
    }

    private void writeFinalResultsToFile(long startTime) throws Exception{
        File output = new File(outputPath);
        try (FileWriter fileWriter = new FileWriter(output)) {
            for (Map.Entry<String, UserStats> stringUserStatsEntry : commentsPerUser.entrySet()) {
                UserStats stats = stringUserStatsEntry.getValue();
                fileWriter.write(String.format("%s,%d,%f\n", stringUserStatsEntry.getKey(), stats.getTotalMessages(), stats.getAverageScore()));
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Total Lines Produced: "+ commentsPerUser.size());
            System.out.println("Elapsed Time (ms): " + (endTime - startTime));
        }
    }

    public static long getBeginningOfNextLineOffset(long offset, RandomAccessFile randomAccessFile) throws Exception {
        long mark = randomAccessFile.getFilePointer();
        randomAccessFile.seek(offset);
        randomAccessFile.readLine();
        long result = randomAccessFile.getFilePointer();
        randomAccessFile.seek(mark);
        return result;
    }

}
