package client;

import java.util.Scanner;

public class Main {
	public static void main(final String[] args) {
		final Scanner scanner = new Scanner(System.in);
		System.out.println("******************************************************");
		System.out.println("\tWelcome to the new Server / Client chooser");
		System.out.println("******************************************************");
		System.out.println("Would you like to open a server press s or type server");
		System.out.println("Would you like to open a client press c or type client");
		System.out.println("For demo clients press �");
		System.out.println("******************************************************");
		System.out.printf("Your input: ");
		String input = scanner.nextLine();

		if (input.equals("s") || input.equals("server")) {
			new Server();
		} else if (input.equals("c") || input.equals("client") || input.equals("�")) {
			WebClient webClient = new WebClient();
			String serverAddress = webClient.getServerAddress();
			String ip = serverAddress.split(":")[0];
			int port = Integer.parseInt(serverAddress.split(":")[1]);
			if (input.equals("�")) {
				System.out.printf("How many?: ");
				int anzahl = Integer.parseInt(scanner.nextLine());
				for (int i = 0; i < anzahl; i++) {
					new Thread(new botClient(ip, port)).start();
				}
			} else {
				new Client(ip, port);
			}
		} else {
			System.out.println("******************************************************");
			System.out.println("You are to stupid to type a correct letter, please\n"
					+ "disconnect your head from your body and let your body\n" + "search a new brain");
			System.out.println("******************************************************");
		}
	}
}