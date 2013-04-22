package au.org.intersect.faims.android.log;

import android.util.Log;

public class FLog {
	// note: could not extend final class Log
	
	private static final String TAG = "FAIMS";
	
	public static int c() {
		return Log.d(TAG, formatMsg());
	}
	
	public static int d(String msg) {
		return Log.d(TAG, formatMsg(msg));
	}
	
	public static int d(String msg, Throwable tr) {
		return Log.d(TAG, formatMsg(msg), tr);
	}
	
	public static int e(String msg) {
		return Log.e(TAG, formatMsg(msg));
	}
	
	public static int e(String msg, Throwable tr) {
		return Log.e(TAG, formatMsg(msg), tr);
	}
	
	public static int i(String msg) {
		return Log.i(TAG, formatMsg(msg));
	}
	
	public static int i(String msg, Throwable tr) {
		return Log.i(TAG, formatMsg(msg), tr);
	}
	
	public static int v(String msg) {
		return Log.v(TAG, formatMsg(msg));
	}
	
	public static int v(String msg, Throwable tr) {
		return Log.v(TAG, formatMsg(msg), tr);
	}
	
	public static int w(String msg) {
		return Log.w(TAG, formatMsg(msg));
	}
	
	public static int w(String msg, Throwable tr) {
		return Log.w(TAG, formatMsg(msg), tr);
	}
	
	private static String formatMsg() {
		return getClassMethod();
	}
	
	private static String formatMsg(String msg) {
		return getClassMethod() + ": " + msg;
	}
	
	private static String getClassMethod()
	{
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		int depth = ste.length - 6; // hardcoded depth
		String className = ste[ste.length - 1 - depth].getClassName(); 
		String methodName = ste[ste.length - 1 - depth].getMethodName(); 
		return className.substring(className.lastIndexOf(".") + 1)  + "." + methodName;
	}
	
}
