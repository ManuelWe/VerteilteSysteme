package client;

import java.util.Scanner;

public class Main {
	public static void main(final String[] args) {
		/*// TODO remove
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
		});*/

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
