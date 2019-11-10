package eu.boxwork.dhbw.examples.webservice;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

public class ClientApp {

	WebResource service = null;

	public void initialise(String baseURL) {
		service = Client.create().resource(baseURL);
	}

	public String getServerIp() {
		Builder b = service.path("server").accept(MediaType.TEXT_PLAIN);
		return b.get(String.class);
	}

	public String getLastMessage() {
		// now we request a json but print it as String
		Builder b = service.path("hello").accept(MediaType.APPLICATION_JSON);
		String response = b.get(String.class);
		return response;
	}

	/**
	 * starts the client to connect to the webservice, IP and port has to be set as
	 * parameter like java -jar serviceclientexample.jar localhost 8080
	 * 
	 * @param args: 0 = IP, 1 = port
	 */
	public static void main(String[] args) {
		String dhcpIp = "127.0.0.1";
		String serverIp = "";
		String dhcpString = "http://" + dhcpIp + ":" + 8081 + "/";

		ClientApp app = new ClientApp();

		app.initialise(dhcpString);
		serverIp = app.getServerIp();
		String serverString = "http://" + serverIp + ":" + 8080 + "/test";
		app.initialise(serverString);
		System.out.println(app.getLastMessage());
	}
}
