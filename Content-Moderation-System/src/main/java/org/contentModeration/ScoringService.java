package org.contentModeration;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ScoringService {
    Random random;
    public ScoringService(){
        random = new Random();
    }
    public Float  WhatIsTheScore(String comment) {
        CompletableFuture<Float> future = new CompletableFuture<>();
        float result;
        try {
             result = future.completeOnTimeout( (float) comment.length(),getLatency(), TimeUnit.MILLISECONDS).get();
        } catch (Exception e) {
            return 0f;
        }
        //System.out.println(result+" -- TRANSLATION SERVICE");
        return result;
    }
    public int getLatency(){
        return 200 + random.nextInt(800);
    }
}
