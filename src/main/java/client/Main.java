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
				System.out.println("DHCP nicht erreichbar! Bitte als cli argument angeben!");
				System.exit(0);
			}
			if (serverAddress.equals("0")) {
				System.out.println("DHCP was offline. Waiting, if server registers in the next 8 seconds!");
				try {
					Thread.sleep(8000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				serverAddress = webClient.getServerAddress();
				if (serverAddress.equals("0")) {
					serverAddress = "127.0.0.1:2";
				}
			}
			System.out.println(serverAddress);

			if (input.equals("ü")) {
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
			System.out.println("Please enter a valid letter!");
			System.out.println("******************************************************");
		}
		scanner.close();
	}
}
