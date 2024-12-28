package org.contentModeration;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentModerationTest {
    // PROGRAM EXECUTION
    TranslationService translationService = mockTranslationService();
    ScoringService scoringService = mockScoringService();

    @Test
    void success_verifyModeratedCSV() throws Exception {
        boolean deleteFiles = true;

        // INPUT DATA GENERATION
        int numberOfUsers = 100;
        int numberOfMessages = 50;

        Map<String,TestUserStats> users = createUsers(numberOfUsers);

        String inputTestFile = "./JUNIT_INPUT_"+System.currentTimeMillis() +"_.csv";
        String moderatedTestFile = "./JUNIT_OUTPUT_"+System.currentTimeMillis() +"_.csv";

        generateComments(users,numberOfMessages,inputTestFile);

        final int numberOfWorkers = 4;
        final int numberOfThreads = 10;

        ContentModeration contentModeration = new ContentModeration(translationService,scoringService,numberOfWorkers,numberOfThreads,inputTestFile,moderatedTestFile);
        contentModeration.startThreadWorkers();

        //VALIDATION
        verifyOutputFileHasExpectedResults(users,moderatedTestFile);

        //CLEAN UP
        deleteFiles(deleteFiles,inputTestFile,moderatedTestFile);
    }

    //@Test
    void fail_one_user_with_wrong_number_of_message() throws Exception {
        boolean deleteFiles = true;

        // INPUT DATA GENERATION
        int numberOfUsers = 10;
        int numberOfMessages = 20;

        Map<String,TestUserStats> users = createUsers(numberOfUsers);

        //WILL FAIL BECAUSE OF THIS LINE!
        users.get(getUserId(0)).addScore(9.72f);

        String inputTestFile = "./JUNIT_INPUT_"+System.currentTimeMillis() +"_.csv";
        String moderatedTestFile = "./JUNIT_OUTPUT_"+System.currentTimeMillis() +"_.csv";

        generateComments(users,numberOfMessages,inputTestFile);

        final int numberOfWorkers = 2;
        final int numberOfThreads = 10;

        ContentModeration contentModeration = new ContentModeration(translationService,scoringService,numberOfWorkers,numberOfThreads,inputTestFile,moderatedTestFile);
        contentModeration.startThreadWorkers();

        //VALIDATION
        verifyOutputFileHasExpectedResults(users,moderatedTestFile);

        //CLEAN UP
        deleteFiles(deleteFiles,inputTestFile,moderatedTestFile);
    }

    private void deleteFiles(boolean deleteFiles, String inputTestFile, String moderatedTestFile){
        if (deleteFiles){
            File finput = new File(inputTestFile);
            finput.delete();
            File fouput = new File(moderatedTestFile);
            fouput.delete();
        }
    }
    private void verifyOutputFileHasExpectedResults(Map<String,TestUserStats> users, String moderatedTestFile){
        // RESULT VERIFICATION
        int totalLinesRead = 0;
        try (CSVReader reader = new CSVReader(new FileReader(moderatedTestFile))) {
            String [] line;
            while ( (line = reader.readNext()) != null){
                totalLinesRead++;

                //String [] elements = line.split(",");
                String user = line[0];

                int totalMessages = Integer.parseInt(line[1]);
                float score = Float.parseFloat(line[2]);
                TestUserStats stats = users.get(user);
                if (stats == null){
                    System.out.println();
                }
                if (stats.getAverageScore() != score){
                    System.out.println("OLE OLA");
                }
                if (stats.getTotalMessages() != totalMessages){
                    System.out.println();
                }
                assert stats.getTotalMessages() == totalMessages;
                assert stats.getAverageScore() == score;
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        int usersWithMessages = 0;
        for (Map.Entry<String, TestUserStats> stringTestUserStatsEntry : users.entrySet()) {
            if ( stringTestUserStatsEntry.getValue().getTotalMessages() > 0 ){
                usersWithMessages++;
            }
        }
        assert usersWithMessages == totalLinesRead;
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