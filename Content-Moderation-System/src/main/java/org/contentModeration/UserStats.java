package org.contentModeration;

public class UserStats {
    private int totalMessages;
    private float sumScores;

    public UserStats(){
        totalMessages = 0;
        sumScores = 0;
    }
    public void addScore(float score){
        sumScores += score;
        totalMessages++;
    }

    public float getAverageScore(){
        return sumScores / totalMessages;
    }
}
