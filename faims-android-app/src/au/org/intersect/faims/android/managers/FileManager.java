package au.org.intersect.faims.android.managers;

import java.io.File;

import android.util.SparseArray;

import com.google.inject.Singleton;

@Singleton
public class FileManager {
	
	public interface FileManagerListener {
		
		public void onFileSelected(File file);
	}
	
	private SparseArray<FileManagerListener> listeners;

	public void init() {
		listeners = new SparseArray<FileManagerListener>();
	}
	
	public void destroy() {
		listeners = null;
	}
	
	public void addListener(int code, FileManagerListener listener) {
		listeners.put(code, listener);
	}
	
	public void selectFile(int code, File file) {
		listeners.get(code).onFileSelected(file);
	}
}

