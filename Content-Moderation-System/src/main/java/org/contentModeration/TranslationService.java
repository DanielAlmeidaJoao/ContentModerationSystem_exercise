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
            comment = future.completeOnTimeout(comment,getLatency(),TimeUnit.MILLISECONDS).get();
        } catch (Exception e) {
        }
        //System.out.println(comment);
        return comment;
    }
    public int getLatency(){
        return 10 + random.nextInt(100);
    }

}
