package client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Client {
	// initialize socket and scanner output streams
	private Socket socket = null;
	private Scanner scanner;
	private ObjectInputStream in = null;
	private ObjectOutputStream out = null;
	private WebClient webClient = null;
	private String serverAddress = null;
	private List<String> messageList = new ArrayList<String>();
	private int heartbeatCounter = 3;
	private int currentElectionTerm = 0;
	private boolean election = false;
	private VoteRequestHandler voteRequestHandler = null;
	private String voteRequestHandlerAddress = "";
	private List<String> voteRequestHandlerAddresses = new ArrayList<String>();
	private int votes = 0;
	private boolean startNewServer = false;
	private boolean closeConnection = false;
	private boolean clientRunning = true;
	private boolean connectToNewServer = false;

	public Client(String serverAddress, WebClient webClient, VoteRequestHandler voteRequestHandler, String a) {
		this.webClient = webClient;
		this.serverAddress = serverAddress;
		this.voteRequestHandler = voteRequestHandler;
		voteRequestHandlerAddress = voteRequestHandler.getAddress();

		System.out.println("CLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENT");

		String address = serverAddress.split(":")[0];
		int port = Integer.parseInt(serverAddress.split(":")[1]);
		System.out.println(address + ":" + port);
		try {
			socket = new Socket(address, port);
			try {
				scanner = new Scanner(System.in);
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}

			new Thread(new messageReceiverThread()).start();
			new Thread(new heartbeatMonitorThread()).start();
			new Thread(new electionHandlerThread()).start();

			Message message = new Message();
			message.setHeader("voteRequestHandlerAddress");
			message.setText(voteRequestHandlerAddress);
			try {
				out.writeObject(message);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException u) {
			u.printStackTrace();
			startNewServer();
		}
	}

	// constructor to put ip address and port
	public Client(String serverAddress, WebClient webClient, VoteRequestHandler voteRequestHandler) {
		this.webClient = webClient;
		this.serverAddress = serverAddress;
		this.voteRequestHandler = voteRequestHandler;
		voteRequestHandlerAddress = voteRequestHandler.getAddress();

		System.out.println("CLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENTCLIENT");

		String address = serverAddress.split(":")[0];
		int port = Integer.parseInt(serverAddress.split(":")[1]);
		System.out.println(address + ":" + port);
		try {
			socket = new Socket(address, port);
			try {
				scanner = new Scanner(System.in);
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}

			new Thread(new messageReceiverThread()).start();
			new Thread(new heartbeatMonitorThread()).start();
			new Thread(new electionHandlerThread()).start();

			Message message = new Message();
			message.setHeader("voteRequestHandlerAddress");
			message.setText(voteRequestHandlerAddress);
			try {
				out.writeObject(message);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			clientMain();
		} catch (IOException u) {
			startNewServer();
		}
	}

	private void clientMain() {
		Message message = null;
		do {
			System.out.printf("Your input: ");
			message = new Message();
			try {
				message.setText(scanner.nextLine());
				message.setHeader("data");
				out.writeObject(message);
			} catch (IOException c) {
				election = true;
			}
		} while ((!message.getText().equals("Over")));
		System.out.println("Closing connection to server");
		scanner.close();
		message = new Message();
		message.setHeader("removeVoteRequestHandlerAddress");
		message.setText(voteRequestHandlerAddress);
		try {
			out.writeObject(message);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

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
		Server server = new Server(webClient);
		serverAddress = server.serverAddress;
		voteRequestHandlerAddresses.remove(voteRequestHandlerAddress);
		server.voteRequestHandlerAddresses = voteRequestHandlerAddresses;
		// server.removeVoteRequestHandlerAddress(voteRequestHandlerAddress); //TODO:
		// also remove in voterequest handler
		clientRunning = false;
	}

	private void connectToNewServer() {
		connectToNewServer = true;
		System.out.println("Connecting to new server " + serverAddress);
		closeConnection();
		currentElectionTerm++;

		try {
			socket = new Socket(serverAddress.split(":")[0], Integer.parseInt(serverAddress.split(":")[1]));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.printf("Your input: ");
	}

	// receives messages from server
	public class messageReceiverThread implements Runnable {
		int counter = 0;

		public void run() {
			Message message = null;
			while (clientRunning) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				try {
					message = (Message) in.readObject();
				} catch (SocketException s) {
					System.out.println(connectToNewServer);
					if (connectToNewServer) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						connectToNewServer = false;
					} else {
						election = true;
					}
				} catch (IOException e) {
					election = true;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				try {
					message.getHeader();
				} catch (Exception e) {
					System.out.println(message + "kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk");
					System.out.println(message.getHeader());
					System.out.println(message.getText());
					break;
				}
				if (message.getHeader().equals("data")) {
					messageList.add(message.getText());
					writeToFile(message.getText());
					System.out.println(messageList);
				} else if (message.getHeader().equals("heartbeat")) {
					heartbeatCounter++;
				} else if (message.getHeader().equals("voteRequestHandlerAddress")) {
					if (!message.getText().equals(voteRequestHandlerAddress)) { // don't add own address
						voteRequestHandlerAddresses.add(message.getText());
					}
					System.out.println(voteRequestHandlerAddresses);
				} else if (message.getHeader().equals("voteRequestHandlerAddresses")) {
					voteRequestHandlerAddresses = message.getList();
				} else if (message.getHeader().equals("removeVoteRequestHandlerAddress")) {
					voteRequestHandlerAddresses.remove(message.getText());
					System.out.println(voteRequestHandlerAddresses);
				} else {
					System.err.println("Unknown header received!");
					System.err.println(message.getHeader());
				}
			}
		}
	}

	private void writeToFile(String messageText) {
		File dir = new File("OutputFiles");
		File file = new File("OutputFiles/OutputFile" + socket.getLocalPort() + ".txt");
		try {
			dir.mkdir();
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		List<String> lines = Arrays.asList(messageText);
		Path filePath = Paths.get("OutputFiles/OutputFile" + socket.getLocalPort() + ".txt");

		try {
			Files.write(filePath, lines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
		} catch (IOException e) {
			try {
				Files.write(filePath, lines, StandardCharsets.UTF_8);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	// monitors heartbeat and starts election
	public class heartbeatMonitorThread implements Runnable {

		public void run() {
			while (clientRunning && !election) {
				if (heartbeatCounter > 0) {
					heartbeatCounter--;
				} else if (heartbeatCounter == 0) {
					System.out.println("heartbeat stopped");
					election = true;
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
	public class electionHandlerThread implements Runnable {

		public void run() {
			while (clientRunning) {
				if (election) {
					int electionTimeout = (int) (Math.random() * ((300 - 150) + 1)) + 150;
					try {
						Thread.sleep(electionTimeout);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					if (voteRequestHandler.currentElectionTerm == currentElectionTerm) {
						System.out.println("Election");
						currentElectionTerm++;
						if (voteRequestHandlerAddresses.size() == 0) {
							startNewServer();
							scanner.close();
							closeConnection();
						} else {
							votes = 0;
							for (String voteRequestHandlerAddress : voteRequestHandlerAddresses) {
								new Thread(new voteRequestHandlerThread(voteRequestHandlerAddress)).start();
							}
						}
					}

					while (voteRequestHandler.electedServer.equals("")) {
						if (startNewServer) {
							startNewServer();
							startNewServer = false;
						}
						if (closeConnection) {
							scanner.close();
							closeConnection();
							closeConnection = false;
						}
					}
					if (clientRunning) {
						System.out.println(voteRequestHandlerAddresses);
						serverAddress = voteRequestHandler.electedServer;
						connectToNewServer();
					}
					election = false;
					heartbeatCounter = 3;
				}
			}
		}
	}

	public class voteRequestHandlerThread implements Runnable {
		private String voteRequestHandlerAddress;

		public voteRequestHandlerThread(String voteRequestHandlerAddress) {
			this.voteRequestHandlerAddress = voteRequestHandlerAddress;
		}

		public void run() {
			ObjectOutputStream out = null;
			ObjectInputStream in = null;
			Socket socket = null;

			String address = voteRequestHandlerAddress.split(":")[0];
			int port = Integer.parseInt(voteRequestHandlerAddress.split(":")[1]);
			try {
				socket = new Socket(address, port);
			} catch (IOException u) {
				u.printStackTrace();
			}

			try {
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
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

			System.out.println(votes);
			while (votes < (voteRequestHandlerAddresses.size() + 1) / 2) {
				System.out.println("votes" + votes);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("votes" + votes);

			String tempServerAddress = serverAddress;
			if (!startNewServer)
				startNewServer = true;
			;
			while (tempServerAddress.equals(serverAddress))
				;

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
	
	public String getPort() {
		return "" + socket.getLocalPort();
	}
}
