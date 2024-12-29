package org.contentModeration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static final MessageDigest messageDigest;

    static {
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
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