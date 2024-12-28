import org.contentModeration.ContentModeration;

public class Main {
    public static void main(String [] args) throws Exception{
        String path = "./TEST_DATA.csv";
        String path2 = "./src/main/resources/MOCK_DATA.csv";
        String path3 = "./200000_TEST_DATA.csv";
        ContentModeration contentModeration = new ContentModeration(4,path);
        contentModeration.startThreadWorkers();
    }
}
