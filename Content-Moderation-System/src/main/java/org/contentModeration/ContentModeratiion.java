package org.contentModeration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class ContentModeratiion {

    public void readFiles() throws FileNotFoundException {
        String filePath = "path";
        FileReader fileReader = new FileReader(filePath);
        int fileLength = filePath.length();
        //fileReader.read
    }

}
