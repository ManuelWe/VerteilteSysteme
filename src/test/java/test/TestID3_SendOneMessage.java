package test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import client.Client;
import client.Message;
import client.Server;
import client.WebClient;

public class TestID3_SendOneMessage {

	final int amountClients = 100;

	Server server = null;
	List<Client> clients = new ArrayList<Client>();
	WebClient webClient = null;

	@Before
	public void setUp() throws Exception {
		try {
			for (File file : new File("OutputFiles").listFiles())
				if (!file.isDirectory())
					file.delete();
		} catch (NullPointerException e) {

		}

		webClient = new WebClient("127.0.0.1");
		server = new Server(webClient);
		for (int i = 0; i < amountClients; i++) {
			clients.add(new Client(server.getServerAddress(), webClient, true));
		}
	}

	@Test
	public void sendOneMessage() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Message message = new Message();
		message.setText("Test " + clients.get(0) + " ID:" + clients.get(0).getID());
		message.setHeader("appendEntry");
		long startTime = System.currentTimeMillis();
		try {
			clients.get(0).getOutputStream().writeObject(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (clients.get(0).getCommittedEntries().size() == 0) {

		}

		System.err.println(System.currentTimeMillis() - startTime + "ms duration");

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@After
	public void teardown() {
		server.closeServer();
		for (Client client : clients) {
			client.stopClient();
		}
	}
}
