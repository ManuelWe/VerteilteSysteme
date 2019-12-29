/**
 * 
 */
package test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import client.Client;
import client.Server;
import client.WebClient;
import dhcp.DhcpServer;

public class ElectionTests {

	public static final String ip = "localhost";
	public static final String port = "5000";

	final int amountClients = 100;

	Server server = null;
	List<Client> clients = new ArrayList<Client>();
	WebClient webClient = null;
	String serverAddress = null;
	static Boolean setupDone = false;

	@Before
	public void setUp() throws Exception {
		if (!setupDone) {
			// new Thread(new dhcpThread()).start();
			setupDone = true;
		}

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

		// TODO remove
		System.setOut(new java.io.PrintStream(System.out) {
			private StackTraceElement getCallSite() {
				for (StackTraceElement e : Thread.currentThread().getStackTrace())
					if (!e.getMethodName().equals("getStackTrace") && !e.getClassName().equals(getClass().getName()))
						return e;
				return null;
			}

			@Override
			public void println(String s) {
				println((Object) s);
			}

			@Override
			public void println(Object o) {
				StackTraceElement e = getCallSite();
				String callSite = e == null ? "??"
						: String.format("%s.%s(%s:%d)", e.getClassName(), e.getMethodName(), e.getFileName(),
								e.getLineNumber());
				super.println(o + "\t\tat " + callSite);
			}
		});
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
	public void serverFails() {
		String newServerAddress = "";

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		server.closeServer();

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// remove new server from clients list
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getClientRunning() == false) {
				newServerAddress = clients.get(i).getServerAddress();
				server = clients.get(i).getServerInstance();
				clients.remove(i);
				break;
			}
		}

		assertEquals(amountClients - 1, clients.size());

		int count = 0;
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getServerAddress().equals(newServerAddress)) {
				count++;
			}
		}
		assertEquals("Only " + count + " clients connected to same server", amountClients - 1, count);
	}

	@After
	public void teardown() {
		server.closeServer();
		for (Client client : clients) {
			client.stopClient();
		}
	}

}
