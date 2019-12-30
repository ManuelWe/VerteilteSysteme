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
		WebClient webClient = new WebClient(dhcpIp);
		String serverAddress = null;
		try {
			serverAddress = webClient.getServerAddress();
		} catch (Exception c) {
			System.out.println("DHCP nicht erreichbar! Bitte als cli argument angeben!");
			System.out.println("******************************************************");
			System.exit(0);
		}
		if (serverAddress.equals("null")) {
			System.out.println("DHCP was offline. Waiting, if server registers in the");
			System.out.println("next 8 seconds!");
			System.out.println("******************************************************");
			try {
				Thread.sleep(8000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			serverAddress = webClient.getServerAddress();
			if (serverAddress.equals("null")) {
				serverAddress = "127.0.0.1:2";
			}
		}
		new Client(serverAddress, webClient, false);
		
	}
}
