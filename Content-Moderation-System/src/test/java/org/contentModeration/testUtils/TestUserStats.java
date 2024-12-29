package org.contentModeration.testUtils;

public class TestUserStats {
    private int totalMessages;
    private float sumScores;

    public TestUserStats(){
        totalMessages = 0;
        sumScores = 0;
    }
    public void addScore(float score){
        sumScores += score;
        totalMessages++;
    }

    public int getTotalMessages(){
        return totalMessages;
    }

    public float getAverageScore(){
        return sumScores / totalMessages;
    }
}
