package dhcp;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.spi.resource.Singleton;

@Path("dhcp")
@Singleton
public class Service {
	String serverAddress = "0";

	@GET
	@Path("server")
	@Produces(MediaType.TEXT_PLAIN)
	public String helloHTML() {
		return serverAddress;
	}

	@POST
	@Path("server")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public String helloWorldJSON(String serverAddress) {
		this.serverAddress = serverAddress;
		return ("OK");
	}
}
