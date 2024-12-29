package org.contentModeration;

import com.opencsv.CSVReader;
import org.contentModeration.testUtils.TestUserStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestContentModeration {
    // PROGRAM EXECUTION
    TranslationService translationService = mockTranslationService();
    ScoringService scoringService = mockScoringService();

    @Test
    void test_getBeginningOfNextLineOffset(){
        try {
            String line1 = MainAux.encryptThenHex("line1");
            String line2 = MainAux.encryptThenHex("line2");
            String line3 = MainAux.encryptThenHex("line3");

            String file = "test_getBeginningOfNextLineOffset.txt";

            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(line1+"\n");
            fileWriter.write(line2+"\n");
            fileWriter.write(line3+"\n");
            fileWriter.close();

            RandomAccessFile randomAccessFile = new RandomAccessFile(file,"r");

            for (int i = 0; i < line1.length(); i++) {
                long currentOffset = randomAccessFile.getFilePointer();
                long offset = ContentModeration.getBeginningOfNextLineOffset(i,randomAccessFile);

                assert currentOffset == randomAccessFile.getFilePointer();

                randomAccessFile.seek(offset);
                String lineRead = randomAccessFile.readLine();

                assert lineRead.equals(line2);
            }

            for (int i = line1.length()+1; i <(line1.length()+ line2.length()); i++) {
                long currentOffset = randomAccessFile.getFilePointer();
                long offset = ContentModeration.getBeginningOfNextLineOffset(i,randomAccessFile);
                assert currentOffset == randomAccessFile.getFilePointer();

                randomAccessFile.seek(offset);
                String lineRead = randomAccessFile.readLine();

                assert lineRead.equals(line3);
            }
            // 2 = the line breaks for line1 and line2
            for (int i = (line1.length() + line2.length()+2); i < randomAccessFile.length(); i++) {
                long currentOffset = randomAccessFile.getFilePointer();
                long offset = ContentModeration.getBeginningOfNextLineOffset(i,randomAccessFile);
                assert currentOffset == randomAccessFile.getFilePointer();

                randomAccessFile.seek(offset);
                String lineRead = randomAccessFile.readLine();

                assert lineRead == null;
            }

            randomAccessFile.close();

            deleteFiles(true,file);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    @ParameterizedTest
    @CsvSource({
            "1, 1, 1, 2",
            "50, 50, 4, 50",
            "100, 50, 10, 100",
            "50, 100, 10, 19"
    })
    void success_verifyModeratedCSV(int numberOfUsers, int numberOfMessages, int numberOfWorkers, int numberOfThreads) throws Exception {
        boolean deleteFiles = true;

        // INPUT DATA GENERATION
        //int numberOfUsers = 100;
        //int numberOfMessages = 50;

        Map<String, TestUserStats> users = createUsers(numberOfUsers);

        String inputTestFile = "./JUNIT_INPUT_"+System.currentTimeMillis() +"_.csv";
        String moderatedTestFile = "./JUNIT_OUTPUT_"+System.currentTimeMillis() +"_.csv";

        generateCommentsAndWriteToFile(users,numberOfMessages,inputTestFile);

        //Execute program
        //final int numberOfWorkers = 4;
        //final int numberOfThreads = 10;
        ContentModeration contentModeration = new ContentModeration(translationService,scoringService,numberOfWorkers,numberOfThreads,inputTestFile,moderatedTestFile);
        contentModeration.startThreadWorkers();

        //VALIDATION
        verifyOutputFileHasExpectedResults(users,moderatedTestFile);

        //CLEAN UP
        deleteFiles(deleteFiles,inputTestFile);
        deleteFiles(deleteFiles,moderatedTestFile);

    }

    private void deleteFiles(boolean deleteFiles, String inputTestFile){
        if (deleteFiles){
            File finput = new File(inputTestFile);
            finput.delete();
        }
    }
    private void verifyOutputFileHasExpectedResults(Map<String, TestUserStats> users, String moderatedTestFile){
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

    private Map<String, TestUserStats> createUsers(int numberOfUsers){
        Map<String, TestUserStats> users = new HashMap<>();
        for (int i = 0; i < numberOfUsers; i++) {
            String userName = getUserId(i);
            users.put(userName,new TestUserStats());
        }
        return users;
    }

    private void generateCommentsAndWriteToFile(Map<String, TestUserStats> users, int numberOfMessages, String outPutFile) throws Exception{
        File f = new File(outPutFile);
        FileWriter fileWriter = new FileWriter(f);

        //Generate messages
        for (int i = 0; i < numberOfMessages; i++) {
            String user = getUserId(i%users.size());
            String message = MainAux.encryptThenHex(System.currentTimeMillis()+"_"+i);
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