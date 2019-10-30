package eu.boxwork.dhbw.examples.http;

import java.io.PrintWriter;
import java.net.Socket;

import eu.boxwork.dhbw.utils.LogManager;
import eu.boxwork.dhbw.utils.Logger;

/**
 * Diese Klasse schickt HTML - Request
 * @author Patrick Jungk
 * @version 1.0
 */
public class HTTPClient implements Runnable {
	protected static Logger Log = LogManager.getLogger(HTTPClient.class.getName());
	private Socket clientSocket = null;
	private static String HTMLREQUEST = "GET /time HTTP/1.1\n"+
		"Host: unknown\n"+
		"User-Agent: unknown\n"+
		"Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\n"+
		"Accept-Language: en-US;q=0.7,en;q=0.3\n"+
		"Accept-Encoding: gzip, deflate\n"+
		"Connection: keep-alive\n"+
		"Upgrade-Insecure-Requests: 1\n";
	
	public HTTPClient(Socket clientSocket) {
		super();
		this.clientSocket = clientSocket;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if (this.clientSocket!=null)
		{
			try {
				htmlRequest(this.clientSocket);
			} catch (Exception e) {
				Log.error(e.getLocalizedMessage());
			}
		}
		else
		{
			Log.error("no client socket. terminating");
		}
	}

	/**
	 * Bearbeitet den Request am Socket
	 * @param clientSocket {@link Socket} des aktuellen Clients
	 * */
	private void htmlRequest(Socket clientSocket) throws Exception
	{
		Log.info("connecting to: "+clientSocket);
	
		PrintWriter out =
		        new PrintWriter(clientSocket.getOutputStream(), true);
				
		out.println(HTMLREQUEST);
		
		out.close();
		
		Log.info("closing connection: "+clientSocket);
		clientSocket.close();		
	}
}
