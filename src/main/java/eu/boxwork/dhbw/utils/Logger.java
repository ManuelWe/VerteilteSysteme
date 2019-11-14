/**
 * 
 */
package eu.boxwork.dhbw.utils;

import java.util.Calendar;

/**
 * Einfache Log-Klasse
 * @author Patrick Jungk
 * @version 1.0
 */
public class Logger {
	
	private String name = "";
	private LogLevel level = LogLevel.ALL;
	
	public Logger(String name)
	{
//		this.name = name;
	}
	/**
	 * Gibt einen Text aus der Standardkonsole aus
	 * @param text Text, der als Fehler ausgegeben werden soll
	 * */
	public synchronized void error(String text)
	{
		if (level.getVal()<=LogLevel.ERROR.getVal())
			System.out.println(name+"|"+Calendar.getInstance().getTime().toGMTString()+" | ERROR: "+text);
	}
	
	/**
	 * Gibt einen Text aus der Standardkonsole aus
	 * @param text Text, der als Warnung ausgegeben werden soll
	 * */
	public synchronized void warn(String text)
	{
		if (level.getVal()<=LogLevel.WARN.getVal())
			System.out.println(name+"|"+Calendar.getInstance().getTime().toGMTString()+" | WARN: "+text);
	}
	
	/**
	 * Gibt einen Text aus der Standardkonsole aus
	 * @param text Text, der als Info ausgegeben werden soll
	 * */
	public synchronized void info(String text)
	{
		if (level.getVal()<=LogLevel.INFO.getVal())
			System.out.println(name+"|"+Calendar.getInstance().getTime().toGMTString()+" | INFO:  "+text);
	}
	
	/**
	 * Gibt einen Text aus der Standardkonsole aus
	 * @param text Text, der als Debug ausgegeben werden soll
	 * */
	public void debug(String string) {
		if (level.getVal()<=LogLevel.DEBUG.getVal())
			System.out.println(name+"|"+Calendar.getInstance().getTime().toGMTString()+" | DEBUG:  "+string);
	}
	/**
	 * @return the level
	 */
	public LogLevel getLevel() {
		return level;
	}
	/**
	 * @param level the level to set
	 */
	public void setLevel(LogLevel level) {
		this.level = level;
	}
}
