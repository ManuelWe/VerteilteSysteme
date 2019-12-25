package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
	private ServerSocket server = null;
	private AtomicInteger connections = new AtomicInteger(0);
	private Vector<ObjectOutputStream> outputStreams = new Vector<ObjectOutputStream>();
	private BlockingQueue<Message> messageList = new LinkedBlockingQueue<Message>();
	private Vector<Message> dataList = new Vector<Message>();
	private CopyOnWriteArrayList<String> voteRequestHandlerAddresses = new CopyOnWriteArrayList<String>();
	private String serverAddress = "";
	private boolean serverRunning = true;
	private AtomicBoolean messageSent = new AtomicBoolean(false);

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
		webClient.setServerAddress(serverAddress);

		new Thread(new messageSenderThread()).start();
		new Thread(new heartbeatThread()).start();
		new Thread(new benchmarkingThread()).start();

		System.out.println("SERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVERSERVER");
		System.out.println("Log files: ");

		new Thread(new serverThread()).start();
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
			message.setList(voteRequestHandlerAddresses);
			out.writeObject(message);

			outputStreams.add(out);
		}

		public void run() {
			int clientID = connections.getAndIncrement();

			System.out.println("Client " + clientID + " is now connected to the socket");
			Message message = null;
			do {
				try {
					message = (Message) in.readObject();
					if (message.getHeader().equals("data")) {
						System.out.println("Client " + clientID + ": \"" + message.getText() + "\"");
						if (!message.getText().equals("Over")) {
							try {
								messageList.put(message);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							dataList.add(message);
						}
					} else if (message.getHeader().equals("voteRequestHandlerAddress")) {
						voteRequestHandlerAddresses.add(message.getText());
						voteRequestHandlerAddress = message.getText();
						try {
							messageList.put(message);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
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
							it.next().writeObject(message);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
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
				// only send heartbeat, when no message was sent
				if (!messageSent.getAndSet(false)) {
					try {
						messageList.put(message);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	// TODO remove
	private class benchmarkingThread implements Runnable {
		public void run() {
			while (serverRunning) {
				if (messageList.size() > 15) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (messageList.size() > 15) {
						System.err.println("Too many messages in queue " + messageList.size());
						System.exit(0);
					}
				}
			}
		}
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public Vector<Message> getDataList() {
		return dataList;
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