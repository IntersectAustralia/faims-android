package au.org.intersect.faims.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class FileUtil {

	public static void makeDirs(String path) {
		String dir = Environment.getExternalStorageDirectory() + path;
		File file = new File(dir);
		Log.d("debug", dir + " exists " + String.valueOf(file.exists()));
		if (!file.exists())
			file.mkdirs();
	}
	
	public static void untarFromStream(String dir, InputStream stream) throws IOException {
		 TarArchiveInputStream ts = new TarArchiveInputStream(new GZIPInputStream(stream));
	     TarArchiveEntry e;
	     while((e = ts.getNextTarEntry()) != null) {
	    	 if (e.isDirectory()) {
	    		 makeDirs(dir + "/" + e.getName());
	    	 } else {
	    		 writeFile(ts, e, new File(Environment.getExternalStorageDirectory() + dir + "/" + e.getName()));
	    	 }
	     }
	}
	
	public static long getExternalStorageSpace() throws Exception {
	    long availableSpace = -1L;
	    try {
	        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
	        stat.restat(Environment.getExternalStorageDirectory().getPath());
	        availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
	    } catch (Exception e) {
	       Log.d("debug", e.toString());
	       throw e;
	    }

	    return availableSpace;
	}
	
	private static void writeFile(TarArchiveInputStream input, TarArchiveEntry entry, File file) throws IOException {
		Log.d("debug", "Writing file: " + file.getName());
		
		FileOutputStream fileOutput = new FileOutputStream(file);
	        
		byte[] buffer = new byte[(int)entry.getSize()];
        int bufferLength = 0; //used to store a temporary size of the buffer
        
        while ( (bufferLength = input.read(buffer)) > 0 ) {
            fileOutput.write(buffer, 0, bufferLength);
        }
        fileOutput.close();
        Log.d("debug", "Finished Writing file: " + file.getName());
	}
	
}
