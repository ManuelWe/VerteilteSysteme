package client;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
	// initialize socket and scanner output streams
	private Socket socket = null;
	private Scanner scanner;
	private ObjectInputStream in = null;
	private ObjectOutputStream out = null;
	private WebClient webClient = null;
	private String serverAddress = null;
	private int heartbeatCounter = 3;
	private int currentElectionTerm = 0;
	private AtomicBoolean election = new AtomicBoolean(false);
	private VoteRequestHandler voteRequestHandler = null;
	private String voteRequestHandlerAddress = "";
	private List<String> voteRequestHandlerAddresses = new ArrayList<String>();
	private int votes = 0;
	private AtomicBoolean startNewServer = new AtomicBoolean(false);
	private boolean closeConnection = false;
	private boolean clientRunning = true;
	private boolean connectToNewServer = false;
	private String tempServerAddress = "";
	private Server server = null;
	private String electedServerAddress = null;
	private Object electionLock = new Object();
	private Map<Integer, Message> uncommittedEntries = new HashMap<Integer, Message>();
	private Vector<Message> committedEntries = new Vector<Message>();
	private int nextClientID = 1;
	private int clientID = 0;
	private int nextSequenceNumber = 0;
	private int highestCommittedSequenceNumber = 0;
	private File file = null;

	public Client(String serverAddress, WebClient webClient, boolean automatedTest) {
		this.webClient = webClient;
		this.serverAddress = serverAddress;
		voteRequestHandler = new VoteRequestHandler(this);
		voteRequestHandlerAddress = voteRequestHandler.getAddress();

		if (!automatedTest) {
			System.out.println("CLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENT");
		}

		String address = serverAddress.split(":")[0];
		int port = Integer.parseInt(serverAddress.split(":")[1]);
		try {
			socket = new Socket(address, port);
			socket.setSoTimeout(15000);

			try {
				scanner = new Scanner(System.in);
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(socket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}

			new Thread(new messageReceiverThread()).start();
			new Thread(new heartbeatMonitorThread()).start();
			new Thread(new electionHandlerThread()).start();

			Message message = new Message();
			message.setHeader("clientID");
			message.setSequenceNumber(clientID);
			try {
				out.writeObject(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
			message = new Message();
			message.setHeader("voteRequestHandlerAddress");
			message.setText(voteRequestHandlerAddress);
			try {
				out.writeObject(message);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			if (!automatedTest) {
				clientMain();
			}
		} catch (IOException u) {
			startNewServer();
			System.out.println("Started server from 85");
		}
	}

	private void clientMain() {
		Message message = null;
		do {
			System.out.printf("Your input: ");
			message = new Message();
			try {
				message.setText(scanner.nextLine() + " " + ZonedDateTime.now() + " ID:" + clientID);
				message.setHeader("appendEntry");
				synchronized (out) {
					out.writeObject(message);
				}
			} catch (IOException c) {
				c.printStackTrace();
				election.set(true);
				synchronized (electionLock) {
					electionLock.notify();
				}
			}
		} while (clientRunning);
		System.out.println("Closing connection to server");
		scanner.close();

		closeConnection();
		System.exit(0);
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
		server = new Server(webClient, clientID, nextClientID, uncommittedEntries, nextSequenceNumber);
		serverAddress = server.getServerAddress();
		clientRunning = false;
	}

	private void connectToNewServer() {
		connectToNewServer = true;
		System.out.println("Connecting to new server " + serverAddress);
		closeConnection();
		currentElectionTerm = voteRequestHandler.getElectionTerm();
		System.out.println("New election term:" + currentElectionTerm);
		uncommittedEntries.clear();

		try {
			socket = new Socket(serverAddress.split(":")[0], Integer.parseInt(serverAddress.split(":")[1]));
			socket.setSoTimeout(3000);
		} catch (Exception e) {
			e.printStackTrace();
			election.set(true);
			synchronized (electionLock) {
				electionLock.notify();
			}
		}

		try {
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		Message message = new Message();
		message.setHeader("clientID");
		message.setSequenceNumber(clientID);
		try {
			synchronized (out) {
				out.writeObject(message);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		message = new Message();
		message.setHeader("voteRequestHandlerAddress");
		message.setText(voteRequestHandlerAddress);
		try {
			synchronized (out) {
				out.writeObject(message);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.printf("Your input: ");
	}

	// receives messages from server
	private class messageReceiverThread implements Runnable {
		@Override
		public void run() {
			Message message = null;
			Message responseMessage = null;
			String messageText = null;

			while (clientRunning) {
				if (election.get()) {
					synchronized (election) {
						try {
							election.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				try {
					message = (Message) in.readObject();
				} catch (SocketException s) {
					if (connectToNewServer) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						connectToNewServer = false;
					} else {
						election.set(true);
						synchronized (electionLock) {
							electionLock.notify();
						}
					}
				} catch (IOException e) {
					if (clientRunning) {
						e.printStackTrace();
						election.set(true);
						synchronized (electionLock) {
							electionLock.notify();
						}
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}

				if (clientRunning) {
					heartbeatCounter = 4;
					if (message.getHeader().equals("appendEntry")) {
						if (message.getSequenceNumber() >= nextSequenceNumber) {
							nextSequenceNumber = message.getSequenceNumber() + 1;
						}
						uncommittedEntries.put(message.getSequenceNumber(), message);
						responseMessage = new Message();
						responseMessage.setHeader("acknowledgeEntry");
						responseMessage.setSequenceNumber(message.getSequenceNumber());
						try {
							synchronized (out) {
								out.writeObject(responseMessage);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else if (message.getHeader().equals("commitEntry")) {
						if (message.getSequenceNumber() > highestCommittedSequenceNumber) {
							highestCommittedSequenceNumber = message.getSequenceNumber();
						}
						if (committedEntries.size() <= message.getSequenceNumber()) {
							for (int i = committedEntries.size(); i <= message.getSequenceNumber(); i++) {
								if (uncommittedEntries.containsKey(i)) {
									if (committedEntries.size() == i) {
										messageText = uncommittedEntries.get(i).getText();
										System.out.println(clientID + ": " + messageText + " committed");
										writeToFile(messageText);
										committedEntries.add(uncommittedEntries.remove(i));
									}
								} else {
									System.out.println("Message " + i + " requested");
									responseMessage = new Message();
									responseMessage.setHeader("requestEntry");
									responseMessage.setSequenceNumber(i);
									try {
										synchronized (out) {
											out.writeObject(responseMessage);
										}
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}
						} else {
							// substitute existing message with new one
							synchronized (file) {
								List<String> fileContent = null;
								try {
									fileContent = new ArrayList<>(
											Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
								} catch (IOException e) {
									e.printStackTrace();
								}

								for (int i = 0; i < fileContent.size(); i++) {
									if (Integer.parseInt(fileContent.get(i).split(" ")[0]) == message
											.getSequenceNumber()) {
										fileContent.set(i,
												uncommittedEntries.get(message.getSequenceNumber()).getText());
										committedEntries.set(i, uncommittedEntries.remove(i));
										break;
									}
								}
								try {
									Files.write(file.toPath(), fileContent, StandardCharsets.UTF_8);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					} else if (message.getHeader().equals("committedEntries")) {
						committedEntries.addAll(message.getMessageList());
						file.delete();
						for (Message newMessage : message.getMessageList()) {
							writeToFile(newMessage.getText());
						}
						if (message.getMessageList().size() > 0) {
							System.out.println("Added " + message.getMessageList().size() + " messages to log!");
						}
					} else if (message.getHeader().equals("requestedEntry")) {
						if (committedEntries.size() == message.getSequenceNumber()) {
							System.out.println(clientID + ": " + message.getText() + " committed");
							writeToFile(message.getText());
							committedEntries.add(message);
							uncommittedEntries.remove(message.getSequenceNumber());
							for (int i = committedEntries.size(); i <= highestCommittedSequenceNumber
									&& committedEntries.size() == i; i++) {
								if (uncommittedEntries.containsKey(i)) {
									messageText = uncommittedEntries.get(i).getText();
									System.out.println(clientID + ": " + messageText + " committed");
									writeToFile(messageText);
									committedEntries.add(uncommittedEntries.remove(i));
								}
							}
						} else {
							uncommittedEntries.put(message.getSequenceNumber(), message);
						}
					} else if (message.getHeader().equals("heartbeat")) {

					} else if (message.getHeader().equals("clientID")) {
						clientID = message.getSequenceNumber();
						file = new File("OutputFiles/OutputFile" + clientID + ".txt");
					} else if (message.getHeader().equals("voteRequestHandlerAddress")) {
						nextClientID = message.getSequenceNumber();
						if (!message.getText().equals(voteRequestHandlerAddress)) { // don't add own address
							voteRequestHandlerAddresses.add(message.getText());
						}
					} else if (message.getHeader().equals("voteRequestHandlerAddresses")) {
						nextClientID = message.getSequenceNumber();
						voteRequestHandlerAddresses = message.getStringList();
						voteRequestHandlerAddresses.remove(voteRequestHandlerAddress);
					} else if (message.getHeader().equals("removeVoteRequestHandlerAddress")) {
						voteRequestHandlerAddresses.remove(message.getText());
					} else if (message.getHeader().equals("connectToNewServer")) {
						serverAddress = message.getText();
						connectToNewServer();
					} else {
						System.err.println("Unknown header received!");
						System.err.println(message.getHeader());
					}
				}
			}
		}
	}

	private void writeToFile(String messageText) {
		synchronized (file) {
			File dir = new File("OutputFiles");
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
	}

	// monitors heartbeat and starts election
	private class heartbeatMonitorThread implements Runnable {

		@Override
		public void run() {
			while (clientRunning) {
				if (election.get()) {
					synchronized (election) {
						try {
							election.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				if (heartbeatCounter > 0) {
					heartbeatCounter--;
				} else {
					System.out.println(clientID + ": heartbeat stopped");
					election.set(true);
					synchronized (electionLock) {
						electionLock.notify();
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// handles election
	private class electionHandlerThread implements Runnable {

		@Override
		public void run() {
			while (clientRunning) {
				if (!election.get()) {
					synchronized (electionLock) {
						try {
							electionLock.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				int electionWait = (int) (Math.random() * (2000 + 1));
				try {
					Thread.sleep(electionWait);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (!clientRunning) { // prevent killed client from starting a election
					break;
				}

				if (voteRequestHandler.getElectionTerm() <= currentElectionTerm) {
					currentElectionTerm++;
					Timestamp ts = new Timestamp(new Date().getTime());
					System.out.println(clientID + ": Election for term: " + currentElectionTerm + " " + ts);
					voteRequestHandler.setElectionTerm(currentElectionTerm);
					if (voteRequestHandlerAddresses.size() == 0) {
						System.out.println(clientID + ": Start server from 322");
						startNewServer();
						scanner.close();
						closeConnection();
					} else {
						votes = 0;
						tempServerAddress = "";
						System.out.println(voteRequestHandlerAddresses);
						for (String voteRequestHandlerAddress : voteRequestHandlerAddresses) {
							new Thread(new voteRequestHandlerThread(voteRequestHandlerAddress)).start();
						}
					}
				}

				boolean serverStarted = false;
				startNewServer.set(false);
				int electionTimeout = 15;
				while (electedServerAddress == null && electionTimeout > 0) {
					if (startNewServer.get() && !serverStarted) {
						System.out.println("Start new server 336");
						startNewServer();
						serverStarted = true;
					}
					if (closeConnection) {
						scanner.close();
						closeConnection();
						closeConnection = false;
					}
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					electionTimeout--;
				}

				if (electionTimeout > 0) {
					if (clientRunning) {
						serverAddress = electedServerAddress;
						electedServerAddress = null;
						heartbeatCounter = 3;
						connectToNewServer();
					}
					election.set(false);
					synchronized (election) {
						election.notifyAll();
					}
				}
			}
		}
	}

	private class voteRequestHandlerThread implements Runnable {
		String voteRequestHandlerAddress;

		public voteRequestHandlerThread(String voteRequestHandlerAddress) {
			this.voteRequestHandlerAddress = voteRequestHandlerAddress;
		}

		@Override
		public void run() {
			ObjectOutputStream out = null;
			ObjectInputStream in = null;
			Socket socket = null;

			String address = voteRequestHandlerAddress.split(":")[0];
			int port = Integer.parseInt(voteRequestHandlerAddress.split(":")[1]);
			try {
				socket = new Socket(address, port);
				socket.setSoTimeout(15000);
			} catch (IOException u) {
				u.printStackTrace();
			}

			try {
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(socket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
			Message message = new Message();
			message.setHeader("voteRequest");
			message.setElectionTerm(currentElectionTerm);
			try {
				out.writeObject(message);
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				message = (Message) in.readObject();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}

			if (message.getHeader().equals("electionResponse")) {
				if (message.getText().equals("Yes")) {
					votes++;
				}
			} else {
				System.err.println("Unexpected header during Election!");
				System.err.println(message.getHeader());
			}

			int electionResponseTimeout = 20;
			int votesNeeded = (voteRequestHandlerAddresses.size() + 1) / 2;
			while (votes < votesNeeded && electionResponseTimeout > 0) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				electionResponseTimeout--;
			}

			if (electionResponseTimeout > 0) {
				if (!startNewServer.get()) {
					startNewServer.set(true);
					tempServerAddress = serverAddress;
				}

				while (tempServerAddress.equals(serverAddress)) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}

				message = new Message();
				message.setHeader("newLeader");
				message.setText(serverAddress);
				try {
					out.writeObject(message);
				} catch (IOException e) {
					e.printStackTrace();
				}

				while (voteRequestHandlerAddresses.size() > 0)
					;

				if (!closeConnection)
					closeConnection = true;
				;
			} else {
				message = new Message();
				message.setHeader("electionCanceled");
				try {
					out.writeObject(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setElectedServer(String electedServer) {
		this.electedServerAddress = electedServer;
	}

	public void sendMessage(String text) {
		if (election.get()) {
			synchronized (election) {
				try {
					election.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		Message message = new Message();
		try {
			message.setText(text + " " + ZonedDateTime.now() + " ID:" + clientID);
			message.setHeader("appendEntry");
			synchronized (out) {
				out.writeObject(message);
			}
		} catch (IOException c) {
			election.set(true);
			synchronized (electionLock) {
				electionLock.notify();
			}
		}
	}

	// ############################## Testing Methods #########################

	public ObjectOutputStream getOutputStream() {
		return out;
	}

	public boolean getClientRunning() {
		return clientRunning;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void stopClient() {
		clientRunning = false;
		closeConnection();
	}

	public Server getServerInstance() {
		return server;
	}

	public Vector<Message> getCommittedEntries() {
		return committedEntries;
	}

	public int getID() {
		return clientID;
	}

	public void setUncommittedEntries(int key, Message message) {
		uncommittedEntries.put(key, message);
	}

	public void startElection() {
		election.set(true);
		synchronized (electionLock) {
			electionLock.notify();
		}
	}
}
