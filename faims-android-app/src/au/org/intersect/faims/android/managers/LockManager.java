package au.org.intersect.faims.android.managers;

import java.io.File;

import au.org.intersect.faims.android.util.FileUtil;

public class LockManager {

	public static void waitForLock(String filename) throws Exception {
		while(new File(filename).exists()) {
			Thread.sleep(1000);
		}
		FileUtil.touch(new File(filename));
	}
	
	public static void clearLock(String filename) {
		File f = new File(filename);
		if (f.exists()) FileUtil.delete(f);
	}
	
	public static boolean isLocked(String filename) {
		return new File(filename).exists();
	}
}
