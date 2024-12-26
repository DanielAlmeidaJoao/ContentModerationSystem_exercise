package org.contentModeration;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContentModeratiion {

    Map<String,StringBuffer> commentsPerUser;
    Set<Integer> processedMessages;
    int numberOfThreads;
    List<Thread> threadList;
    ScoringService scoringService;
    TranslationService translationService;
    public ContentModeratiion(int numberOfThreads){
        commentsPerUser = new ConcurrentHashMap<>();
        processedMessages = new HashSet<>();
        this.numberOfThreads = numberOfThreads;
        threadList = new ArrayList<>(numberOfThreads);
        scoringService = new ScoringService();
        translationService = new TranslationService();

    }
    public void startThreadWorkers(){
        for (int i = 0; i < numberOfThreads; i++) {
            Thread worker = new Thread(new Runnable() {
                @Override
                public void run() {

                }
            });
            threadList.add(worker);
            worker.start();
        }
    }
    public void readFiles(int threadSequence, int offset, int len) throws FileNotFoundException {
        ExecutorService executorService = Executors.newCachedThreadPool();

        String filePath = "path/to/your/file.csv";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.skip(threadSequence*len);
            while ((line = br.readLine()) != null) {
                String[] values = line.split(","); // Split based on your delimiter
                String userName = values[0];
                String comment = values[1];
                if (processedMessages.add(comment.hashCode())){
                    StringBuffer stringBuffer = commentsPerUser.computeIfAbsent(userName, key -> new StringBuffer());
                    stringBuffer.append("\n").append(comment);
                    executorService.execute(()->{
                        final String user = userName;
                        String translatedText = translationService.TranslateToEnglish(comment);
                        executorService.execute(()->{
                            float score = scoringService.WhatIsTheScore(translatedText);

                        });
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
