package org.contentModeration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class MainAux {
    public static void main(String[] args) throws Exception {
        int size = 1000 * 200;
        File f = new File("./"+size+"_TEST_DATA.csv");

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        FileWriter fileWriter = new FileWriter(f);
        for (int i = 0; i < size; i++) {
            String time = System.currentTimeMillis()+""+i;
            byte [] hashed = messageDigest.digest((time.getBytes(StandardCharsets.UTF_8)));
            fileWriter.write("user_name_"+i+","+toHex(hashed)+"\n");
        }
        fileWriter.flush();
        fileWriter.close();
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