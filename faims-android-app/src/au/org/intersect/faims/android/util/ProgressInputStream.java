package au.org.intersect.faims.android.util;

import java.io.IOException;
import java.io.InputStream;

// TODO use this class to make download progress dialogs
public class ProgressInputStream extends InputStream {
	
	public interface ProgressListener {
		
		public void onProgress(int percentage);
	}
	
	private InputStream input;
	private ProgressListener listener;
	private int totalBytes;
	private int bytesRead;
	
	public ProgressInputStream(InputStream stream, ProgressListener listener, int totalBytes) {
		this.input = stream;
		this.listener = listener;
		this.totalBytes = totalBytes;
		this.bytesRead = 0;
	}
	
	@Override
	public int read() throws IOException {
		bytesRead++;
		listener.onProgress((int) bytesRead / totalBytes);
		return input.read();
	}
	
	@Override
	public int read(byte[] buffer) throws IOException {
		int size = super.read(buffer);
		bytesRead += size;
		listener.onProgress((int) bytesRead / totalBytes);
		return size;
	}
	
	@Override
	public int read(byte[] buffer, int off, int len) throws IOException {
		int size = super.read(buffer, off, len);
		bytesRead += size;
		listener.onProgress((int) bytesRead / totalBytes);
		return size;
	}

}
