package client;

import java.util.Scanner;

public class Main {
	public static void main(final String[] args) {
		String dhcpIp = null;
		final Scanner scanner = new Scanner(System.in);
		if (args.length == 0) {
			System.out.println("Please specify dhcp server ip as argument!");
			System.exit(0);
		} else {
			dhcpIp = args[0];
		}

		System.out.println("******************************************************");
		System.out.println("\tWelcome to the new Client / Bot Client chooser");
		System.out.println("******************************************************");
		System.out.println("Would you like to open a client press c or type client");
		System.out.println("For demo clients press ü");
		System.out.println("******************************************************");
		System.out.printf("Your input: ");
		// String input = scanner.nextLine();
		String input = "c";

		if (input.equals("c") || input.equals("client") || input.equals("ü")) {
			WebClient webClient = new WebClient(dhcpIp);
			String serverAddress = null;
			try {
				serverAddress = webClient.getServerAddress();
			} catch (Exception c) {
				System.out.println("DHCP nicht erreichbar!");
				System.exit(0);
			}

			if (input.equals("ü")) {
				System.out.printf("How many?: ");
				int anzahl = Integer.parseInt(scanner.nextLine());
				for (int i = 0; i < anzahl; i++) {
					new Thread(new botClient(serverAddress, webClient)).start();
				}
			} else {
				new Client(serverAddress, webClient);
			}
		} else {
			System.out.println("******************************************************");
			System.out.println("You are to stupid to type a correct letter, please\n"
					+ "disconnect your head from your body and let your body\n" + "search a new brain");
			System.out.println("******************************************************");
		}
		scanner.close();
	}
}
