package client;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

public class WebClient {

	private WebResource service = null;

	public WebClient() {
		String dhcpIp = "127.0.0.1";
		String dhcpString = "http://" + dhcpIp + ":" + 8080 + "/";

		service = Client.create().resource(dhcpString);
	}

	public void setServerAddress(String serverAddress) {
		WebResource b = service.path("dhcp/server");
		b.post(String.class, serverAddress);
	}

	public String getServerAddress() {
		Builder b = service.path("dhcp/server").accept(MediaType.TEXT_PLAIN);
		return b.get(String.class);
	}

}
