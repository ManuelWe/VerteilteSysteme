package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
	// initialize socket and input stream
	private ServerSocket server = null;
	private int connections = 0;
	private Vector<ObjectOutputStream> outputStreams = new Vector<ObjectOutputStream>();
	public Vector<Message> messageList = new Vector<Message>();
	public List<Message> dataList = Collections.synchronizedList(new ArrayList<Message>());
	public CopyOnWriteArrayList<String> voteRequestHandlerAddresses = new CopyOnWriteArrayList<String>();
	public String serverAddress = "";
	private boolean serverRunning = true;

	public Server(WebClient webClient) {
		Socket clientSocket = null;

		// starts server and waits for a connection
		try {
			server = new ServerSocket(0);
		} catch (IOException i) {
			i.printStackTrace();
		}

		String localAddress = "";
		try {
			localAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		// String serverAddress = localAddress + ":" + server.getLocalPort();
		serverAddress = "127.0.0.1" + ":" + server.getLocalPort();
		webClient.setServerAddress(serverAddress);

		new Thread(new messageSenderThread()).start();
		new Thread(new heartbeatThread()).start();

		System.out.println("SERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVER");
		System.out.println("Log files: ");

		new Thread(new serverThread(clientSocket)).start();
	}

	private class serverThread implements Runnable {
		private Socket clientSocket = null;

		public serverThread(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		public void run() {
			while (serverRunning) {
				try {
					clientSocket = server.accept();
					new Thread(new clientSocketThread(clientSocket)).start();
				} catch (IOException e) {
					if (serverRunning) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private class clientSocketThread implements Runnable {
		Socket clientSocket;
		ObjectInputStream in = null;
		ObjectOutputStream out;
		String voteRequestHandlerAddress = null;

		private clientSocketThread(Socket clientSocket) throws IOException {
			this.clientSocket = clientSocket;
			Message message = new Message();

			try {
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				outputStreams.add(out);
				in = new ObjectInputStream(clientSocket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}

			// new Thread(new messageSenderThread(out, messageList.size())).start();

			message.setHeader("voteRequestHandlerAddresses");
			message.setList(voteRequestHandlerAddresses);
			messageList.add(message);
		}

		public void run() {
			String clientID = Integer.toString(connections);
			connections++;
//			try {
//				out.writeUTF(clientID);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			System.out.println("Client " + clientID + " is now connected to the socket");
			Message message = null;
			do {
				try {
					message = (Message) in.readObject();
					if (message.getHeader().equals("data")) {
						System.out.println("Client " + clientID + ": \"" + message.getText() + "\"");
						// out.writeUTF("Message received");
						if (!message.getText().equals("Over")) {
							messageList.add(message);
							dataList.add(message);
						}
					} else if (message.getHeader().equals("voteRequestHandlerAddress")) {
						voteRequestHandlerAddresses.add(message.getText());
						voteRequestHandlerAddress = message.getText();
						messageList.add(message);
					} else {
						System.err.println("Message with wrong header received!");
					}
					System.out.println(voteRequestHandlerAddresses);
				} catch (IOException i) {
					break;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} while (serverRunning && !message.getText().equals("Over"));
			System.out.println("Closing connection with Client " + clientID);

			closeConnection();
		}

		private void closeConnection() {
			outputStreams.remove(out);
			Message message = new Message();
			message.setHeader("removeVoteRequestHandlerAddress");
			message.setText(voteRequestHandlerAddress);
			messageList.add(message);
			voteRequestHandlerAddresses.remove(voteRequestHandlerAddress);

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
			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

//	public class messageSenderThread implements Runnable {
//		ObjectOutputStream out = null;
//		int counter = 0;
//
//		public messageSenderThread(ObjectOutputStream out, int counter) {
//			this.out = out;
//			this.counter = counter;
//		}
//
//		public void run() {
//			boolean socketOpen = true;
//
//			while (serverRunning && socketOpen) {
//				if (counter != messageList.size()) {
//					try {
//						out.writeObject(messageList.get(counter));
//					} catch (IOException e) {
//						socketOpen = false;
//						// TODO handle
//					}
//					counter++;
//				}
//			}
//		}
//	}

	// sends incoming messages to all clients
	public class messageSenderThread implements Runnable {

		public void run() {
			int counter = 0;
			Iterator<ObjectOutputStream> it = null;

			while (serverRunning) {
				if (counter < messageList.size()) {
					synchronized (outputStreams) {
						for (it = outputStreams.iterator(); it.hasNext();) {
							try {
								it.next().writeObject(messageList.get(counter));
							} catch (IOException e) {
								e.printStackTrace();
							}

						}
					}
					counter++;
				}
			}
		}
	}

	private class heartbeatThread implements Runnable {
		Message message = new Message();

		public void run() {
			message.setHeader("heartbeat");
			while (serverRunning) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				messageList.add(message);
			}
		}

	}

	// ############################## Testing Methods #########################

	public void closeServer() {
		System.out.println("CLOSING SERVER!!!!!!!!!!!!!!!!!!!!!!!!!!");
		serverRunning = false;
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}