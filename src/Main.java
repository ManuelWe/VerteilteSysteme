package src;

public class Main {
    public static void main(String args[]) {
        Server server = new Server(5000);
        Client client = new Client("127.0.0.1", 5000);
    }
}
