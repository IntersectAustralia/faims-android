package au.org.intersect.faims.android.util;

import android.util.Log;


// custom logging class to automatically find the calling class and method
public class FAIMSLog {
	
	public static void log() {
		Log.d("FAIMS", getCaller());
	}

	public static void log(String message) {
		Log.d("FAIMS", getCaller() + ": " + message);
	}
	
	public static void log(Exception e) {
		Log.d("FAIMS", getCaller() + ": " + e.toString());
	}
	
	public static String getCaller()
	{
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		int depth = ste.length - 5;
		String className = ste[ste.length - 1 - depth].getClassName(); 
		String methodName = ste[ste.length - 1 - depth].getMethodName(); 
		return "("+Thread.currentThread().getId()+")"+className.substring(className.lastIndexOf(".") + 1)  + "." + methodName;
	}
	
}
