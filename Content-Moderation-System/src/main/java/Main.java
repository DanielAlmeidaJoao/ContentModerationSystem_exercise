import org.contentModeration.ContentModeration;
import org.contentModeration.ScoringService;
import org.contentModeration.TranslationService;

public class Main {
    public static void main(String [] args) throws Exception{
        int numberOfWorkers = 4;
        int numberOfThreads = 9000;
        if (args.length > 0){
            numberOfWorkers = Integer.parseInt(args[0]);
        }
        if (args.length > 0){
            numberOfThreads = Integer.parseInt(args[1]);
        }
        String path = "./TEST_DATA.csv";
        String path2 = "./src/main/resources/MOCK_DATA.csv";
        String path3 = "./200000_TEST_DATA.csv";

        TranslationService translationService = new TranslationService();
        ScoringService scoringService = new ScoringService();

        String outputPath = "./OUTPUT_"+System.currentTimeMillis()+".csv";
        ContentModeration contentModeration = new ContentModeration(translationService,scoringService,numberOfWorkers,numberOfThreads,path,outputPath);

        contentModeration.startThreadWorkers();
    }
}
