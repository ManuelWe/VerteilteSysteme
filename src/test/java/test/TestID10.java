package test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import client.Client;
import client.Server;
import client.WebClient;
import dhcp.DhcpServer;
import test.BasicEnvironmentTest.dhcpThread;

public class TestID10 {
	List<File> files = new ArrayList<File>();

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
		server = new Server(webClient);
		for (int i = 0; i < amountClients; i++) {
			clients.add(new Client(server.getServerAddress(), webClient, true));
		}
	}
	
	public class dhcpThread implements Runnable {
		public DhcpServer dhcpServer = new DhcpServer();
		
		@Override
		public void run() {
			try {
				String a[] = {};
				dhcpServer.main(a);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		public void interrupt() {
			dhcpServer.stopServer();
		}
	}
	
	@Test
	public void testID10() {
		//Letting the system startup
		try {
			Thread.sleep(10000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		dhcpServerThread.interrupt();
		
		//Timeout for slowing down
		try {
			Thread.sleep(10000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		dhcpServerThread = new Thread(new dhcpThread());
		dhcpServerThread.start();
		
		assertEquals(server.getServerAddress(), webClient.getServerAddress()); 
	}
}
