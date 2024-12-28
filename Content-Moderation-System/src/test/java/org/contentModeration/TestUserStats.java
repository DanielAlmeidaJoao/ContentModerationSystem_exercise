package org.contentModeration;

import java.util.HashSet;
import java.util.Set;

public class TestUserStats {
    private int totalMessages;
    private float sumScores;
    Set<String> messages;

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
