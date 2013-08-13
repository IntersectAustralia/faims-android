package au.org.intersect.faims.android.log;

import android.util.Log;

public class FLog {
	// note: could not extend final class Log
	
	private static final String TAG = "FAIMS";
	
	private static final int DEBUG = 1;
	private static final int INFO = 2;
	private static final int WARNING = 4;
	private static final int ERROR = 8;
	private static final int VERBOSE = 16;
	//private static final int ALL = 31;
	
	//private static int enableLevel = ALL; // use this for debug builds
	private static int enableLevel = WARNING | ERROR | VERBOSE; // use this for release builds
	
	private static boolean hasLevel(int type) {
		return (enableLevel & type) == type;
	}
	
	public static int c() {
		if (hasLevel(DEBUG)) return Log.d(TAG, formatMsg());
		return -1;
	}
	
	public static int d(String msg) {
		if (hasLevel(DEBUG)) return Log.d(TAG, formatMsg(msg));
		return -1;
	}
	
	public static int d(String msg, Throwable tr) {
		if (hasLevel(DEBUG)) Log.d(TAG, formatMsg(msg), tr);
		return -1;
	}
	
	public static int e(String msg) {
		if (hasLevel(ERROR)) return Log.e(TAG, formatMsg(msg));
		return -1;
	}
	
	public static int e(String msg, Throwable tr) {
		if (hasLevel(ERROR)) return Log.e(TAG, formatMsg(msg), tr);
		return -1;
	}
	
	public static int i(String msg) {
		if (hasLevel(INFO)) return Log.i(TAG, formatMsg(msg));
		return -1;
	}
	
	public static int i(String msg, Throwable tr) {
		if (hasLevel(INFO)) return Log.i(TAG, formatMsg(msg), tr);
		return -1;
	}
	
	public static int v(String msg) {
		if (hasLevel(VERBOSE)) return Log.v(TAG, formatMsg(msg));
		return -1;
	}
	
	public static int v(String msg, Throwable tr) {
		if (hasLevel(VERBOSE)) return Log.v(TAG, formatMsg(msg), tr);
		return -1;
	}
	
	public static int w(String msg) {
		if (hasLevel(WARNING)) return Log.w(TAG, formatMsg(msg));
		return -1;
	}
	
	public static int w(String msg, Throwable tr) {
		if (hasLevel(WARNING)) return Log.w(TAG, formatMsg(msg), tr);
		return -1;
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
