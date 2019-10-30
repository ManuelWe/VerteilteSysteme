package eu.boxwork.dhbw.examples.webservice;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path( Webserver.BASE ) // set the root path of this service
public class Service {

	/**
	 * use this private methode to create the usage-feedback
	 * @param html if <code>true</code>, then this methode returns a HTML page, else text only
	 * @return html or plain string
	 * */
	public String getUsage(boolean html)
	{
		String usage =  "call 'hello' to feedback hallo or 'hallo/{username}' to say hallo to a username.";
		if (!html)
		{
			return usage;
		}
		else
		{
			String ret ="<HTML><HEADER><TITLE>EXAMPLE WEBSERIVE USAGE</TITLE></HEADER>";
			ret = ret + "<BODY><p>"+usage+"</p></BODY></HTML>";
			return ret; 
		}
	}
	
	@GET // Method to return usage
	@Path( "" ) // sub path hello => http://<serverip>/rest/hello
	@Produces(MediaType.TEXT_PLAIN) // we return a text type
	public String usage()
	{
		return getUsage(false);
	}
	
	@GET // Method to return usage
	@Path( "" ) // sub path hello => http://<serverip>/rest/hello
	@Produces(MediaType.TEXT_HTML) // we return a text type
	public String usageHTML()
	{
		return getUsage(true);
	}
	
/*	@GET // Method type used by the client
	@Path( "hello" ) // sub path hello => http://<serverip>/rest/hello
	@Produces(MediaType.TEXT_PLAIN) // we return a text type
	public String hello()
	{
		return "hello!!!";
	}*/
	  
	@GET // Method type used by the client
	@Path( "hello/{name}" ) // sub path hello => http://<serverip>/rest/hello/name of user
	@Consumes(MediaType.TEXT_PLAIN) // parameter request type
	@Produces(MediaType.APPLICATION_JSON) // we return a json object
	public HelloToSend helloWorldJSON(@PathParam("name") String name)
	{
		HelloToSend hello = new HelloToSend();
		hello.setName(name);
		return hello;
	}
	
	@GET // Method type used by the client
	@Path( "hello/{name}" ) // sub path hello => http://<serverip>/rest/hello/name of user
	@Consumes(MediaType.TEXT_PLAIN) // parameter request type
	@Produces(MediaType.TEXT_PLAIN) // we return a text string
	public HelloToSend helloWorldText(@PathParam("name") String name)
	{
		HelloToSend hello = new HelloToSend();
		hello.setName(name);
		return hello;
	}
	
	@GET // Method type used by the client
	@Path( "hello/{name}" ) // sub path hello => http://<serverip>/rest/hello/name of user
	@Consumes(MediaType.TEXT_PLAIN) // parameter request type
	@Produces(MediaType.TEXT_HTML) // we return a html page since a browser calls this IF automatically
	public String helloWorldHTML(@PathParam("name") String name)
	{
		HelloToSend hello = new HelloToSend();
		hello.setName(name);
		return hello.toHTML();
	}

	@GET // Method type used by the client
	@Path( "hello" ) // sub path hello => http://<serverip>/rest/hello/name of user
	@Consumes(MediaType.TEXT_PLAIN) // parameter request type
	@Produces(MediaType.TEXT_HTML) // we return a html page since a browser calls this IF automatically
	public String hello()
	{
		HelloToSend hello = new HelloToSend();
		return hello.toHTML();
	}
}
