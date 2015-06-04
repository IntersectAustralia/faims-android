package au.org.intersect.faims.android.util;

import android.app.Activity;
import au.org.intersect.faims.android.log.FLog;

public class MemoryUtil {
	
	private static final double MAX_MEMORY_PERCENTAGE = 0.9;

	public static boolean isMemoryLow(Activity activity) {
		final Runtime runtime = Runtime.getRuntime();
		final double usedMemory = (double) runtime.totalMemory() - runtime.freeMemory();
		final double maxHeapMemory = (double) runtime.maxMemory();
		FLog.d("usedMemory: " + usedMemory);
		FLog.d("maxHeapMemory: " + maxHeapMemory);
		return usedMemory / maxHeapMemory > MAX_MEMORY_PERCENTAGE;
	}

}
