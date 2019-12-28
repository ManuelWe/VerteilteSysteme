package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
	private ServerSocket server = null;
	private AtomicInteger nextClientID = new AtomicInteger(1);
	private Vector<ObjectOutputStream> outputStreams = new Vector<ObjectOutputStream>();
	private BlockingQueue<Message> messageList = new LinkedBlockingQueue<Message>();
	private Vector<String> voteRequestHandlerAddresses = new Vector<String>();
	private String serverAddress = "";
	private boolean serverRunning = true;
	private AtomicBoolean messageSent = new AtomicBoolean(false);
	private Map<Integer, Entry<Message, Integer>> uncommittedEntries = new ConcurrentHashMap<Integer, Entry<Message, Integer>>();
	private Vector<Message> committedEntries = new Vector<Message>(); // only String if we dont request particular
																		// messages
	private AtomicInteger sequenceNumber = new AtomicInteger(0);

	public Server(WebClient webClient) {
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
		// serverAddress = localAddress + ":" + server.getLocalPort();
		serverAddress = "127.0.0.1" + ":" + server.getLocalPort();
		System.out.println(serverAddress);
		webClient.setServerAddress(serverAddress);

		new Thread(new messageSenderThread()).start();
		new Thread(new heartbeatThread(webClient)).start();
		new Thread(new benchmarkingThread()).start();

		System.out.println("SERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVER");
		System.out.println("Log files: ");

		new Thread(new serverThread()).start();
	}

	public Server(WebClient webClient, int clientID, int nextID) {
		this(webClient);
		this.nextClientID.set(nextID);

		if (clientID > 0) {
			File file = new File("OutputFiles/OutputFile" + clientID + ".txt");
			Scanner sc = null;
			try {
				sc = new Scanner(file);
				Message message = null;
				while (sc.hasNextLine()) {
					message = new Message();
					message.setText(sc.nextLine());
					committedEntries.add(message);
				}
				file.delete();
				sc.close();
			} catch (FileNotFoundException e) {
				// If no messages were written, do nothing
			}

		}
	}

	private class serverThread implements Runnable {
		Socket clientSocket = null;

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
		Socket clientSocket = null;
		ObjectInputStream in = null;
		ObjectOutputStream out = null;
		String voteRequestHandlerAddress = null;

		private clientSocketThread(Socket clientSocket) throws IOException {
			this.clientSocket = clientSocket;

			try {
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				in = new ObjectInputStream(clientSocket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}

			// new Thread(new messageSenderThread(out, messageList.size())).start();
			Message message = new Message();
			message.setHeader("voteRequestHandlerAddresses");
			message.setStringList(voteRequestHandlerAddresses);
			message.setSequenceNumber(nextClientID.get());
			out.writeObject(message);
			message = new Message();
			message.setHeader("committedEntries");
			message.setMessageList(committedEntries);
			out.writeObject(message);

			outputStreams.add(out);
		}

		public void run() {
			int acknowledgesNeeded = 0;
			Message message = null;
			int clientID = 0;

			do {
				try {
					message = (Message) in.readObject();
					if (message.getHeader().equals("appendEntry")) {
						message.setSequenceNumber(sequenceNumber.getAndIncrement());
						message.setText(sequenceNumber.get() + " " + message.getText());
						try {
							messageList.put(message);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						acknowledgesNeeded = (int) Math.ceil(outputStreams.size() / 2.0);
						uncommittedEntries.put(message.getSequenceNumber(),
								new AbstractMap.SimpleEntry<Message, Integer>(message, acknowledgesNeeded));
					} else if (message.getHeader().equals("acknowledgeEntry")) {
						synchronized (uncommittedEntries) {
							if (uncommittedEntries.containsKey(message.getSequenceNumber())) {
								uncommittedEntries.compute(message.getSequenceNumber(), (key, val) -> {
									val.setValue(val.getValue() - 1);
									return val;
								});
								if (uncommittedEntries.get(message.getSequenceNumber()).getValue() == 0) {
									System.out.println("Client " + clientID + ": \""
											+ uncommittedEntries.get(message.getSequenceNumber()).getKey().getText()
											+ "\"");
									Message commitMessage = new Message();
									commitMessage.setHeader("commitEntry");
									commitMessage.setSequenceNumber(message.getSequenceNumber());
									try {
										messageList.put(commitMessage);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									committedEntries.add(uncommittedEntries.get(message.getSequenceNumber()).getKey());
									uncommittedEntries.remove(message.getSequenceNumber());
								}
							}
						}
					} else if (message.getHeader().equals("clientID")) {
						if (message.getSequenceNumber() == 0) {
							clientID = nextClientID.getAndIncrement();
						} else {
							clientID = message.getSequenceNumber();
						}
						System.out.println("Client " + clientID + " is now connected!");
						message = new Message();
						message.setHeader("clientID");
						message.setSequenceNumber(clientID);
						out.writeObject(message);
					} else if (message.getHeader().equals("voteRequestHandlerAddress")) {
						voteRequestHandlerAddresses.add(message.getText());
						voteRequestHandlerAddress = message.getText();
						message.setSequenceNumber(nextClientID.get());
						try {
							messageList.put(message);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						System.err.println("Message with wrong header received!");
					}
				} catch (IOException i) {
					break;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} while (serverRunning);
			System.out.println("Closing connection with Client " + clientID);

			closeConnection();
		}

		private void closeConnection() {
			outputStreams.remove(out);
			Message message = new Message();
			message.setHeader("removeVoteRequestHandlerAddress");
			message.setText(voteRequestHandlerAddress);
			try {
				messageList.put(message);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
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
			Message message = null;
			Iterator<ObjectOutputStream> it = null;
			ObjectOutputStream oos = null;

			while (serverRunning) {
				try {
					message = messageList.take();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				messageSent.set(true);

				synchronized (outputStreams) {
					for (it = outputStreams.iterator(); it.hasNext();) {
						try {
							oos = it.next();
							oos.writeObject(message);
						} catch (IOException e) {
							// TODO valid????
							outputStreams.remove(oos);
						}
					}
				}
			}
		}
	}

	private class heartbeatThread implements Runnable {
		Message message = new Message();
		WebClient webClient = null;
		int counter = 0;

		private heartbeatThread(WebClient webClient) {
			this.webClient = webClient;
		}

		public void run() {
			message.setHeader("heartbeat");
			while (serverRunning) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// only send heartbeat, when no message was sent
				if (!messageSent.getAndSet(false)) {
					try {
						messageList.put(message);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (counter > 10) {
					String addressOnDHCP = webClient.getServerAddress();
					if (!addressOnDHCP.equals(serverAddress)) {
						Message newMessage = new Message();
						newMessage.setHeader("connectToNewServer");
						newMessage.setText(addressOnDHCP);
						try {
							messageList.put(newMessage);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						// TODO new Client(addressOnDHCP, webClient, false);
						closeServer();
					}
					counter = 0;
				}
				counter++;
			}
		}
	}

	// TODO remove
	private class benchmarkingThread implements Runnable {
		public void run() {
			while (serverRunning) {
				if (messageList.size() > 40) {
					System.out.println(System.currentTimeMillis());
					while (messageList.size() > 0) {

					}
					System.out.println(System.currentTimeMillis());
				}
			}
		}
	}

	public String getServerAddress() {
		System.out.println(serverAddress);
		return serverAddress;
	}

	public Vector<Message> getEntriesList() {
		return committedEntries;
	}

	public void send() {
		Message message1 = new Message();
		message1.setHeader("heartbeat");
		for (int i = 0; i < 50; i++) {
			messageList.add(message1);
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