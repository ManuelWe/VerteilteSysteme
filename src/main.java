public class main {
    public static void main(String[] args) {
        FileWriter fileWriter = new FileWriter("1");
        fileWriter.start();
        FileWriter fileWriter2 = new FileWriter("2");
        fileWriter2.start();
    }
}