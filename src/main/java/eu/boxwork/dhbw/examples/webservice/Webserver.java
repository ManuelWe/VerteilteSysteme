package eu.boxwork.dhbw.examples.webservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;

public class Webserver {
	
	// we set the base URL manually
	public static final String BASE = "example";
	public HttpServer server=null;
	
	/**
	 * starts the webserver, IP and port has to be set as parameter
	 * like java -jar webserverexample.jar localhost 8080
	 * @param args: 0 = IP, 1 = port
	 * */
	public static void main( String[] args ) throws IOException, InterruptedException 
	{
		String ip = "127.0.0.1";
		String port = "8080";
		
		Webserver server = new Webserver();
		server.startServer(ip, port);
		System.out.print("Enter key to close server...");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

	     try {
				br.readLine();
			} catch (IOException e) {
				System.err.println(e.getLocalizedMessage());
			}
	     server.stopServer();
	}
	
	public void startServer(String ip, String port)
	{
		String serverString = "http://"+ip+":"+port+"/";
		String baseURLRoot = serverString+BASE;
		try {
			server = HttpServerFactory.create( serverString );
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		}
		server.start();		
		System.out.println("started server at IP '"+ip+"' and port '"+port+"'. Request base URL: "+baseURLRoot);
	}
	
	public void stopServer()
	{
		System.out.print("closing server...");
		server.stop( 0 );
		System.out.print("done.");
		System.exit(0);
	}
}
