package org.contentModeration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainAux {
    public static final MessageDigest messageDigest;

    static {
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws Exception {
        int size = 1000 * 200;
        File f = new File("./"+size+"_TEST_DATA.csv");

        FileWriter fileWriter = new FileWriter(f);

        for (int i = 0; i < size; i++) {
            String message = System.currentTimeMillis()+""+i;
            fileWriter.write("user_name_"+i+","+ encryptThenHex(message)+"\n");
        }
        fileWriter.flush();
        fileWriter.close();
    }

    public static String encryptThenHex(String message){
        byte [] hashed = messageDigest.digest((message.getBytes(StandardCharsets.UTF_8)));
        return toHex(hashed);
    }

    public static String toHex(byte [] input){
        StringBuilder hexString = new StringBuilder();
        for (byte b : input) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}