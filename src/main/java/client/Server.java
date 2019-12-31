package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.ArrayList;
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
	private BlockingQueue<String> messageTextQueue = new LinkedBlockingQueue<String>();

	/*
	 * Erstelle server socket; sende addresse zum dhcp; wenn server der erste node
	 * im cluster ist, lese größtes file ein; starte server threads INPUTS:
	 * webClient instanz; clientID (0, wenn server der erste node im cluster ist)
	 * OUTPUTS: keine AUTOR: Manuel VERSION: 1.6.3 ERSTELLT: 16.11.19 GEÄNDERT:
	 * 28.12.19
	 */
	public Server(WebClient webClient, int clientID) {
		try {
			server = new ServerSocket(0);
		} catch (IOException i) {
			i.printStackTrace();
		}

		// get local ip address
		String localAddress = null;
		try (final DatagramSocket socket = new DatagramSocket()) {
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			localAddress = socket.getLocalAddress().getHostAddress();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		// serverAddress = localAddress + ":" + server.getLocalPort();
		serverAddress = "127.0.0.1" + ":" + server.getLocalPort();

		webClient.setServerAddress(serverAddress);

		// if server is first node in cluster, read largest file as committed messages
		if (clientID == 0) {
			List<File> files = new ArrayList<File>();
			try {
				for (File file : new File("OutputFiles").listFiles())
					if (!file.isDirectory())
						files.add(file);
			} catch (NullPointerException e) {

			}
			if (files.size() > 0) {
				File biggestFile = files.get(0);
				for (int i = 1; i < files.size(); i++) {
					if (biggestFile.length() < files.get(i).length()) {
						biggestFile = files.get(i);
					}
				}

				if (!biggestFile.getName().equals("OutputFileSERVER.txt")) {
					new File("OutputFiles/OutputFileSERVER.txt").delete();
				}

				readFromFile(biggestFile);

				System.out.println("Messages read from " + biggestFile.getName());

				for (File file : new File("OutputFiles").listFiles())
					if (!file.isDirectory() && !file.getName().equals("OutputFileSERVER.txt"))
						file.delete();
			}
		}

		new Thread(new messageSenderThread()).start();
		new Thread(new heartbeatThread(webClient)).start();
		new Thread(new fileWriterThread()).start();

		System.out.println("******************************************************");
		System.out.println("You are now the server");
		System.out.println("******************************************************");
		System.out.println("Server log: ");

		new Thread(new serverThread()).start();
	}

	/*
	 * Lese file des clients ein, der zum server wird, wenn server nicht erster node
	 * im cluster INPUTS: webClient instanz; clientID (0, wenn server der erste node
	 * im cluster ist); id des nächsten clients; uncommittete einträge des clients,
	 * der server startet; nächste sequenznummer OUTPUTS: keine AUTOR: Manuel
	 * VERSION: 1.6.5 ERSTELLT: 19.11.19 GEÄNDERT: 29.12.19
	 */
	public Server(WebClient webClient, int clientID, int nextID, Map<Integer, Message> uncommittedEntries,
			int nextSequenceNumber) {
		this(webClient, clientID);
		this.nextClientID.set(nextID);
		if (this.nextSequenceNumber.get() < nextSequenceNumber) {
			this.nextSequenceNumber.set(nextSequenceNumber);
		}

		if (clientID > 0) {
			new File("OutputFiles/OutputFileSERVER.txt").delete();
			File file = new File("OutputFiles/OutputFile" + clientID + ".txt");
			readFromFile(file);
		}
		new Thread(new uncommittedMessagesThread(uncommittedEntries)).start();
	}

	/*
	 * Ließt zeilen eines files ein, und fügt nachrichten zu committeden entries
	 * dazu und schreibt sie in das server file INPUTS: file instanz OUTPUTS: keine
	 * AUTOR: Manuel VERSION: 1.0.0 ERSTELLT: 20.12.19 GEÄNDERT: 30.12.19
	 */
	public void readFromFile(File file) {
		Scanner sc = null;
		try {
			sc = new Scanner(file);
			Message message = null;
			String text = "";
			while (sc.hasNextLine()) {
				message = new Message();
				text = sc.nextLine();
				message.setText(text);
				if (!file.getName().equals("OutputFileSERVER.txt")) {
					try {
						messageTextQueue.put(text);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				committedEntries.add(message);
			}
			if (message != null && message.getText() != null) {
				nextSequenceNumber.set(Integer.parseInt(message.getText().split(" ")[0]) + 1);
			}
			file.delete();
			sc.close();
		} catch (FileNotFoundException e) {
			// If no messages were written, do nothing
		}
	}

	/*
	 * Konsumiere nachricht aus messageTextQueue und schreibe diese ins file INPUTS:
	 * keine OUTPUTS: keine AUTOR: Manuel VERSION: 1.0.2 ERSTELLT: 30.12.19
	 * GEÄNDERT: 31.12.19
	 */
	private class fileWriterThread implements Runnable {
		File file = new File("OutputFiles/OutputFileSERVER.txt");

		public fileWriterThread() {
			File dir = new File("OutputFiles");
			try {
				dir.mkdir();
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			String messageText = "";
			while (serverRunning) {
				try {
					messageText = messageTextQueue.take();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				synchronized (file) {
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
			}
		}
	}

	/*
	 * Aktzeptiert einkommende verbindungen und startet clientSocketThread INPUTS:
	 * keine OUTPUTS: keine AUTOR: Manuel VERSION: 1.0.0 ERSTELLT: 14.11.19
	 * GEÄNDERT: 18.11.19
	 */
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

	/*
	 * open streams and send voteRequestHandlerAddresses to client; füge
	 * outputstream zur liste der outputstreams hinzu und verarbeite einkommende
	 * nachrichten INPUTS: clientSocket instanz OUTPUTS: keine AUTOR: Manuel
	 * VERSION: 1.1.0 ERSTELLT: 18.11.19 GEÄNDERT: 31.12.19
	 */
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
						// füge nachricht zur liste der uncommitteten einträge hinzu und berechne
						// acknoledges needed
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
						// zähle acknowledge messages und commite entry sobald genügend acknowledges
						// gezählt wurden und sende commit message an clients
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
											try {
												messageTextQueue.put(uncommittedEntries.get(i).getKey().getText());
											} catch (InterruptedException e) {
												e.printStackTrace();
											}
											committedEntries.add(uncommittedEntries.get(i).getKey());
											uncommittedEntries.remove(i);
										}
									}
								}
							}
						}
					} else if (message.getHeader().equals("requestEntry")) {
						// send requested entry back to client
						responseMessage = new Message();
						responseMessage.setHeader("requestedEntry");
						responseMessage.setText(committedEntries.get(message.getSequenceNumber()).getText());
						responseMessage.setSequenceNumber(message.getSequenceNumber());
						synchronized (outputStreams) {
							out.writeObject(responseMessage);
						}
					} else if (message.getHeader().equals("clientID")) {
						// new client id if client was not previously connected to cluster
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

	/*
	 * send next message in queue to all outputstreams INPUTS:keine OUTPUTS: keine
	 * AUTOR: Manuel VERSION: 1.9.1 ERSTELLT: 24.11.19 GEÄNDERT: 30.12.19
	 */
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
							outputStreams.remove(oos);
						}
					}
				}
			}
		}
	}

	/*
	 * adds heartbeat to message queue every second; every 7 second it is checked,
	 * if server is still the server refferenced by dhcp, if not he send
	 * connectToNewServer message to clients INPUTS: webClient instanz OUTPUTS:
	 * keine AUTOR: Manuel VERSION: 1.2.0 ERSTELLT: 18.11.19 GEÄNDERT: 28.12.19
	 */
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
				// only send heartbeat, when no message was sent in the last second
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
						if (addressOnDHCP.equals("null")) {
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
							closeServer();
						}
					}
					counter = 0;
				}
				counter++;
			}
		}
	}

	/*
	 * wait shortly, until previously connected clients have connected to new server
	 * after election and send them uncommitted messages, that the client had, who
	 * got elected INPUTS: liste der uncommittedEntries OUTPUTS: keine AUTOR: Manuel
	 * VERSION: 1.3.2 ERSTELLT: 10.12.19 GEÄNDERT: 15.12.19
	 */
	private class uncommittedMessagesThread implements Runnable {
		Map<Integer, Message> oldUncommittedEntries = null;

		private uncommittedMessagesThread(Map<Integer, Message> uncommittedEntries) {
			oldUncommittedEntries = uncommittedEntries;
		}

		@Override
		public void run() {
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

	public String getServerAddress() {
		return serverAddress;
	}
}