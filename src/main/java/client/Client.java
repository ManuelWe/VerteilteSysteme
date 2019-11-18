package client;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
	// initialize socket and scanner output streams
	private Socket socket = null;
	private Scanner scanner;
	private DataInputStream in = null;
	private DataOutputStream out = null;
	private String clientID = "";
	private WebClient webClient = null;
	private String clientAddress = null;

	// constructor to put ip address and port
	public Client(String address, int port, WebClient webClient) {
		this.webClient = webClient;

		System.out.println("******************************************************");
		System.out.println("\tWelcome to the client interface");
		System.out.println("******************************************************");
		System.out.println("Currently you are only able to send messages to the");
		System.out.println("server by typing them into here (\"Over\" to close)");
		System.out.println("******************************************************");

		try {
			socket = new Socket(address, port);
		} catch (IOException u) {
			System.out.println("!!!!!! No server available, you are the server !!!!!!");
			beNewServer();
		}

		String ip = socket.getLocalAddress().getHostAddress();
		int clientPort = socket.getLocalPort();
		clientAddress = ip + ":" + clientPort;
		webClient.addClientAddress(clientAddress);

		try {
			scanner = new Scanner(System.in);
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			clientID = in.readUTF();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String line = "";
		while (!line.equals("Over")) {
			System.out.printf("Your input: ");
			try {
				line = scanner.nextLine();
				out.writeUTF(line);
				System.out.println(in.readUTF());
			} catch (IOException c) {
				this.webClient = new WebClient();
				String serverAddress = null;
				try {
					serverAddress = this.webClient.getServerAddress();
				} catch (Exception d) {
					System.out.println("DHCP nicht erreichbar!");
					System.exit(0);
				}
				address = serverAddress.split(":")[0];
				port = Integer.parseInt(serverAddress.split(":")[1]);
				try {
					socket = new Socket(address, port);
				} catch (IOException e) {
					serverNotAvailable();
				}
			}
		}
		System.out.println("Closing connection to server");
		closeConnection();
	}

	private void closeConnection() {
		scanner.close();
		try {
			in.close();
		} catch (IOException i) {
			i.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException i) {
			i.printStackTrace();
		}
		try {
			socket.close();
		} catch (IOException i) {
			i.printStackTrace();
		}
	}
	private void serverNotAvailable() {
		webClient.noServerAvailable();
	}
	
	private void beNewServer() {
		new Server();
	}
}
