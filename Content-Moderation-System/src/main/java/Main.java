import org.contentModeration.ContentModeration;

public class Main {
    public static void main(String [] args) throws Exception{
        String path = "/home/tsunami/Documents/javaProjects/ContentModerationSystem_exercise/Content-Moderation-System/TEST_DATA.csv";
        String path2 = "/home/tsunami/Documents/javaProjects/ContentModerationSystem_exercise/Content-Moderation-System/src/main/resources/MOCK_DATA.csv";
        String path3 = "/home/tsunami/Documents/javaProjects/ContentModerationSystem_exercise/Content-Moderation-System/200000_TEST_DATA.csv";
        ContentModeration contentModeration = new ContentModeration(4,path3);
        contentModeration.startThreadWorkers();
    }
}
