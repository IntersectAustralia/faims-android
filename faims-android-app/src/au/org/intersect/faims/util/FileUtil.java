package au.org.intersect.faims.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class FileUtil {
	
	private static funciton toPath(String path) {
		
	}

	public static void makeDirs(String path) {
		String dir = Environment.getExternalStorageDirectory() + path;
		File file = new File(dir);
		Log.d("debug", dir + " exists " + String.valueOf(file.exists()));
		if (!file.exists())
			file.mkdirs();
	}
	
	public static void untarFromStream(String dir, String filename) throws IOException {
		TarArchiveInputStream ts = null;
		try {
		 ts = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(Environment.getExternalStorageDirectory() + filename)));
	     TarArchiveEntry e;
	     while((e = ts.getNextTarEntry()) != null) {
	    	 if (e.isDirectory()) {
	    		 makeDirs(dir + "/" + e.getName());
	    	 } else {
	    		 writeTarFile(ts, e, new File(Environment.getExternalStorageDirectory() + dir + "/" + e.getName()));
	    	 }
	     }
		} finally {
			if (ts != null) ts.close();
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
	
	public static void saveFile(InputStream input, String filename) throws IOException {
		FileOutputStream fileOutput = null;
		try {
			fileOutput = new FileOutputStream(Environment.getExternalStorageDirectory() + filename);
		        
			byte[] buffer = new byte[1024];
	        int bufferLength = 0; //used to store a temporary size of the buffer
	        
	        while ( (bufferLength = input.read(buffer)) > 0 ) {
	            fileOutput.write(buffer, 0, bufferLength);
	        }
		} finally {
			if (fileOutput != null) fileOutput.close();
		}
        
        Log.d("debug", "Finished Writing file: " + filename);
	}
	
	public static String generateMD5Hash(String filename) throws IOException, NoSuchAlgorithmException {
		Log.d("debug", "Generating MD5 Hash for file: " + new File(Environment.getExternalStorageDirectory() + filename).length());
		
		FileInputStream input = null;
		try {
			input = new FileInputStream(Environment.getExternalStorageDirectory() + filename);
			
			MessageDigest digester = MessageDigest.getInstance("MD5");
			byte[] bytes = new byte[8192];
			int byteCount;
			while ((byteCount = input.read(bytes)) > 0) {
				digester.update(bytes, 0, byteCount);
			}
			
			return new BigInteger(1, digester.digest()).toString(16);
		} finally {
			if (input != null) input.close();
		}
	}
	
	public static void deleteFile(String filename) throws IOException {
		new File(Environment.getExternalStorageDirectory() + filename).delete();
	}
	
	private static void writeTarFile(TarArchiveInputStream input, TarArchiveEntry entry, File file) throws IOException {
		FileOutputStream fileOutput = null;
		try {
			fileOutput = new FileOutputStream(file);
		        
			byte[] buffer = new byte[(int)entry.getSize()];
	        int bufferLength = 0; //used to store a temporary size of the buffer
	        
	        while ( (bufferLength = input.read(buffer)) > 0 ) {
	            fileOutput.write(buffer, 0, bufferLength);
	        }
		} finally {
			if (fileOutput != null) fileOutput.close();
		}
        Log.d("debug", "Finished Writing Tar file: " + file.getName());
	}
	
}
