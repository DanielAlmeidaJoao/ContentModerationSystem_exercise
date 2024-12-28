package org.contentModeration;

import com.opencsv.bean.util.OpencsvUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentModerationTest {

    @Test
    void verifyModeratedCSV() throws Exception {
        boolean deleteFiles = false;

        int numberOfUsers = 10;
        int numberOfMessages = 20;

        Map<String,TestUserStats> users = createUsers(numberOfUsers);
        String inputTestFile = "./JUNIT_INPUT_"+System.currentTimeMillis() +"_.csv";
        String moderatedTestFile = "./JUNIT_OUTPUT_"+System.currentTimeMillis() +"_.csv";
        generateComments(users,numberOfMessages,inputTestFile);

        TranslationService translationService =mockTranslationService();
        ScoringService scoringService = mockScoringService();

        final int numberOfWorkers = 2;
        final int numberOfThreads = 10;

        ContentModeration contentModeration = new ContentModeration(translationService,scoringService,numberOfWorkers,numberOfThreads,inputTestFile,moderatedTestFile);
        contentModeration.startThreadWorkers();

        //TEST
        int totalLinesRead = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(moderatedTestFile))) {
            String line;
            while ( (line = reader.readLine()) != null){
                totalLinesRead++;

                String [] elements = line.split(",");
                String user = elements[0];
                int totalMessages = Integer.parseInt(elements[1]);
                float score = Float.parseFloat(elements[2]);
                TestUserStats stats = users.get(user);
                assert stats.getTotalMessages() == totalMessages;
                assert stats.getAverageScore() == score;
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        assert users.size() == totalLinesRead;
        if (deleteFiles){
            File finput = new File(inputTestFile);
            finput.delete();
            File fouput = new File(moderatedTestFile);
            fouput.delete();
        }
    }

    private String getUserId(int i){
        return "user_name_"+i;
    }
    private float getTestScore(String message){
        return message.charAt(0);
    }

    private Map<String,TestUserStats> createUsers(int numberOfUsers){
        Map<String,TestUserStats> users = new HashMap<>();
        for (int i = 0; i < numberOfUsers; i++) {
            String userName = getUserId(i);
            users.put(userName,new TestUserStats());
        }
        return users;
    }

    private void generateComments(Map<String,TestUserStats> users, int numberOfMessages, String outPutFile) throws Exception{
        File f = new File(outPutFile);
        FileWriter fileWriter = new FileWriter(f);

        //Generate messages
        for (int i = 0; i < numberOfMessages; i++) {
            String user = getUserId(i%users.size());
            String message = MainAux.encrypt(System.currentTimeMillis()+"_"+i);
            users.get(user).addScore(getTestScore(message));

            //Wriite to input file
            fileWriter.write(user+","+message+"\n");
        }
        fileWriter.flush();
        fileWriter.close();
    }

    private ScoringService mockScoringService(){
         ScoringService scoringService = mock(ScoringService.class);
         when(scoringService.WhatIsTheScore(anyString())).then(invocationOnMock -> {
         String message = invocationOnMock.getArgument(0);
            return getTestScore(message);
         });
         return scoringService;
    }

    private TranslationService mockTranslationService(){
        TranslationService translationService = mock(TranslationService.class);
        when(translationService.TranslateToEnglish(anyString())).then(invocationOnMock -> {
            return invocationOnMock.getArgument(0);
        });
        return translationService;
    }
}