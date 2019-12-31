package client;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
	// initialize socket and scanner output streams
	private Socket socket = null;
	private Scanner scanner;
	private ObjectInputStream in = null;
	private ObjectOutputStream out = null;

	private WebClient webClient = null;
	private Server server = null;
	private String serverAddress = null;
	private volatile boolean clientRunning = true;
	private boolean automatedTest = false;
	private int nextClientID = 1;
	private int clientID = 0;

	// election variables
	private int heartbeatCounter = 4;
	private int currentElectionTerm = 0;
	private AtomicBoolean election = new AtomicBoolean(false);
	private VoteRequestHandler voteRequestHandler = null;
	private String voteRequestHandlerAddress = "";
	private List<String> voteRequestHandlerAddresses = new ArrayList<String>();
	private volatile int votes = 0;
	private AtomicBoolean startNewServer = new AtomicBoolean(false);
	private boolean closeConnection = false;
	private boolean connectToNewServer = false;
	private String tempServerAddress = "";
	private volatile String electedServerAddress = null;
	private Object electionLock = new Object();

	// log replication variables
	private Map<Integer, Message> uncommittedEntries = new HashMap<Integer, Message>();
	private Vector<Message> committedEntries = new Vector<Message>();
	private int nextSequenceNumber = 0;
	private int highestCommittedSequenceNumber = 0;
	private File file = null;
	private BlockingQueue<String> messageTextQueue = new LinkedBlockingQueue<String>();

	/*
	 * Konstruktor versucht einen clientsocket mit der angegebenen server addresse
	 * zu verbinden und messageReceiverThread, heartbeatMonitorThread und
	 * electionHandlerThread werden gestartet. Der client sendet seine ID (0, wenn
	 * noch keine erhalten) und sendet seine voteRequestHandler addresse. Wenn der
	 * client nicht im rahmen eines automatisierten tests gestartet wurde, soll dem
	 * nutzer die möglichkeit zum eingeben von nachrichten gegeben werden. Schlägt
	 * der verbindungsaufbau zur server addresse fehl, wird der client zum server.
	 * INPUTS: ip und port des servers; instanz des webClients; true, wenn
	 * aufgerufen von automatisiertem test OUTPUTS: keine AUTOR: Manuel VERSION:
	 * 1.0.1 ERSTELLT: 10.11.19 GEÄNDERT: 30.12.19
	 */
	public Client(String serverAddress, WebClient webClient, boolean automatedTest) {
		this.webClient = webClient;
		this.serverAddress = serverAddress;
		this.automatedTest = automatedTest;
		voteRequestHandler = new VoteRequestHandler(this);
		voteRequestHandlerAddress = voteRequestHandler.getAddress();

		if (!automatedTest) {
			System.out.println("A client was started");
		}

		String address = serverAddress.split(":")[0];
		int port = Integer.parseInt(serverAddress.split(":")[1]);
		try {
			socket = new Socket(address, port);

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
		}
	}

	/*
	 * Ermöglicht dem Nutzer die eingabe von nachrichten und sendet diese zum
	 * server; Pausiert während der election; Wenn nutzer over schreibt, oder der
	 * client nicht mehr läuft, schließe offene verbindungen und terminiere INPUTS:
	 * keine OUTPUTS: keine AUTOR: Manuel VERSION: 1.0.0 ERSTELLT: 10.11.19
	 * GEÄNDERT: 30.12.19
	 */
	private void clientMain() {
		Message message = null;
		String input = "";
		do {
			if (!automatedTest) {
				System.out.println("******************************************************");
				System.out.println("Your input: ");
			}
			input = scanner.nextLine();
			if (!input.equals("over") && !election.get()) {
				message = new Message();
				try {
					message.setText(input + " " + ZonedDateTime.now() + " ID:" + clientID);
					message.setHeader("appendEntry");
					synchronized (out) {
						out.writeObject(message);
					}
					if (!automatedTest) {
						System.out.println("******************************************************");
						System.out.println("Message was send to server ... it might take a while");
						System.out.println("for processing");
					}
				} catch (IOException c) {
					c.printStackTrace();
					election.set(true);
					synchronized (electionLock) {
						electionLock.notify();
					}
				}
			}
		} while (clientRunning && !input.equals("over"));
		if (!automatedTest) {
			System.out.println("******************************************************");
			System.out.println("Closing connection to server");
		}
		scanner.close();
		clientRunning = false;
		voteRequestHandler.close();
		closeConnection();
		System.exit(0);
	}

	/*
	 * Schließe input-/output stream und socket INPUTS: keine OUTPUTS: keine AUTOR:
	 * Manuel VERSION: 0.1.1 ERSTELLT: 10.11.19 GEÄNDERT: 11.11.19
	 */
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

	/*
	 * Erstellt server instanz; ließt server addresse, des neu erstellten servers
	 * aus; beendet client und voteRequestHandler des clients INPUTS: keine OUTPUTS:
	 * keine AUTOR: Manuel VERSION: 1.0.0 ERSTELLT: 10.11.19 GEÄNDERT: 30.12.19
	 */
	private void startNewServer() {
		server = new Server(webClient, clientID, nextClientID, uncommittedEntries, nextSequenceNumber);
		serverAddress = server.getServerAddress();
		clientRunning = false;
		voteRequestHandler.close();
	}

	/*
	 * Schließt offene verbindungen; löscht uncommittete nachrichten und baut
	 * verbindung zu neuem server auf; sendet clientID und
	 * voteRequestHandlerAddresse INPUTS: keine OUTPUTS: keine AUTOR: Manuel
	 * VERSION: 1.1.1 ERSTELLT: 16.11.19 GEÄNDERT: 30.12.19
	 */
	private void connectToNewServer() {
		connectToNewServer = true;
		if (!automatedTest) {
			System.out.println("******************************************************");
			System.out.println("Connecting to new server " + serverAddress);
		}
		closeConnection();
		currentElectionTerm = voteRequestHandler.getElectionTerm();
		uncommittedEntries.clear();

		try {
			socket = new Socket(serverAddress.split(":")[0], Integer.parseInt(serverAddress.split(":")[1]));
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
		if (!automatedTest) {
			System.out.println("******************************************************");
			System.out.println("Your input: ");
		}
	}

	/*
	 * Schließt vom server eingehende nachrichten und verarbeitet diese. Setzt
	 * heartbeat counter beim empfangen einer nachricht auf 4. Pausiert, während der
	 * election. Startet election, wenn socket unerwartet vom server geschlossen
	 * wird. INPUTS: keine OUTPUTS: keine AUTOR: Manuel VERSION: 1.2.2 ERSTELLT:
	 * 18.11.19 GEÄNDERT: 30.12.19
	 */
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
					// wait until connected to new server
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
						// add message to list of uncommitted entries and send acknowledge message
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
						// commit entry if possible; request entries if not in uncommitted entries
						if (message.getSequenceNumber() > highestCommittedSequenceNumber) {
							highestCommittedSequenceNumber = message.getSequenceNumber();
						}
						if (committedEntries.size() <= message.getSequenceNumber()) {
							for (int i = committedEntries.size(); i <= message.getSequenceNumber(); i++) {
								// if entry is available, commit if it has the following sequence number
								if (uncommittedEntries.containsKey(i)) {
									if (committedEntries.size() == i) {
										messageText = uncommittedEntries.get(i).getText();
										try {
											messageTextQueue.put(messageText);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
										if (!automatedTest) {
											System.out
													.println("******************************************************");
											System.out.println("Message \"" + messageText + "\" committed");
											System.out
													.println("******************************************************");
											System.out.println("Your input: ");
										}
										committedEntries.add(uncommittedEntries.remove(i));
									}
								} else {
									// if entry not available, request
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
							// if entry is already committed, substitute existing message with new one
							// (server hat immer recht)
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
						// add received list to own list, which should be empty
						committedEntries.addAll(message.getMessageList());
						file.delete();
						for (Message newMessage : message.getMessageList()) {
							try {
								messageTextQueue.put(newMessage.getText());
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						if (message.getMessageList().size() > 0) {
							if (!automatedTest) {
								System.out.println("******************************************************");
								System.out.println("Added " + message.getMessageList().size() + " messages to file!");
							}
						}
					} else if (message.getHeader().equals("requestedEntry")) {
						// commit entry, if sequence number is following, else add entry to list of
						// uncommitted; check if following entries can also be committed.
						if (committedEntries.size() == message.getSequenceNumber()) {

							System.out.println(clientID + ": " + message.getText() + " committed");

							try {
								messageTextQueue.put(message.getText());
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
							if (!automatedTest) {
								System.out.println("******************************************************");
								System.out.println("Message \"" + message.getText() + "\" committed");
							}
							committedEntries.add(message);
							uncommittedEntries.remove(message.getSequenceNumber());
							// check, if following entries were already committed by server and can be
							// committed by client
							for (int i = committedEntries.size(); i <= highestCommittedSequenceNumber
									&& committedEntries.size() == i; i++) {
								if (uncommittedEntries.containsKey(i)) {
									messageText = uncommittedEntries.get(i).getText();
									if (!automatedTest) {
										System.out.println("******************************************************");
										System.out.println("Message \"" + messageText + "\" committed");
									}
									try {
										messageTextQueue.put(messageText);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									committedEntries.add(uncommittedEntries.remove(i));
								}
							}
						} else {
							uncommittedEntries.put(message.getSequenceNumber(), message);
						}
					} else if (message.getHeader().equals("heartbeat")) {
						// heartbeatCounter is resetted after every message
					} else if (message.getHeader().equals("clientID")) {
						clientID = message.getSequenceNumber();
						file = new File("OutputFiles/OutputFile" + clientID + ".txt");
						new Thread(new fileWriterThread()).start();
					} else if (message.getHeader().equals("voteRequestHandlerAddress")) {
						nextClientID = message.getSequenceNumber();
						if (!message.getText().equals(voteRequestHandlerAddress)) { // don't add own address
							voteRequestHandlerAddresses.add(message.getText());
						}
					} else if (message.getHeader().equals("voteRequestHandlerAddresses")) {
						// nextClientID is send together with voteRequestHandlerAddresses
						nextClientID = message.getSequenceNumber();
						voteRequestHandlerAddresses = message.getStringList();
						voteRequestHandlerAddresses.remove(voteRequestHandlerAddress);
					} else if (message.getHeader().equals("removeVoteRequestHandlerAddress")) {
						voteRequestHandlerAddresses.remove(message.getText());
					} else if (message.getHeader().equals("connectToNewServer")) {
						// if server tells client to connect to new server, do so
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

	/*
	 * Konsumiere nachricht aus messageTextQueue und schreibe diese ins file INPUTS:
	 * keine OUTPUTS: keine AUTOR: Manuel VERSION: 1.0.2 ERSTELLT: 30.12.19
	 * GEÄNDERT: 31.12.19
	 */
	private class fileWriterThread implements Runnable {
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
			while (clientRunning) {
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
	 * Verringert den HeartbeatCounter jede sekunde um 1; pausiert während der
	 * election INPUTS: keine OUTPUTS: keine AUTOR: Manuel VERSION: 0.9.2 ERSTELLT:
	 * 10.11.19 GEÄNDERT: 11.11.19
	 */
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

	/*
	 * Wartet bis eine election gestartet wird und schläft für eine zufällige zeit;
	 * startet election, wenn der voteRequestHandler noch nicht gevoted hat. Für die
	 * elektion wird pro bekanntem vote equest handler ein thread gestartet. Warte,
	 * bis eine server addresse empfangen wurde, oder ein timeout passiert. Verbinde
	 * zu neuem server, wenn dieser gewählt oder starte elektion für nächsten term.
	 * INPUTS: keine OUTPUTS: keine AUTOR: Manuel VERSION: 1.4.8 ERSTELLT: 24.11.19
	 * GEÄNDERT: 29.12.19
	 */
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

				// only start election, if voteRequestHandler hasn't voted already in this term
				if (voteRequestHandler.getElectionTerm() <= currentElectionTerm) {
					currentElectionTerm++;
					Timestamp ts = new Timestamp(new Date().getTime());
					if (!automatedTest) {
						System.out.println("******************************************************");
						System.out.println("Election started");
					}
					voteRequestHandler.setElectionTerm(currentElectionTerm);
					if (voteRequestHandlerAddresses.size() == 0) {
						startNewServer();
						scanner.close();
						closeConnection();
					} else {
						votes = 0;
						tempServerAddress = "";
						for (String voteRequestHandlerAddress : voteRequestHandlerAddresses) {
							new Thread(new voteRequestHandlerThread(voteRequestHandlerAddress)).start();
						}
					}
				}

				boolean serverStarted = false;
				startNewServer.set(false);
				int electionTimeout = 20;
				while (electedServerAddress == null && electionTimeout > 0) {
					if (startNewServer.get() && !serverStarted) {
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
						heartbeatCounter = 4;
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

	/*
	 * Baut verbindung zu einem voteRequesthandler eines bekannten clients auf und
	 * fordert einen vote. Sind genügend stimmen gesammelt, werde zum server und
	 * sende neue server addresse an voteRequestHandler. Wenn nicht genügend votes
	 * gesammelt werden konnten, benachrichtige voteRequestHandler und schließe
	 * verbindung. INPUTS: keine OUTPUTS: keine AUTOR: Manuel VERSION: 1.1.8
	 * ERSTELLT: 30.11.19 GEÄNDERT: 31.12.19
	 */
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
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SocketTimeoutException e) {
				System.err.println("TIMEOUT ON CLIENT");
			} catch (IOException e) {
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
		voteRequestHandler.close();
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
}
