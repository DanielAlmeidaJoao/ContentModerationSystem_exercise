import org.contentModeration.ContentModeration;
import org.contentModeration.ScoringService;
import org.contentModeration.TranslationService;

public class Main {

    public static void main(String [] args) throws Exception{
        if (args.length != 3){
            System.err.println("Missing args: csvFilepath numberOfCSVWorkers numberOfThreads");
            return;
        }
        String filePath = args[0];
        int numberOfWorkers = Integer.parseInt(args[1]);
        int numberOfThreads = Integer.parseInt(args[2]);

        TranslationService translationService = new TranslationService();
        ScoringService scoringService = new ScoringService();

        String outputPath = "./OUTPUT_"+System.currentTimeMillis()+".csv";
        ContentModeration contentModeration = new ContentModeration(translationService,scoringService,numberOfWorkers,numberOfThreads,filePath,outputPath);

        contentModeration.executeContentModeration();
        System.out.println("Final Result is at: "+outputPath);
    }
}
