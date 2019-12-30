package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import client.Client;
import client.Message;
import client.Server;
import client.WebClient;

/*
 * Überprüft ob Nachrichten erneut versendet werden und richtig festgeschrieben werden
 * wenn ein Server mit einer uncomitteten Nachricht abstürzt
 */
public class UncommittedMessages {

	final int amountClients = 100;

	Server server = null;
	List<Client> clients = new ArrayList<Client>();
	WebClient webClient = null;
	String serverAddress = null;
	static Boolean setupDone = false;
	Message uncommittedMessage = new Message();

	@Before
	public void setUp() throws Exception {
		try { // delete all Output Files
			for (File file : new File("OutputFiles").listFiles())
				if (!file.isDirectory())
					file.delete();
		} catch (NullPointerException e) {

		}

		webClient = new WebClient("127.0.0.1");
		server = new Server(webClient, 0);
		serverAddress = webClient.getServerAddress();
		for (int i = 0; i < amountClients; i++) {
			clients.add(new Client(serverAddress, webClient, true));
		}

		uncommittedMessage.setHeader("appendEntry");
		uncommittedMessage.setText("0 UncommitedMessage " + " ID:" + "0");
		uncommittedMessage.setSequenceNumber(0);
		for (int i = 0; i < amountClients; i++) {
			clients.get(i).setUncommittedEntries(0, uncommittedMessage);
		}
	}

	@Test
	public void serverFails() {
		try {
			Thread.sleep(9000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		server.closeServer();

		try {
			System.err.println("System sleeping");
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getClientRunning() == false) {
				server = clients.get(i).getServerInstance();
				clients.remove(i);
			}
		}

		assertEquals(amountClients - 1, clients.size());

		Message message = new Message();
		message.setHeader("requestedEntry");
		message.setText("1 babababab");
		message.setSequenceNumber(1);
		server.sendMessage(message);

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		boolean messageCommitted = true;
		for (int i = 0; i < clients.size(); i++) {

			if (clients.get(i).getCommittedEntries().get(0).getSequenceNumber() == 0) {
				System.err.println("Client " + clients.get(i).getID() + " committed the message");
			} else {
				messageCommitted = false;
				break;
			}

			if (!(clients.get(i).getCommittedEntries().size() == 2)) {
				fail("client " + i + " has more than one message committed");
			}
		}
		assertEquals(true, messageCommitted);
	}

	@After
	public void teardown() {
		server.closeServer();
		for (Client client : clients) {
			client.stopClient();
		}
	}

}
