/**
 * 
 */
package eu.boxwork.dhbw.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Einfache Klasse zum Erzeugen von Loggern
 * @author Patrick Jungk
 * @version 1.0
 */
public class LogManager {
	
	// erzeuge Logger
	private static Map<String, Logger> loggerMap = new HashMap<String, Logger>();
	private static LogLevel defaultLoglevel = LogLevel.INFO;
	/**
	 * Erzeugt einen einfachen Logger
	 * @param name Bezeichnung der Loggers
	 * @return {@link Logger}
	 * */
	public static synchronized Logger getLogger(String name)
	{
		if (loggerMap.containsKey(name))
		{
			// nothing to do.
		}
		else
		{
			Logger l = new Logger(name);
			l.setLevel(defaultLoglevel);
			loggerMap.put(name, l);
		}
		return loggerMap.get(name);
	}
}
