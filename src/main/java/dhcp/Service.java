package dhcp;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import eu.boxwork.dhbw.examples.webservice.HelloToSend;

@Path("dhcp") // set the root path of this service
public class Service {

	@GET // Method type used by the client
	@Path("server") // sub path hello => http://<serverip>/rest/hello/name of user
	@Produces(MediaType.TEXT_PLAIN) // we return a html page since a browser calls this IF automatically
	public String helloHTML() {
		HelloToSend hello = new HelloToSend();
		return hello.tail();
	}
	
	@POST // Method type used by the client
	@Path("server") // sub path hello => http://<serverip>/rest/hello/name of user
	@Consumes(MediaType.TEXT_PLAIN) // parameter request type
	@Produces(MediaType.TEXT_PLAIN) // we return a json object
	public HelloToSend helloWorldJSON(@PathParam("name") String name) {
		HelloToSend hello = new HelloToSend();
		hello.setName(name);
		return hello;
	}
	
}
