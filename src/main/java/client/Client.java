package client;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
	private String serverAddress = null;
	private List<String> messageList = Collections.synchronizedList(new ArrayList<String>());

	// constructor to put ip address and port
	public Client(String serverAddress, WebClient webClient) {
		this.webClient = webClient;
		this.serverAddress = serverAddress;

		System.out.println("******************************************************");
		System.out.println("\tWelcome to the client interface");
		System.out.println("******************************************************");
		System.out.println("Currently you are only able to send messages to the");
		System.out.println("server by typing them into here (\"Over\" to close)");
		System.out.println("******************************************************");

		String address = serverAddress.split(":")[0];
		int port = Integer.parseInt(serverAddress.split(":")[1]);

		try {
			socket = new Socket(address, port);
		} catch (IOException u) {
			System.out.println("Trying local address!");
			try {
				// workaround to still be able to launch on laptop; not needed on raspberry
				socket = new Socket("127.0.0.1", port);
			} catch (IOException e) {
				System.out.println("!!!!!! No server available, you are the server !!!!!!");
				startNewServer();
			}
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

		new Thread(new messageReceiverThread()).start();

		String line = "";
		String newServerAddress;
		while (!line.equals("Over")) {
			System.out.printf("Your input: ");
			try {
				line = scanner.nextLine();
				out.writeUTF(line);
				// System.out.println(in.readUTF());
			} catch (IOException c) {
				newServerAddress = webClient.noServerAvailable(clientAddress, serverAddress);
				if (newServerAddress.equals(clientAddress)) {
					System.out.println("Starting server....");
					startNewServer();
				} else {
					System.out.println("Connecting to new server " + newServerAddress);
					closeConnection();

					try {
						socket = new Socket(newServerAddress.split(":")[0],
								Integer.parseInt(newServerAddress.split(":")[1]));

						try {
							scanner = new Scanner(System.in);
							out = new DataOutputStream(socket.getOutputStream());
							out.writeUTF(line); // send lost message to new server
							in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
							clientID = in.readUTF();
						} catch (IOException e) {
							e.printStackTrace();
						}
					} catch (Exception e) {
						System.out.println(e);
					}
				}
				serverAddress = newServerAddress;
			}
		}
		System.out.println("Closing connection to server");
		closeConnection();
		scanner.close();
	}

	private void closeConnection() {
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

	private void startNewServer() {
		new Server(webClient);
	}

	// receives messages from server
	public class messageReceiverThread implements Runnable {
		int counter = 0;

		public void run() {
			while (true) {
				try {
					messageList.add(in.readUTF());
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println(messageList);
			}
		}
	}
}
