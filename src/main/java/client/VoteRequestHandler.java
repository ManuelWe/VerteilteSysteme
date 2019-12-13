package client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class VoteRequestHandler {
	private ServerSocket server = null;
	private String voteRequestHandlerAddress = "";
	public int currentElectionTerm = 0;
	public String electedServer = "";
	private boolean voted = false;

	public VoteRequestHandler() {
		try {
			server = new ServerSocket(0);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String localAddress = "";
		try {
			localAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		// String serverAddress = localAddress + ":" + server.getLocalPort();
		voteRequestHandlerAddress = "127.0.0.1" + ":" + server.getLocalPort();

		new Thread(new voteRequestHandlerThread()).start();
	}

	public String getAddress() {
		return voteRequestHandlerAddress;
	}

	private class voteRequestHandlerThread implements Runnable {
		public void run() {
			Socket clientSocket = null;
			while (true) {
				try {
					clientSocket = server.accept();
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
		private Socket clientSocket;
		private ObjectInputStream in = null;
		private ObjectOutputStream out = null;

		private clientSocketThread(Socket clientSocket) throws IOException {
			this.clientSocket = clientSocket;
			try {
				out = new ObjectOutputStream(clientSocket.getOutputStream());
				in = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			Message message = null;

			try {
				message = (Message) in.readObject();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}

			if (message.getElectionTerm() > currentElectionTerm) {
				currentElectionTerm = message.getElectionTerm();
				electedServer = "";

				message = new Message();
				message.setHeader("electionResponse");
				if (voted) {
					message.setText("No");
				} else {
					System.out.println("VOTED FOR " + clientSocket + "VOTEDVOTEDVOTED");
					message.setText("Yes");
					voted = true;
				}

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

				if (message.getHeader().equals("newLeader")) {
					electedServer = message.getText();
					voted = false;
				} else {
					System.err.println("Unexpected header in election!");
				}
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
}
