package client;

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
	private int currentElectionTerm = 0;
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
		System.out.println(voteRequestHandlerAddress);

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
					clientSocket.setSoTimeout(10000);
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
				in = new ObjectInputStream(clientSocket.getInputStream());
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

			if (!voted && message.getElectionTerm() > currentElectionTerm) {
				currentElectionTerm = message.getElectionTerm();
				System.out.println("VOTED FOR " + clientSocket + "VOTEDVOTEDVOTED " + currentElectionTerm);
				message = new Message();
				message.setHeader("electionResponse");
				message.setText("Yes");
				voted = true;
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
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}

			if (message.getHeader().equals("newLeader")) {
				System.err.println("Got leader address");
				electedServer = message.getText();
				voted = false;
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
}
