package test.Testplan;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import client.Client;
import client.Server;
import client.WebClient;
import dhcp.DhcpServer;

public class TestID10_DHCPDown {
	List<File> files = new ArrayList<File>();
	public boolean firstDHCPRun = true;
	final int amountClients = 100; // number of clients for test / at least 8

	Server server = null;
	List<Client> clients = new ArrayList<Client>();
	WebClient webClient = null;
	static Boolean setupDone = false;
	Thread dhcpServerThread;

	@Before
	public void setUp() throws Exception {
		if (!setupDone) {
			dhcpServerThread = new Thread(new dhcpThread());
			dhcpServerThread.start();
			setupDone = true;
		}

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

	public class dhcpThread implements Runnable {
		public DhcpServer dhcpServer = new DhcpServer();

		@Override
		public void run() {
			dhcpServer.startServer("127.0.0.1", "8080");
			if (firstDHCPRun) {
				try {
					Thread.sleep(10000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				dhcpServer.server.stop(0);
				firstDHCPRun = false;
			}
		}
	}

	@Test
	public void testID10() {
		// Letting the system startup
		try {
			Thread.sleep(10000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		dhcpServerThread = new Thread(new dhcpThread());
		dhcpServerThread.start();
<<<<<<< HEAD:src/test/java/test/Testplan/TestID10_DHCPDown.java

		// DHCP-Server starts, but has not the ServerAddress
		assertEquals("0", webClient.getServerAddress());

=======
		
		//DHCP-Server starts, but has not the ServerAddress
		assertEquals("null", webClient.getServerAddress()); 
		
>>>>>>> 71778b4c02d47884ba942d53fe206c19f87cd9fc:src/test/java/test/TestID10.java
		try {
			Thread.sleep(10000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// DHCP-Server is started, and has the Serveraddress
		assertEquals(server.getServerAddress(), webClient.getServerAddress());
	}
}
