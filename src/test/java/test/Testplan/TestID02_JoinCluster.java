package test.Testplan;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import client.Client;
import client.Server;
import client.WebClient;

public class TestID02_JoinCluster {

	final int amountClients = 500;

	Server server = null;
	List<Client> clients = new ArrayList<Client>();
	WebClient webClient = null;
	String serverAddress = null;
	static Boolean setupDone = false;

	@Before
	public void setUp() throws Exception {
		webClient = new WebClient("127.0.0.1");
		serverAddress = null;
		server = new Server(webClient);
		serverAddress = webClient.getServerAddress();
	}

	@Test
	public void connectClients() {
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < amountClients; i++) {
			clients.add(new Client(serverAddress, webClient, true));
		}
		while (server.getOutputStreams().size() < amountClients) {
		}
		long endTime = System.currentTimeMillis();
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getClientRunning() == false) {
				clients.remove(i);
			}
		}

		assertEquals(amountClients, clients.size());
		int count = 0;
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getServerAddress().equals(serverAddress)) {
				count++;
			}
		}
		assertEquals("Only " + count + " clients are connected to the server", amountClients, count);
		System.err.println(endTime - startTime);

	}

	@After
	public void teardown() {
		server.closeServer();
		for (Client client : clients) {
			client.stopClient();
		}
	}
}
