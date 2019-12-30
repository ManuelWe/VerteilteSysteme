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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
	private Vector<Message> committedEntries = new Vector<Message>();
	private AtomicInteger nextSequenceNumber = new AtomicInteger(0);

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

		System.out.println("SERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVER");
		System.out.println("Log files: ");

		new Thread(new serverThread()).start();

	}

	public Server(WebClient webClient, int clientID, int nextID, Map<Integer, Message> uncommittedEntries,
			int nextSequenceNumber) {
		this(webClient);
		this.nextClientID.set(nextID);
		this.nextSequenceNumber.set(nextSequenceNumber);

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
				if (message.getText() != null) {
					this.nextSequenceNumber.set(Integer.parseInt(message.getText().split(" ")[0]) + 1);
				}
				file.delete();
				sc.close();
			} catch (FileNotFoundException e) {
				// If no messages were written, do nothing
			}
		}
		new Thread(new uncommittedMessagesThread(uncommittedEntries)).start();
	}

	// TODO remove
	private void writeToFile(String messageText) {
		File dir = new File("OutputFiles");
		File file = new File("OutputFiles/OutputFileSERVER.txt");
		try {
			dir.mkdir();
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		List<String> lines = Arrays.asList(messageText);

		try {
			Files.write(file.toPath(), lines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
		} catch (IOException e) {
			try {
				Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private class serverThread implements Runnable {
		Socket clientSocket = null;

		@Override
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

			Message message = new Message();
			message.setHeader("voteRequestHandlerAddresses");
			message.setStringList(voteRequestHandlerAddresses);
			message.setSequenceNumber(nextClientID.get());
			out.writeObject(message);

			outputStreams.add(out);
		}

		@Override
		public void run() {
			int acknowledgesNeeded = 0;
			Message message = null;
			Message responseMessage = null;
			int clientID = 0;
			boolean newClient = true;

			do {
				try {
					message = (Message) in.readObject();
					if (message.getHeader().equals("appendEntry")) {
						synchronized (uncommittedEntries) {
							message.setSequenceNumber(nextSequenceNumber.get());
							message.setText(nextSequenceNumber.getAndIncrement() + " " + message.getText());
							try {
								messageList.put(message);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							acknowledgesNeeded = (int) Math.ceil(outputStreams.size() / 2.0);
							uncommittedEntries.put(message.getSequenceNumber(),
									new AbstractMap.SimpleEntry<Message, Integer>(message, acknowledgesNeeded));
						}
					} else if (message.getHeader().equals("acknowledgeEntry")) {
						synchronized (uncommittedEntries) {
							if (uncommittedEntries.containsKey(message.getSequenceNumber())) {
								uncommittedEntries.compute(message.getSequenceNumber(), (key, val) -> {
									val.setValue(val.getValue() - 1);
									return val;
								});
								if (uncommittedEntries.get(message.getSequenceNumber()).getValue() == 0) {
									String messageText = uncommittedEntries.get(message.getSequenceNumber()).getKey()
											.getText();
									System.out.println(
											"Client " + messageText.split("ID:")[1] + ": \"" + messageText + "\"");
									responseMessage = new Message();
									responseMessage.setHeader("commitEntry");
									responseMessage.setSequenceNumber(message.getSequenceNumber());
									try {
										messageList.put(responseMessage);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}

									for (int i = committedEntries.size(); i <= message.getSequenceNumber(); i++) {
										if (uncommittedEntries.containsKey(i)) {
											writeToFile(uncommittedEntries.get(i).getKey().getText());
											committedEntries.add(uncommittedEntries.get(i).getKey());
											uncommittedEntries.remove(i);
										}
									}
								}
							}
						}
					} else if (message.getHeader().equals("requestEntry")) {
						responseMessage = new Message();
						responseMessage.setHeader("requestedEntry");
						responseMessage.setText(committedEntries.get(message.getSequenceNumber()).getText());
						responseMessage.setSequenceNumber(message.getSequenceNumber());
						synchronized (outputStreams) {
							out.writeObject(responseMessage);
						}
					} else if (message.getHeader().equals("clientID")) {
						if (message.getSequenceNumber() == 0) {
							clientID = nextClientID.getAndIncrement();
							newClient = true;
						} else {
							clientID = message.getSequenceNumber();
							newClient = false;
						}
						System.out.println("Client " + clientID + " is now connected!");
						message = new Message();
						message.setHeader("clientID");
						message.setSequenceNumber(clientID);
						synchronized (outputStreams) {
							out.writeObject(message);
						}
						if (newClient) {
							message = new Message();
							message.setHeader("committedEntries");
							message.setMessageList(committedEntries);
							synchronized (outputStreams) {
								out.writeObject(message);
							}
						}
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

	// sends incoming messages to all clients
	public class messageSenderThread implements Runnable {

		@Override
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
		String addressOnDHCP = null;

		private heartbeatThread(WebClient webClient) {
			this.webClient = webClient;
		}

		@Override
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
				if (counter > 7) {
					try {
						addressOnDHCP = webClient.getServerAddress();
					} catch (Exception e) {
						System.out.println("DHCP unreachable. The current network is not affected!");
						addressOnDHCP = null;
					}

					if (addressOnDHCP != null) {
						if (addressOnDHCP.equals("0")) {
							webClient.setServerAddress(serverAddress);
							System.out.println("DHCP reachable again!");
						} else if (!addressOnDHCP.equals(serverAddress)) {
							Message newMessage = new Message();
							newMessage.setHeader("connectToNewServer");
							newMessage.setText(addressOnDHCP);
							try {
								messageList.put(newMessage);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

							System.out.println("Server not leader anymore!");
							closeServer();
						}
					}
					counter = 0;
				}
				counter++;
			}
		}
	}

	private class uncommittedMessagesThread implements Runnable {
		Map<Integer, Message> oldUncommittedEntries = null;

		private uncommittedMessagesThread(Map<Integer, Message> uncommittedEntries) {
			oldUncommittedEntries = uncommittedEntries;
		}

		@Override
		public void run() { // TODO test?
			try {
				Thread.sleep(1000); // wait until most of clients are connected
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			int acknowledgesNeeded = (int) Math.ceil(outputStreams.size() / 2.0);
			for (Map.Entry<Integer, Message> mapEntry : oldUncommittedEntries.entrySet()) {
				if (outputStreams.size() == 0) {
					committedEntries.add(mapEntry.getKey(), mapEntry.getValue());
				} else {
					try {
						messageList.put(mapEntry.getValue());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					uncommittedEntries.put(mapEntry.getKey(),
							new AbstractMap.SimpleEntry<Message, Integer>(mapEntry.getValue(), acknowledgesNeeded));
				}
			}
		}
	}

	public String getServerAddress() {
		return serverAddress;
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

	public Vector<Message> getCommittedEntries() {
		return committedEntries;
	}
  
	public Vector<ObjectOutputStream> getOutputStreams() {
		return outputStreams;
  }
  
	public void setCommittedEntries(int key, Message message) {
		committedEntries.add(key, message);
	}

	public void sendMessage(Message message) {
		messageList.add(message);
	}
}