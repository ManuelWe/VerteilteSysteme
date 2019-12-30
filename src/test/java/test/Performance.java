package test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import client.Client;
import client.Message;
import client.Server;
import client.WebClient;

public class Performance {

	final int amountClients = 100;
	final int amountMessages = 100;

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
	public void sendMessages() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long startTime = System.currentTimeMillis();

		for (int i = 0; i < amountMessages; i++) {
			Message message = new Message();
			message.setText("Test " + clients.get(0) + " ID:" + clients.get(0).getID());
			message.setHeader("appendEntry");
			try {
				synchronized (clients.get(0).getOutputStream()) {
					clients.get(0).getOutputStream().writeObject(message);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		while (clients.get(amountClients - 1).getCommittedEntries().size() < amountMessages) {

		}

		System.err.println(System.currentTimeMillis() - startTime + "ms duration");

		assertEquals(amountMessages, server.getCommittedEntries().size());

		for (int i = 0; i < clients.size(); i++) {
			assertEquals("" + i, amountMessages, clients.get(i).getCommittedEntries().size());
			System.err.println(clients.get(i).getCommittedEntries().size());
		}

		JUnitCore jUnitCore = new JUnitCore();
		Result result = jUnitCore.run(CorrectSequenceNumberOrder.class);
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.err.printf("Test ran: %s, Failed: %s%n", result.getRunCount(), result.getFailureCount());
		assertEquals("Sequence numbers are not correct!", 0, result.getFailureCount());

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
