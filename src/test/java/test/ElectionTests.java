/**
 * 
 */
package test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import client.Client;
import client.Message;
import client.Server;
import client.WebClient;
import dhcp.DhcpServer;

public class ElectionTests {

	public static final String ip = "localhost";
	public static final String port = "5000";

	final int amountClients = 20;

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
	public void sendMessagesToServer() {
		for (int i = 0; i < clients.size(); i++) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Message message = new Message();
			message.setText("Test" + clients.get(i));
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

		assertEquals(amountClients, server.getDataList().size());
	}

	@Test
	public void serverFails() {
		String newServerAddress = "";

		try {
			Thread.sleep(15000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		server.closeServer();

		try {
			Thread.sleep(30000);
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
//
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e1) {
//			e1.printStackTrace();
//		}
//
//		server.closeServer();
//
//		try {
//			Thread.sleep(15000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		// remove new server from clients list
//		for (int i = 0; i < clients.size(); i++) {
//			if (clients.get(i).getClientRunning() == false) {
//				newServerAddress = clients.get(i).getServerAddress();
//				server = clients.get(i).getServerInstance();
//				clients.remove(i);
//			}
//		}
//
//		assertEquals(amountClients - 2, clients.size());
//
//		int count1 = 0;
//		for (int i = 0; i < clients.size(); i++) {
//			if (clients.get(i).getServerAddress().equals(newServerAddress)) {
//				count1++;
//			}
//		}
//
//		assertEquals("Only " + count1 + " clients connected to same server", amountClients - 2, count1);
	}

	@After
	public void teardown() {
		server.closeServer();
		for (Client client : clients) {
			client.stopClient();
		}
	}

}
