package test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import client.Client;
import client.Message;
import client.Server;
import client.VoteRequestHandler;
import client.WebClient;

public class LogReplicationTests {

	public static final String ip = "localhost";
	public static final String port = "5000";

	final int amountClients = 4;

	Server server = null;
	List<Client> clients = new ArrayList<Client>();
	WebClient webClient = null;
	String serverAddress = null;
	VoteRequestHandler voteRequestHandler = null;

	@Before
	public void setUp() throws Exception {
		webClient = new WebClient("127.0.0.1");
		serverAddress = null;
		while (serverAddress == null) {
			try {
				serverAddress = webClient.getServerAddress();
			} catch (Exception c) {
				Thread.sleep(100);
			}
		}

		server = new Server(webClient);
		serverAddress = webClient.getServerAddress();
		for (int i = 0; i < amountClients; i++) {
			clients.add(new Client(serverAddress, webClient, true));
		}
	}

	@Test
	public void dhcpWorking() {
		webClient.setServerAddress("127.0.0.1:23452");
		assertEquals(webClient.getServerAddress(), "127.0.0.1:23452");
	}

	@Test
	public void sendMessagesToServer() {
		for (int i = 0; i < clients.size(); i++) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			Message message = new Message();
			message.setText("Test " + clients.get(i));
			message.setHeader("appendEntry");
			try {
				clients.get(i).getOutputStream().writeObject(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals(amountClients, server.getEntriesList().size());
	}

	@Test
	public void filesEqual() {
		sendMessagesToServer();

		// TODO check, if files are equal
	}

	@After
	public void teardown() {
		for (Client client : clients) {
			client.stopClient();
		}
	}
}
