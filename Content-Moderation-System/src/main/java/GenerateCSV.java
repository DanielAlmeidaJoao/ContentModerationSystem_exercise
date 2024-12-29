import java.io.File;
import java.io.FileWriter;

import static org.contentModeration.Utils.encryptThenHex;

public class GenerateCSV {

    public static void main(String[] args) throws Exception {
        if (args.length != 2){
            System.err.println("ERROR: Missing file size and file path. Eg.: GenerateCSV 100000 ./nameOfFile.csv");
            return;
        }

        int size = Integer.parseInt(args[0]);
        File f = new File(args[1]);

        FileWriter fileWriter = new FileWriter(f);

        for (int i = 0; i < size; i++) {
            String message = System.currentTimeMillis()+""+i;
            fileWriter.write("user_name_"+i+","+ encryptThenHex(message)+"\n");
        }

        fileWriter.flush();
        fileWriter.close();
    }
}
