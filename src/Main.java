package src;
import java.util.Scanner;
public class Main
{
    public static void main(final String[] args) {
        final Scanner scanner = new Scanner(System.in);
        System.out.println("******************************************************");
        System.out.println("\tWelcome to the new Server / Client chooser");
        System.out.println("******************************************************");
        System.out.println("Would you like to open a server press s or type server");
        System.out.println("Would you like to open a client press c or type client");
        System.out.println("******************************************************");
        System.out.printf("Your input: ");
        String input = scanner.nextLine();
        if (input.equals("s") || input.equals("server")) {
            final Server server = new Server(5000);
        }
        else if (input.equals("c") || input.equals("client")) {
            System.out.println("Please specify the IP the server is on");
            input = scanner.nextLine();
            final Client client = new Client(input, 5000);
        }
        else {
            System.out.println("******************************************************");
            System.out.println("You are to stupid to type a correct letter, please\n" +
                    "disconnect your head from your body and let your body\n" +
                    "search a new brain");
            System.out.println("******************************************************");
        }
    }
}
