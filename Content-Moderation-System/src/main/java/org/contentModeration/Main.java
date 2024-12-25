package org.contentModeration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello world!");
        String filePath = "path/to/your/file.csv";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(","); // Split based on your delimiter
                System.out.println(String.join(", ", values));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}