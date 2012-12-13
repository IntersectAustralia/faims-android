package au.org.intersect.faims.android.util;

import android.util.Log;

public class FAIMSLog {
	
	public static void log() {
		Log.d("debug", getCaller());
	}

	public static void log(String message) {
		Log.d("debug", getCaller() + ": " + message);
	}
	
	public static void log(Exception e) {
		Log.d("debug", getCaller() + ": " + e.toString());
	}
	
	public static String getCaller()
	{
		int depth = 1;
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		String className = ste[ste.length - 1 - depth].getClassName(); 
		String methodName = ste[ste.length - 1 - depth].getMethodName(); 
		return className  + "." + methodName;
	}
	
}
