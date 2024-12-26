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
        try {
            return future.completeOnTimeout(random.nextFloat(),getLatency(), TimeUnit.MILLISECONDS).get();
        } catch (Exception e) {
            return 0f;
        }
    }
    public int getLatency(){
        return 200 + random.nextInt(400);
    }
}
