package eu.boxwork.dhbw.examples.http;

import java.io.IOException;
import java.net.Socket;

import eu.boxwork.dhbw.utils.LogManager;
import eu.boxwork.dhbw.utils.Logger;

/**
 * Kleiner Webserver für die Rückgabe von Vorlesungsinhalten
 * @author Patrick Jungk
 * @version 1.0
 * */
public class DOSClient implements Runnable{
	protected Logger Log = LogManager.getLogger(DOSClient.class.getName());
	/*
	 * private Objekte
	 * */
	private static String SERVERADRESS = "";
	private Thread runner = null;
	private boolean active=true;
	private Socket clientSocket = null;
	private MODE modeToRune = MODE.SINGLE_THREADED;

	/*
	 * oeffentliche Einstellungen
	 * */
	public enum MODE {SINGLE_THREADED, MULTI_THREADED};
	 
	/**
	 * initialisiert den Webserver und startet diesen.
	 * */
	public void initialise()
	{
		if (runner==null)
		{
			runner=new Thread(this);
		}

		try {
			Log.info("starting DOS on server: "+SERVERADRESS);		    
		    runner.start();
		}	catch (Exception e) {
			Log.error(e.getLocalizedMessage());
		}
	}
	
	/**
	 * schließt den Server wieder
	 * */
	private void close()
	{
		try {
			clientSocket.close();
		} catch (IOException e) {
			Log.error(e.getLocalizedMessage());
		}
	}
	
	/**
	 * Startet den Server und das "Lauschen" auf einen Port.
	 * */
	@Override
	public void run() {		
		while (active)
		{
			try {
				String[]serverPort = SERVERADRESS.split(":");
				clientSocket = new Socket(serverPort[0], Integer.parseInt(serverPort[1]));
				
				if (modeToRune==MODE.SINGLE_THREADED)
				{
					HTTPClient worker = new HTTPClient(clientSocket);
					worker.run();
				}	
				else if (modeToRune==MODE.MULTI_THREADED)
				{
					Thread worker = new Thread( new HTTPClient(clientSocket));
					worker.start();
				}	
				else
				{
					Log.error("Client-Mode not impleemented.");
				}
				
			} catch (Exception e) {	
				Log.error(e.getLocalizedMessage());
			}
		}
		close();
		
	}
		
	/**
	 * @param modeToRune der Modus, wie der Server laufen soll (Single Threaded, Multithreaded TODO)
	 */
	public void setModeToRune(MODE modeToRune) {
		this.modeToRune = modeToRune;
	}

	/**
	 * Testmain zur Prüfung der bereitgestellten Methoden
	 * ein Parameter:
	 * single | multi
	 * für die Anzahl der Threads
	 * */
	public static void main(String[] args) {
		
		DOSClient ws = new DOSClient();
		if (args.length>0)
		{
			SERVERADRESS=args[0];
		}
		if (args.length>1)
		{
			switch (args[1].toLowerCase()) {
			case "multi":
				ws.setModeToRune(MODE.MULTI_THREADED);
				break;
			case "single":
				ws.setModeToRune(MODE.SINGLE_THREADED);
				break;
			default:
				ws.setModeToRune(MODE.SINGLE_THREADED);
				break;
			}
		}
		else
		{
			ws.setModeToRune(MODE.SINGLE_THREADED);
		}
		
		ws.initialise();
	}
}
