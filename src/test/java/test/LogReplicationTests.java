package test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
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
	List<File> files = new ArrayList<File>();

	@Before
	public void setUp() throws Exception {
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
	public void filesEqual() {
		sendMessagesToServer();
		for (int i = 0; i < clients.size(); i++) {
			files.add(new File("OutputFile" + clients.get(i).getPort() + ".txt")); 
		}
		boolean output = false;
		try {
			if(FileUtils.contentEquals(files.get(0), files.get(1))) {
				if(FileUtils.contentEquals(files.get(0), files.get(2))) {
					if(FileUtils.contentEquals(files.get(0), files.get(3))) {
						if(FileUtils.contentEquals(files.get(1), files.get(2))) {
							if(FileUtils.contentEquals(files.get(1), files.get(3))) {
								if(FileUtils.contentEquals(files.get(2), files.get(3))) {
									output = true;
								}
								else output = false;
							}
							else output = false;
						}
						else output = false;
					}
					else output = false;
				}
				else output = false;
			}
			else output = false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertEquals(true, output);
	}
}
