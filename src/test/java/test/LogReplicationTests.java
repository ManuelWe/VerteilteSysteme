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

	public static final String ip = "localhost";
	public static final String port = "5000";

	final int amountClients = 50;

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
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals(amountClients, server.getEntriesList().size());

		for (int i = 0; i < clients.size(); i++) {
			assertEquals("" + i, amountClients, clients.get(i).getMessageList().size());
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
