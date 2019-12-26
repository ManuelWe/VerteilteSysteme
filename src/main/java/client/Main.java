package client;

import java.util.Scanner;

public class Main {
	public static void main(final String[] args) {
		// TODO remove
		System.setOut(new java.io.PrintStream(System.out) {
			private StackTraceElement getCallSite() {
				for (StackTraceElement e : Thread.currentThread().getStackTrace())
					if (!e.getMethodName().equals("getStackTrace") && !e.getClassName().equals(getClass().getName()))
						return e;
				return null;
			}

			@Override
			public void println(String s) {
				println((Object) s);
			}

			@Override
			public void println(Object o) {
				StackTraceElement e = getCallSite();
				String callSite = e == null ? "??"
						: String.format("%s.%s(%s:%d)", e.getClassName(), e.getMethodName(), e.getFileName(),
								e.getLineNumber());
				super.println(o + "\t\tat " + callSite);
			}
		});

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
		System.out.println("For demo clients press �");
		System.out.println("******************************************************");
		System.out.printf("Your input: ");
		// String input = scanner.nextLine();
		String input = "c";

		if (input.equals("c") || input.equals("client") || input.equals("�")) {
			WebClient webClient = new WebClient(dhcpIp);
			String serverAddress = null;
			try {
				serverAddress = webClient.getServerAddress();
			} catch (Exception c) {
				System.out.println("DHCP nicht erreichbar!");
				System.exit(0);
			}

			if (input.equals("�")) {
				System.out.printf("How many?: ");
				int anzahl = Integer.parseInt(scanner.nextLine());
				for (int i = 0; i < anzahl; i++) {
					new Thread(new botClient(serverAddress, webClient)).start();
				}
			} else {
				new Client(serverAddress, webClient, false);
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
