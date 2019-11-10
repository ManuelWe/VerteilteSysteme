package dhcp;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("") // set the root path of this service
public class Service {

	@GET // Method type used by the client
	@Path("server") // sub path hello => http://<serverip>/rest/hello/name of user
	@Produces(MediaType.TEXT_PLAIN) // we return a html page since a browser calls this IF automatically
	public String helloHTML() {
		HelloToSend hello = new HelloToSend();
		return hello.tail();
	}
}
