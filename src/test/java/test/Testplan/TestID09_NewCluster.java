package test.Testplan;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

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
	Vector<Message> writtenMessages = new Vector<Message>();
	static Boolean setupDone = false;
	boolean output = true;
	List<File> files = new ArrayList<File>();

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

		Message message = null;
		for (int i = 0; i < clients.size(); i++) {
			message = new Message();
			message.setText("Test " + i + clients.get(i) + " ID:" + clients.get(i).getID());
			message.setHeader("appendEntry");
			try {
				synchronized (clients.get(i).getOutputStream()) {
					clients.get(i).getOutputStream().writeObject(message);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		while (clients.get(9).getCommittedEntries().size() < 10) {
		}

		File file1 = new File("OutputFiles/OutputFile3.txt");

		List<String> lines = Arrays.asList("10 bla");

		try {
			Files.write(file1.toPath(), lines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			for (File file : new File("OutputFiles").listFiles())
				if (!file.isDirectory())
					files.add(file);
		} catch (NullPointerException e) {
		}
		File biggestFile = files.get(0);
		int clientWithBiggestFile = 0;
		for (int i = 1; i < files.size(); i++) {
			if (biggestFile.length() < files.get(i).length()) {
				biggestFile = files.get(i);
			}
		}
		clientWithBiggestFile = Integer.parseInt(biggestFile.getName().split("Files")[1].split(".")[0]);

		writtenMessages = clients.get(clientWithBiggestFile).getCommittedEntries();

		server.closeServer();
		for (int i = 0; i < clients.size(); i++) {
			clients.get(i).stopClient();
		}

		client = new Client(serverAddress, webClient, true);
		Server newServer = client.getServerInstance();

		Vector<Message> committedEntries = newServer.getCommittedEntries();
		for (int i = 0; i < writtenMessages.size(); i++) {
			if (writtenMessages.get(i).getText().equals(committedEntries.get(i).getText())) {

			} else {
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
}
