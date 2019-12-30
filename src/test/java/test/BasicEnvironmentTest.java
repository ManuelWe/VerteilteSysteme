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

import org.junit.Before;
import org.junit.Test;

import client.Client;
import client.Server;
import client.WebClient;
import dhcp.DhcpServer;

public class BasicEnvironmentTest {
	List<File> files = new ArrayList<File>();

	final int amountClients = 10; // number of clients for test / at least 8

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
	/* ClientCount: 8
	 * Following tests are executed
	 * - Sending messages and checking the files (Timeout: 20s)
	 * - Killing server and checking number of clients (Timeout: 20s)
	 * - Sending messages and checking the files (Timeout: 20s)
	 * - Killing two clients and checking number of clients (Timeout: 20s)
	 * - Sending messages and checking the files (Timeout: 20s)
	 * - Killing the dhcp server (no check possible) (Timeout: 20s)
	 * - Killing server and checking number of clients (Timeout: 20s)
	 * - Killing two clients and checking number of clients (Timeout: 20s)
	 * - Sending messages and checking the files (Timeout: 20s) 
	 */
	public void fullTest() {
		String newServerAddress = "";
		int numberOfClients = amountClients;
		Long slowDownServershutdown = 10000L;
		Long slowDownMessageSend = 10000L;
		Long slowdownKillingClients = 10000L;
		
		//Letting the system startup
		try {
			Thread.sleep(10000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		/* *************************************
		 * Test of sending messages #1 - start */
		//All of the clients sends "Hello World #1"
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getClientRunning() != false) {
				clients.get(i).sendMessage("Hello World #1");
			}
		}
		
		//Timeout for slowing down
		try {
			Thread.sleep(slowDownMessageSend);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Testing the output files
		files.clear();
		for (int i = 0; i < clients.size(); i++) {
			files.add(new File("OutputFiles/OutputFile" + clients.get(i).getID() + ".txt"));
		}
		boolean fileTest0 = true;
		for (int i = 0; i < files.size(); i++) {
			if (!(isEqual(files.get(0).toPath(), files.get(i).toPath()))) {
				fail("First file: Output" + clients.get(0).getID() + ".txt - Second file: Output" + clients.get(i).getID() + ".txt");
				fileTest0 = false;
			}
		}
		assertEquals(true, fileTest0);	
		/* Test of sending messages #1 - end
		 * *********************************/
		
		
		/* ***************************************
		 * Test of killing the server #1 - start */
		server.closeServer();
		//Timeout for slowing down
		try {
			Thread.sleep(slowDownServershutdown);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// remove new server from clients list
		numberOfClients--;	
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getClientRunning() == false) {
				newServerAddress = clients.get(i).getServerAddress();
				server = clients.get(i).getServerInstance();
				File file = new File("OutputFiles/OutputFile" + clients.get(i).getID() + ".txt");
				file.delete();
				clients.remove(i);
			}
		}
		assertEquals(numberOfClients, clients.size());
		/* Test of killing the server #1 - end
		 * ***********************************/
		
		
		/* *************************************
		 * Test of sending messages #2 - start */
		//All of the clients sends "Hello World #2"
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getClientRunning() != false) {
				clients.get(i).sendMessage("Hello World #2");
			}
		}
		
		//Timeout for slowing down
		try {
			Thread.sleep(slowDownMessageSend);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Testing the output files
		files.clear();
		for (int i = 0; i < clients.size(); i++) {
			files.add(new File("OutputFiles/OutputFile" + clients.get(i).getID() + ".txt"));
		}
		boolean fileTest1 = true;
		for (int i = 0; i < files.size(); i++) {
			if (!(isEqual(files.get(0).toPath(), files.get(i).toPath()))) {
				fail("First file: Output" + clients.get(0).getID() + ".txt - Second file: Output" + clients.get(i).getID() + ".txt");
				fileTest1 = false;
			}
		}
		assertEquals(true, fileTest1);	
		/* Test of sending messages #2 - end
		 *************************************/
		
		
		/* *************************************
		 * Testing killing two clients #1 - start */
		numberOfClients-=2;
		for (int i = 0; i < 2; i++) {
			if (clients.get(i).getClientRunning()) {
				clients.get(i).stopClient();
				clients.remove(i);
				
			}
		}
		
		//Timeout for slowing down
		try {
			Thread.sleep(slowdownKillingClients);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertEquals(numberOfClients, clients.size());
		/* Testing killing two clients #1 - end
		 * *********************************/
		
		
		/* *************************************
		 * Test of sending messages #3 - start */
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getClientRunning()) {
				clients.get(i).sendMessage("Hello World #3");
			}
		}
		//Timeout for slowing down
		try {
			Thread.sleep(slowDownMessageSend);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Testing the output files
		files.clear();
		for (int i = 0; i < clients.size(); i++) {
			files.add(new File("OutputFiles/OutputFile" + clients.get(i).getID() + ".txt"));
		}
		boolean fileTest2 = true;
		for (int i = 0; i < files.size(); i++) {
			if (!(isEqual(files.get(0).toPath(), files.get(i).toPath()))) {
				fail("First file: Output" + clients.get(0).getID() + ".txt - Second file: Output" + clients.get(i).getID() + ".txt");
				fileTest2 = false;
			}
		}
		assertEquals(true, fileTest2);	
		/* Test of sending messages #3 - end
		 * *********************************/
		
		
		/* ****************************
		 * Killing dhcp server - start*/
		 dhcpServerThread.stop();
		/* Killing dhcp server - end
		 * *************************/
		
		
		/* ***************************************
		 * Test of killing the server #2 - start */
		server.closeServer();
		//Timeout for slowing down
		try {
			Thread.sleep(slowDownServershutdown);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// remove new server from clients list
		numberOfClients--;	
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getClientRunning() == false) {
				newServerAddress = clients.get(i).getServerAddress();
				server = clients.get(i).getServerInstance();
				File file = new File("OutputFiles/OutputFile" + clients.get(i).getID() + ".txt");
				file.delete();
				clients.remove(i);
			}
		}
		assertEquals(numberOfClients, clients.size());
		/* Test of killing the server #2 - end
		 * ***********************************/
		
		/* ****************************************
		 * Testing killing two clients #2 - start */
		numberOfClients-=2;
		for (int i = 0; i < 2; i++) {
			if (clients.get(i).getClientRunning()) {
				clients.get(i).stopClient();
				clients.remove(i);
				
			}
		}
		
		//Timeout for slowing down
		try {
			Thread.sleep(slowdownKillingClients);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		assertEquals(numberOfClients, clients.size());
		/* Testing killing two clients #2 - end
		 * ************************************/
		
		
		/* *************************************
		 * Test of sending messages #4 - start */
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getClientRunning()) {
				clients.get(i).sendMessage("Hello World #3");
			}
		}
		//Timeout for slowing down
		try {
			Thread.sleep(slowDownMessageSend);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//Testing the output files
		files.clear();
		for (int i = 0; i < clients.size(); i++) {
			files.add(new File("OutputFiles/OutputFile" + clients.get(i).getID() + ".txt"));
		}
		boolean fileTest3 = true;
		for (int i = 0; i < files.size(); i++) {
			if (!(isEqual(files.get(0).toPath(), files.get(i).toPath()))) {
				fail("First file: Output" + clients.get(0).getID() + ".txt - Second file: Output" + clients.get(i).getID() + ".txt");
				fileTest3 = false;
			}
		}
		assertEquals(true, fileTest3);	
		/* Test of sending messages #4 - end
		 * *********************************/
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
