/**
 * 
 */
package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import client.Client;
import client.Message;
import client.Server;
import client.VoteRequestHandler;
import client.WebClient;
import dhcp.DhcpServer;

public class ElectionTests {

	public static final String ip = "localhost";
	public static final String port = "5000";

	final int amountClients = 4;

	Server server = null;
	List<Client> clients = new ArrayList<Client>();
	WebClient webClient = null;
	String serverAddress = null;
	VoteRequestHandler voteRequestHandler = null;
	static Boolean setupDone = false;

	@Before
	public void setUp() throws Exception {
		if (!setupDone) {
			// new Thread(new dhcpThread()).start();
			setupDone = true;
		}

		voteRequestHandler = new VoteRequestHandler();

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
			clients.add(new Client(serverAddress, webClient, voteRequestHandler, "a"));
		}
	}

	public class dhcpThread implements Runnable {
		public void run() {
			try {
				String a[] = {};
				DhcpServer.main(a);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Message message = new Message();
			message.setText("Test" + clients.get(i));
			message.setHeader("data");
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

		assertEquals(amountClients, server.dataList.size());
	}

	@Test
	public void serverFails() {
		String newServerAddress = "";

		server.closeServer();

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// remove new server from clients list
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getClientRunning() == false) {
				newServerAddress = clients.get(i).getServerAddress();
				clients.remove(i);
			}
		}

		assertEquals(amountClients - 1, clients.size());

		for (int i = 0; i < clients.size(); i++) {
			if (!clients.get(i).getServerAddress().equals(newServerAddress)) {
				fail("Not all clients connected to same server!");
			}
		}
	}

}