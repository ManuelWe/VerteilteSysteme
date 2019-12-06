package dhcp;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("dhcp")
public class Service {
	FileHandler fileHandler;

	@GET
	@Path("server")
	@Produces(MediaType.TEXT_PLAIN)
	public String helloHTML() {
		fileHandler = new FileHandler();
		return fileHandler.getLastFileEntry();
	}

	@POST
	@Path("server")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public String helloWorldJSON(String serverAddress) {
		fileHandler = new FileHandler();
		fileHandler.setServerAddress(serverAddress);
		return ("OK");
	}
}
