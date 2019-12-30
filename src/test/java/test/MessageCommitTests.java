/**
 * 
 */
package test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import client.Client;
import client.Message;
import client.Server;
import client.WebClient;

public class MessageCommitTests {

	final int amountClients = 1; // only one needed and tested

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

	@Test
	public void uncommittedMessagesGetCommitted() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for (int i = 0; i < 5; i++) {
			Message message = new Message();
			message.setHeader("appendEntry");
			message.setText(i + " blabla" + i);
			message.setSequenceNumber(i);
			clients.get(0).setUncommittedEntries(i, message);
		}
		Message message = new Message();
		message.setHeader("commitEntry");
		message.setSequenceNumber(4);
		server.sendMessage(message);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(5, clients.get(0).getCommittedEntries().size());
	}

	@Test
	public void existingMessageGetsOverwritten() {
		uncommittedMessagesGetCommitted();

		Message message = new Message();
		message.setHeader("appendEntry");
		message.setText("2 new Message");
		message.setSequenceNumber(2);
		clients.get(0).setUncommittedEntries(2, message);

		message = new Message();
		message.setHeader("commitEntry");
		message.setSequenceNumber(2);
		server.sendMessage(message);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals(5, clients.get(0).getCommittedEntries().size());
		assertEquals("2 new Message", clients.get(0).getCommittedEntries().get(2).getText());
	}

	@After
	public void teardown() {
		for (Client client : clients) {
			client.stopClient();
		}
		server.closeServer();
	}

}
