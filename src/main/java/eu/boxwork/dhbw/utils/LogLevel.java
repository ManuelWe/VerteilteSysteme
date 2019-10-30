package eu.boxwork.dhbw.utils;

public enum LogLevel {
	TRACE(1), DEBUG(2),INFO(3), WARN(4), ERROR(5), ALL(6);
	
	int val = -1;
	
	LogLevel(int val)
	{
		this.val = val;
	}
	
	public int getVal()
	{
		return val;
	}
}
