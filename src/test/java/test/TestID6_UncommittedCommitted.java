package test;

import static org.junit.Assert.assertEquals;

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

public class TestID6_UncommittedCommitted {

	final int amountClients = 100;
	final int amountUncommittedMessages = 100;

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
		server = new Server(webClient, 0);
		for (int i = 0; i < amountClients; i++) {
			clients.add(new Client(server.getServerAddress(), webClient, true));
		}
	}

	@Test
	public void uncommittedMessagesGetCommitted() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for (int i = 0; i < amountUncommittedMessages; i++) {
			Message message = new Message();
			message.setHeader("appendEntry");
			message.setText(i + " blabla" + i);
			message.setSequenceNumber(i);
			for (int j = 0; j < amountClients; j++) {
				clients.get(j).setUncommittedEntries(i, message);
			}
		}

		Message message = new Message();
		message.setHeader("commitEntry");
		message.setSequenceNumber(amountUncommittedMessages - 1);
		server.sendMessage(message);
		try {
			Thread.sleep(8000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(amountUncommittedMessages, clients.get(0).getCommittedEntries().size());
	}

	@After
	public void teardown() {
		server.closeServer();
		for (Client client : clients) {
			client.stopClient();
		}
	}
}
