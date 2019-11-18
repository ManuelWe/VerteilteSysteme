package client;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

//sends random text to server in random intervals; closed when server down
public class botClient implements Runnable {
	// initialize socket and input output streams
	private Socket socket = null;
	private DataInputStream in = null;
	private DataOutputStream out = null;
	private String clientID = "";
	private boolean threadRunning = true;
	private WebClient webClient = null;
	private String clientAddress = null;

	public botClient(String serverAddress, WebClient webClient) {
		this.webClient = webClient;
		String address = serverAddress.split(":")[0];
		int port = Integer.parseInt(serverAddress.split(":")[1]);

		try {
			socket = new Socket(address, port);
		} catch (IOException u) {
			u.printStackTrace();
		}

		String ip = socket.getLocalAddress().getHostAddress();
		int clientPort = socket.getLocalPort();
		clientAddress = ip + ":" + clientPort;
		webClient.addClientAddress(clientAddress);

		try {
			out = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		int timeout;
		String line;

		try {
			in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			clientID = in.readUTF();
		} catch (IOException e) {
			e.printStackTrace();
		}

		while (threadRunning) {
			timeout = (int) ((Math.random() * 6000) + 2000);
			try {
				Thread.sleep(timeout);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			line = randomText() + " " + sdf.format(cal.getTime()) + " " + clientID;
			try {
				out.writeUTF(line);
			} catch (IOException c) {
				closeConnection();
			}
		}
	}

	private void closeConnection() {
		webClient.removeClientAddress(clientAddress);

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
		System.out.println("Connection closed");
		threadRunning = false;
	}

	private String randomText() {
		int leftLimit = 65; // letter 'A'
		int rightLimit = 122; // letter 'z'
		int targetStringLength = 20;
		Random random = new Random();
		StringBuilder buffer = new StringBuilder(targetStringLength);
		for (int i = 0; i < targetStringLength; i++) {
			int randomLimitedInt = leftLimit + (int) (random.nextFloat() * (rightLimit - leftLimit + 1));
			buffer.append((char) randomLimitedInt);
		}
		String generatedString = buffer.toString();
		return generatedString;
	}
}
