import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
public class FileWriter extends Thread {
    private String threadName = "";
    public FileWriter(String threadName) {
        this.threadName = threadName;
    }
    public void run() {
        while (true) {
            System.out.println(threadName);
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            List<String> lines = Arrays.asList(randomText() + " " + sdf.format(cal.getTime()) + " " + threadName);
            Path file = Paths.get("file.txt");
            try {
                Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } catch (IOException e) {
                try {
                    Files.write(file, lines, StandardCharsets.UTF_8);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            try {
                int timeout = (int) ((Math.random() * 5000) + 1000);
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    private String randomText() {
        int leftLimit = 65; // letter 'A'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 20;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int) (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        String generatedString = buffer.toString();
        return generatedString;
    }
}