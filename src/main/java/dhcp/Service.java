package dhcp;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("dhcp") // set the root path of this service
public class Service {

	@GET
	@Path("server")
	@Produces(MediaType.TEXT_PLAIN)
	public String helloHTML() {
		FileHandler fileHandler = new FileHandler();
		return fileHandler.getLastFileEntry();
	}

	@POST
	@Path("server")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public String helloWorldJSON(String serverAddress) {
		FileHandler fileHandler = new FileHandler();
		fileHandler.setServerAddress(serverAddress);
		return ("OK");
	}

}
