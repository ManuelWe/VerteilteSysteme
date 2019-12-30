package test.Testplan;

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

public class TestID09_NewCluster {

	Server server = null;
	List<Client> clients = new ArrayList<Client>();
	WebClient webClient = null;
	String serverAddress = null;
	Client client;
	static Boolean setupDone = false;

	@Before
	public void setUp() throws Exception {
		webClient = new WebClient("127.0.0.1");
		serverAddress = null;
		server = new Server(webClient, 0);
		serverAddress = webClient.getServerAddress();
	}

	@Test
	public void createNewCluster() {

		for (int i = 0; i < 10; i++) {
			clients.add(new Client(serverAddress, webClient, true));
		}
		while (server.getOutputStreams().size() < 10) {
		}
		Message message = new Message();
		message.setText("Test " + clients.get(0) + " ID:" + clients.get(0).getID());
		message.setHeader("appendEntry");
		try {
			clients.get(0).getOutputStream().writeObject(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
		message.setText("Test2 " + clients.get(0) + " ID:" + clients.get(0).getID());
		message.setHeader("appendEntry");
		try {
			clients.get(0).getOutputStream().writeObject(message);
		} catch (IOException e) {
			e.printStackTrace();
		}

		server.closeServer();
		for (int i = 0; i < clients.size(); i++) {
			clients.get(i).stopClient();
		}

		client = new Client(serverAddress, webClient, true);
		System.err.println(client.getCommittedEntries());
		while (true) {
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
