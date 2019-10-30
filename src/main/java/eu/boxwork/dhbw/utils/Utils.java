/**
 * 
 */
package eu.boxwork.dhbw.utils;

import java.util.Calendar;
import java.util.HashMap;

/**
 * Utils-Klasse mit diversen Hilfsmethoden
 * @author Patrick Jungk
 * @version 1.0
 * @category helper
 */
public class Utils {
	
	/*
	 * Methoden für Zeitmessung
	 * */
	private static HashMap<Object, Long> times = new HashMap<>();
	
	/**
	 * Statische Funktion für das Starten eines Timers
	 * @param callingObject Objekt, dass einen Timer starten will
	 * @return void
	 * */
	public static void startTimer(Object callingObject)
	{
		synchronized (times) {
			times.put(callingObject, new Long(Calendar.getInstance().getTimeInMillis()));			
		}
		return;
	}
	
	/**
	 * Statische Funktion für das Stoppen eines Timers; gibt die Zeit zurück, die der Timer gelaufen ist.
	 * @param callingObject Objekt, dass einen Timer starten will
	 * @return long, Zeit, die der Timer gelaufen ist (in ms); -1, wenn ein Fehler aufgetreten ist
	 * */
	public synchronized static long stopTimer(Object callingObject)
	{
		long diff = -1;
		Long startTime = null;
		long now = Calendar.getInstance().getTimeInMillis();
		
		synchronized (times) {
			startTime = times.remove(callingObject);
		}
		
		if (startTime!=null)
		{
			diff=now-startTime.longValue();
		}
		return diff;
	}

	
	/**
	 * Testmain zur Prüfung der bereitgestellten Methoden
	 * */
	public static void main(String[] args) {
	}

	
}
