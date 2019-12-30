package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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

	final int amountClients = 100;

	Server server = null;
	List<Client> clients = new ArrayList<Client>();
	WebClient webClient = null;
	String serverAddress = null;
	VoteRequestHandler voteRequestHandler = null;

	@Before
	public void setUp() throws Exception {
		try {
			for (File file : new File("OutputFiles").listFiles())
				if (!file.isDirectory())
					file.delete();
		} catch (NullPointerException e) {

		}

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
		System.err.println(System.currentTimeMillis() - startTime + "end time");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void sendMessagesToServer() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		Message message = null;
		long start = System.currentTimeMillis();
		for (int i = 0; i < clients.size(); i++) {
			message = new Message();
			message.setText("Test " + clients.get(i) + " ID:" + clients.get(i).getID());
			message.setHeader("appendEntry");
			try {
				synchronized (clients.get(i).getOutputStream()) {
					clients.get(i).getOutputStream().writeObject(message);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		while (clients.get(0).getCommittedEntries().size() < 50) {

		}
		System.err.println(System.currentTimeMillis() - start);
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals(amountClients, server.getCommittedEntries().size());

		for (int i = 0; i < clients.size(); i++) {
			assertEquals("" + i, amountClients, clients.get(i).getCommittedEntries().size());
		}
	}

	@Test
	public void filesEqual() {
		sendMessagesToServer();
		List<File> files = new ArrayList<File>();
		for (int i = 0; i < clients.size(); i++) {
			files.add(new File("OutputFiles/OutputFile" + clients.get(i).getID() + ".txt"));
		}
		boolean output = true;
		for (int i = 0; i < files.size(); i++) {
			if (!(isEqual(files.get(0).toPath(), files.get(i).toPath()))) {
				fail(clients.get(0).getID() + " " + clients.get(i).getID());
				output = false;
			}
		}
		assertEquals(true, output);
	}

	@After
	public void teardown() {
		server.closeServer();
		for (Client client : clients) {
			client.stopClient();
		}
	}

	private boolean isEqual(Path firstFile, Path secondFile) {
		try {
			if (Files.size(firstFile) != Files.size(secondFile)) {
				return false;
			}

			byte[] first = Files.readAllBytes(firstFile);
			byte[] second = Files.readAllBytes(secondFile);
			return Arrays.equals(first, second);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
