package client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
	// initialize socket and input stream
	private ServerSocket server = null;
	private int connections = 0;
	private WebClient webClient;
	public List<String> clientAddresses = new ArrayList<String>();
	private List<ObjectOutputStream> outputStreams = Collections.synchronizedList(new ArrayList<ObjectOutputStream>());
	public List<Message> messageList = Collections.synchronizedList(new ArrayList<Message>());
	public List<Message> dataList = Collections.synchronizedList(new ArrayList<Message>());
	public List<String> voteRequestHandlerAddresses = Collections.synchronizedList(new ArrayList<String>());
	public String serverAddress = "";
	private boolean serverRunning = true;
	private ReentrantLock mutex = new ReentrantLock();

	// constructor with port
	public Server(WebClient webClient) {
		Socket clientSocket = null;
		this.webClient = webClient;

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
		System.out.println(serverAddress);

		new Thread(new messageSenderThread()).start();
		new Thread(new heartbeatThread()).start();

		System.out.println("SERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVER");
		System.out.println("Log files: ");

		new Thread(new serverThread(clientSocket)).start();
	}

	public void removeVoteRequestHandlerAddress(String voteRequestHandlerAddress) {
		try { // TODO: remove when message commits work
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		voteRequestHandlerAddresses.remove(voteRequestHandlerAddress);
		Message message = new Message();
		message.setHeader("removeVoteRequestHandlerAddress");
		message.setText(voteRequestHandlerAddress);
		messageList.add(message);
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

	public class clientSocketThread implements Runnable {
		protected Socket clientSocket;
		protected ObjectInputStream in = null;
		protected ObjectOutputStream out;

		private clientSocketThread(Socket clientSocket) throws IOException {
			this.clientSocket = clientSocket;
			Message message = new Message();

			try {
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				mutex.lock();
				try {
					outputStreams.add(out);
				} finally {
					mutex.unlock();
				}
				in = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}

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
						messageList.add(message);
					} else if (message.getHeader().equals("removeVoteRequestHandlerAddress")) {
						voteRequestHandlerAddresses.remove(message.getText());
						messageList.add(message);
						System.out.println(voteRequestHandlerAddresses);
					} else {
						System.err.println("Message with wrong header received!");
					}
					System.out.println(voteRequestHandlerAddresses);
				} catch (IOException i) {
					break;
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} while (serverRunning && !message.getText().equals("Over"));
			System.out.println("Closing connection with Client " + clientID);
			String clientAddress = clientSocket.getInetAddress().toString().split("/")[1] + ":"
					+ clientSocket.getPort();
			clientAddresses.remove(clientAddress);
			System.out.println("Connected Clients:" + Arrays.toString(clientAddresses.toArray()));
			closeConnection();
		}

		private void closeConnection() {
			mutex.lock();
			try {
				outputStreams.remove(out);
			} finally {
				mutex.unlock();
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
			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// sends incoming messages to all clients
	public class messageSenderThread implements Runnable {
		int counter = 0;

		public void run() {
			while (serverRunning) {
				if (counter != messageList.size()) {
					for (int i = 0; i < outputStreams.size(); i++) {
						mutex.lock();
						try {
							try {
								outputStreams.get(i).writeObject(messageList.get(counter));
							} catch (IOException e) {
								e.printStackTrace();
							}
						} finally {
							mutex.unlock();
						}
					}
					counter++;
				}
			}
		}
	}

	public class heartbeatThread implements Runnable {
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