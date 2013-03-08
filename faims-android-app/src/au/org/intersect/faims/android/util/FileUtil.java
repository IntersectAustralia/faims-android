package au.org.intersect.faims.android.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.util.XFormUtils;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class FileUtil {

	public static void makeDirs(String dir) {
		File file = new File(dir);
		if (!file.exists())
			file.mkdirs();
	}
	
	public static void tarFile(String dir, String filename) throws IOException, FileNotFoundException {
		tarFile(dir, new File(dir).getName(), filename, null);
	}
	
	public static void tarFile(String dir, String baseDir, String filename, List<String> excludeFiles) throws IOException, FileNotFoundException {
		Log.d("FAIMS", "tar file " + dir + " to " + filename);
		
		TarArchiveOutputStream ts = null;
		try {
		 ts = new TarArchiveOutputStream(
				 new GZIPOutputStream(
						 new FileOutputStream(filename)));
		 
		 tarDirToStream(dir, baseDir, ts, excludeFiles);
		}
		finally {
			if (ts != null) ts.close();
		}
	}
	
	private static void tarDirToStream(String dir, String tarname, TarArchiveOutputStream ts, List<String> excludeFiles) throws IOException, FileNotFoundException {
		Log.d("FAIMS", "add dir " + dir + "  to tarname " + tarname);
		
		File d = new File(dir);
		if (d.isDirectory()) {
			String[] fileList = d.list();
			
			for (int i = 0; i < fileList.length; i++) {
				File f = new File(d, fileList[i]);
				if (f.isDirectory()) {
					tarDirToStream(f.getPath(), tarname + "/" + f.getName(), ts, excludeFiles);
				} else {
					
					String tarFile = tarname + "/" + f.getName();
					
					boolean fileExcluded = false;
					if (excludeFiles != null) {
						
						for (String exf : excludeFiles) {
							if (exf.equals(tarFile)) {
								fileExcluded = true;
								break;
							}
						}
					}
					
					if (!fileExcluded) {
						tarFileToStream(f.getPath(), tarFile, ts);
					}
				}
			}
		} else {
			tarFileToStream(d.getPath(), tarname, ts);
		}
	}
	
	private static void tarFileToStream(String filename, String tarname, TarArchiveOutputStream ts) throws IOException {
		Log.d("FAIMS", "add file " + filename + "  to tarname " + tarname);
		
		FileInputStream fs = null;
		try {
			File f = new File(filename);
			fs = new FileInputStream(f);
			TarArchiveEntry te = new TarArchiveEntry(f);
			te.setName(tarname);
			te.setSize(f.length());
			ts.putArchiveEntry(te);
			IOUtils.copy(fs, ts);
			ts.closeArchiveEntry();
		}
		finally {
			if (fs != null) fs.close();
		}
	}
	
	public static void untarFile(String dir, String filename) throws IOException {
		Log.d("FAIMS", "untar file " + filename + " to " + dir);
		
		TarArchiveInputStream ts = null;
		try {
		 ts = new TarArchiveInputStream(
				 new GZIPInputStream(
						 new FileInputStream(filename)));
		 
	     TarArchiveEntry e;
	     while((e = ts.getNextTarEntry()) != null) {
	    	 if (e.isDirectory()) {
	    		 makeDirs(dir + "/" + e.getName());
	    	 } else {
	    		 writeTarFile(ts, e, new File(dir + "/" + e.getName()));
	    	 }
	     }
		} finally {
			if (ts != null) ts.close();
		}
	}
	
	// from: http://stackoverflow.com/questions/3163045/android-how-to-check-availability-of-space-on-external-storage
	public static long getExternalStorageSpace() throws Exception {
	    long availableSpace = -1L;
	    
	    StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
	    stat.restat(Environment.getExternalStorageDirectory().getPath());
	    availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();

	    return availableSpace;
	}
	
	public static void saveFile(InputStream input, String filename) throws IOException {
		Log.d("FAIMS", "saving to file " + filename);
		
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(filename);
			IOUtils.copy(input, os);
		} finally {
			if (os != null) os.close();
		}
	}
	
	public static String generateMD5Hash(String filename) throws IOException, NoSuchAlgorithmException {
		FileInputStream fs = null;
		try {
			fs = new FileInputStream(filename);
			
			MessageDigest digester = MessageDigest.getInstance("MD5");
			byte[] bytes = new byte[8192];
			int byteCount;
			while ((byteCount = fs.read(bytes)) > 0) {
				digester.update(bytes, 0, byteCount);
			}
			return new String(Hex.encodeHex(digester.digest()));
		} finally {
			if (fs != null) fs.close();
		}
	}
	
	private static void writeTarFile(TarArchiveInputStream ts, TarArchiveEntry entry, File file) throws IOException {
		Log.d("FAIMS", "Writing tar file " + file.getName());
		FileOutputStream os = null;
		
		try {
			os = new FileOutputStream(file);
		        
			byte[] buffer = new byte[(int)entry.getSize()];
	        int bufferLength = 0; //used to store a temporary size of the buffer
	        
	        while ( (bufferLength = ts.read(buffer)) > 0 ) {
	            os.write(buffer, 0, bufferLength);
	        }
		} finally {
			if (os != null) os.close();
		}
	}
	
	public static FormEntryController readXmlContent(String path) {
		FormDef fd = null;
		FileInputStream fis = null;
		String mErrorMsg = null;

		File formXml = new File(path);

		try {
			fis = new FileInputStream(formXml);
			fd = XFormUtils.getFormFromInputStream(fis);
			if (fd == null) {
				mErrorMsg = "Error reading XForm file";
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			mErrorMsg = e.getMessage();
		} catch (XFormParseException e) {
			mErrorMsg = e.getMessage();
			e.printStackTrace();
		} catch (Exception e) {
			mErrorMsg = e.getMessage();
			e.printStackTrace();
		}

		if (mErrorMsg != null) {
			return null;
		}

		// new evaluation context for function handlers
		fd.setEvaluationContext(new EvaluationContext(null));

		// create FormEntryController from formdef
		FormEntryModel fem = new FormEntryModel(fd);
		return new FormEntryController(fem);
	}
	
	public static String readFileIntoString(String path) {
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(new File(path));
		    FileChannel fc = stream.getChannel();
		    MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		    /* Instead of using default, pass in a decoder. */
		    return Charset.defaultCharset().decode(bb).toString();
		}
		catch(IOException ioe){
			return "";
		}
		finally {
			try{
				stream.close();
			}
			catch(Exception e){
				// continue
			}
		}
	}
	
	public static String convertStreamToString(InputStream stream) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(stream));
		
			StringBuilder sb = new StringBuilder();
		
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			} 
	
			return sb.toString();
		} catch (IOException e) {
			FAIMSLog.log(e);
		} finally {
			try {
				if (br != null) br.close();
			} catch (IOException e) {
				FAIMSLog.log(e);
			}
		}
		return null;
	}

	public static void deleteDirectory(File dir) {
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				deleteDirectory(f);
			} else {
				f.delete();
			}
		}
		dir.delete();
	}

	public static void copyFile(String fromPath, String toPath) throws IOException {
		Log.d("FAIMS", "copying file " + fromPath + " to " + toPath);
		
		FileInputStream input = null;
		FileOutputStream output = null;
		try {
			input = new FileInputStream(fromPath);
			output = new FileOutputStream(toPath);
			IOUtils.copy(input, output);
		} finally {
			if (input != null) input.close();
			if (output != null) output.close();
		}
	}

	public static List<String> listDir(String uploadPath) {
		ArrayList<String> fileList = new ArrayList<String>();
		getDirectoryList(new File(uploadPath), fileList, "");
		return fileList;
	}
	
	private static void getDirectoryList(File dir, List<String> fileList, String base) {
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				getDirectoryList(f, fileList, base + f.getName() + "/");
			} else {
				fileList.add(base + f.getName());
			}
		}
	}

	
}


