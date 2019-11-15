package dhcp;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.spi.resource.Singleton;

@Path("dhcp")
@Singleton
public class Service {
	FileHandler fileHandler;

	public void setup() {
		if (fileHandler == null) {
			fileHandler = new FileHandler();
		}
	}

	@GET
	@Path("server")
	@Produces(MediaType.TEXT_PLAIN)
	public String helloHTML() {
		setup();
		return fileHandler.getLastFileEntry();
	}

	@POST
	@Path("server")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public String helloWorldJSON(String serverAddress) {
		setup();
		fileHandler.setServerAddress(serverAddress);
		return ("OK");
	}

	@POST
	@Path("clients")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public String clients(String clientAddress) {
		setup();
		fileHandler.addClientAddress(clientAddress);
		return ("OK");
	}

	@DELETE
	@Path("clients")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public String deleteClient(String clientAddress) {
		setup();
		fileHandler.removeClientAddress(clientAddress);
		return ("OK");
	}
}
