package client;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server {
	// initialize socket and input stream
	private ServerSocket server = null;
	private int connections = 0;
	WebClient webClient;
	private List<String> clientAddresses = new ArrayList<String>();

	// constructor with port
	public Server() {
		Socket clientSocket = null;

		// starts server and waits for a connection
		try {
			server = new ServerSocket(0);
		} catch (IOException i) {
			i.printStackTrace();
		}

		String serverAddress = "127.0.0.1:" + server.getLocalPort();
		webClient = new WebClient();
		webClient.setServerAddress(serverAddress);

		System.out.println("******************************************************");
		System.out.println("\tWelcome to the server interface");
		System.out.println("******************************************************");
		System.out.println("Currently there is no functionality except seeing");
		System.out.println("incoming connections/disconnections");
		System.out.println("******************************************************");
		System.out.println("Log files: ");

		while (true) {
			try {
				clientSocket = server.accept();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// int clientPort =
			// Integer.parseInt(server.getLocalSocketAddress().toString().split(":")[1]);
//			System.out.println(clientPort);	//problematic on localhost
//			System.out.println(server.getLocalPort());
//			System.out.println(server.getInetAddress());
//			System.out.println(server.getPort());
			if (connections == 0) { // workaround to detect dhcp; if client is dhcp, start special thread
				try {
					new Thread(new dhcpSocketThread(clientSocket)).start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				try {
					new Thread(new clientSocketThread(clientSocket)).start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public abstract class clientThread implements Runnable {
		protected Socket clientSocket;
		protected DataInputStream in = null;
		protected DataOutputStream out;

		public clientThread(Socket clientSocket) {
			this.clientSocket = clientSocket;
			try {
				out = new DataOutputStream(clientSocket.getOutputStream());
				in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		protected void closeConnection() {
			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			connections--;
		}
	}

	public class clientSocketThread extends clientThread {
		private clientSocketThread(Socket clientSocket) throws IOException {
			super(clientSocket);
		}

		public void run() {
			String clientID = Integer.toString(connections);
			connections++;
			try {
				out.writeUTF(clientID);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Client " + clientID + " is now connected to the socket");
			String line = "";
			while (!line.equals("Over")) {
				try {
					line = in.readUTF();
					System.out.println("Client " + clientID + ": \"" + line + "\"");
					out.writeUTF("Message received");
				} catch (IOException i) {
					line = "Over";
				}
			}
			System.out.println("Closing connection with Client " + clientID);
			String clientAddress = clientSocket.getInetAddress().toString().split("/")[1] + ":"
					+ clientSocket.getPort();
			clientAddresses.remove(clientAddress);
			webClient.removeClientAddress(clientAddress);
			System.out.println("Connected Clients:" + Arrays.toString(clientAddresses.toArray()));
			closeConnection();
		}
	}

	public class dhcpSocketThread extends clientThread {
		private dhcpSocketThread(Socket clientSocket) throws IOException {
			super(clientSocket);
		}

		public void run() {
			connections++;
			String line = "";
			while (!line.equals("Over")) {
				try {
					line = in.readUTF();
					clientAddresses.add(line);
					System.out.println("Connected Clients:" + Arrays.toString(clientAddresses.toArray()));
				} catch (IOException i) {
					line = "Over";
				}
			}
			System.out.println("Closing connection with dhcp");
			closeConnection();
		}
	}
}
