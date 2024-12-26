package org.contentModeration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.*;

public class TranslationService {
    Random random;
    public TranslationService(){
        random = new Random();
    }
    public String  TranslateToEnglish(String comment) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            return future.completeOnTimeout(comment,getLatency(),TimeUnit.MILLISECONDS).get();
        } catch (Exception e) {
            return comment;
        }
    }
    public int getLatency(){
        return 200 + random.nextInt(400);
    }

    public static void main (String [] args){
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(()->{

        });
        //Executors.newSingleThreadExecutor();
        //new ScheduledThreadPoolExecutor() //new ThreadPoolExecutor();
    }
}
