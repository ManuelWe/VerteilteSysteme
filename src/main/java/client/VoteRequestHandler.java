package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

public class VoteRequestHandler {
	private ServerSocket server = null;
	private volatile String voteRequestHandlerAddress = "";
	private volatile int currentElectionTerm = 0;
	private AtomicBoolean voted = new AtomicBoolean(false);
	private Client client = null;

	public VoteRequestHandler(Client client) {
		this.client = client;
		try {
			server = new ServerSocket(0);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String localAddress = null;
		try (final DatagramSocket socket = new DatagramSocket()) {
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			localAddress = socket.getLocalAddress().getHostAddress();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		// voteRequestHandlerAddress = localAddress + ":" + server.getLocalPort();
		voteRequestHandlerAddress = "127.0.0.1" + ":" + server.getLocalPort();

		new Thread(new voteRequestHandlerThread()).start();
	}

	private class voteRequestHandlerThread implements Runnable {
		@Override
		public void run() {
			Socket clientSocket = null;
			while (client.getClientRunning()) {
				try {
					clientSocket = server.accept();
					clientSocket.setSoTimeout(15000);
				} catch (SocketException e) {
					if (client.getClientRunning()) {
						e.printStackTrace();
					} else {
						break;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					new Thread(new clientSocketThread(clientSocket)).start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class clientSocketThread implements Runnable {
		private Socket clientSocket = null;
		private ObjectInputStream in = null;
		private ObjectOutputStream out = null;

		private clientSocketThread(Socket clientSocket) throws IOException {
			this.clientSocket = clientSocket;
			try {
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				in = new ObjectInputStream(clientSocket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			Message message = null;

			try {
				message = (Message) in.readObject();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}

			if (message.getElectionTerm() > currentElectionTerm) {
				voted.set(false);
				currentElectionTerm = message.getElectionTerm();
			}

			if (!voted.get()) {
				voted.set(true);
				message = new Message();
				message.setHeader("electionResponse");
				message.setText("Yes");
			} else {
				message = new Message();
				message.setHeader("electionResponse");
				message.setText("No");
			}

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
				System.err.println("TIMEOUT ON requesthandler");
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (message.getHeader().equals("newLeader")) {
				client.setElectedServer(message.getText());
			} else if (message.getHeader().equals("electionCanceled")) {
			} else {
				System.err.println("Unexpected header in election!");
				System.err.println(message.getHeader());
			}

			closeConnection();
		}

		private void closeConnection() {
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
		}
	}

	public void setElectionTerm(int newElectionTerm) {
		currentElectionTerm = newElectionTerm;
	}

	public int getElectionTerm() {
		return currentElectionTerm;
	}

	public String getAddress() {
		return voteRequestHandlerAddress;
	}

	public void close() {
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
