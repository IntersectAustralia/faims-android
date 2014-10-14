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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.compress.utils.IOUtils;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.util.XFormUtils;

import android.os.Environment;
import android.os.StatFs;
import au.org.intersect.faims.android.log.FLog;

public class FileUtil {

	public static final String ORIGINAL_EXT = ".original";
	public static final String THUMBNAIL_EXT = ".thumbnail";

	// File Helpers
	public static void touch(File file) throws Exception {
        if (!file.exists())
            new FileOutputStream(file).close();
        file.setLastModified(System.currentTimeMillis());
	}
	
	public static void makeDirs(File dir) {
		if (!dir.isDirectory())
			dir.mkdirs();
	}
	
	public static void delete(File file) {
		if (file.isDirectory()) {
			deleteDirectory(file);
		} else if (file.isFile()) {
			deleteFile(file);
		}
	}
	
	private static void deleteFile(File file) {
		if (!file.isFile()) {
			FLog.d("Cannot delete file " + file);
			return;
		}
		
		// android hack: rename file to unused location before deleting as file so current location can be reused
		File toFile = new File(file.getAbsolutePath() + System.currentTimeMillis());
		file.renameTo(toFile);
		boolean success = toFile.delete();
		if (!success) {
			FLog.d("failed to delete file " + toFile);
		}
	}

	private static void deleteDirectory(File dir) {
		if (!dir.isDirectory()) {
			FLog.d("Cannot delete directory " + dir);
			return;
		}
		
		for (File f : dir.listFiles()) {
			delete(f);
		}
		
		// android hack: rename file to unused location before deleting as file so current location can be reused
		File toDir = new File(dir.getAbsolutePath() + System.currentTimeMillis());
		dir.renameTo(toDir);
		boolean success = toDir.delete();
		if (!success) {
			FLog.d("failed to delete file " + toDir);
		}
	}
	
	public static void saveFile(InputStream input, String filename) throws IOException {
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(filename);
			IOUtils.copy(input, os);
		} finally {
			if (os != null) os.close();
		}
	}
	
	public static void saveFile(InputStream input, File file) throws IOException {
		FileOutputStream os = null;
		try {
			os = new FileOutputStream(file);
			IOUtils.copy(input, os);
		} finally {
			if (os != null) os.close();
		}
	}
	
	public static void copyFile(File fromPath, File toPath) throws IOException {
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

	public static void moveDir(String fromDir, String toDir) throws Exception {
		toDir = new File(toDir).getAbsolutePath() + "/";
		
		File d = new File(fromDir);
		if (d.isDirectory()) {
			String[] fileList = d.list();
			
			for (int i = 0; i < fileList.length; i++) {
				File f = new File(d, fileList[i]);
				if (f.isDirectory()) {
					moveDir(f.getAbsolutePath(), toDir + f.getName() + "/");
				} else {
					moveFile(f, toDir);
				}
			}
		} else {
			moveFile(d, toDir);
		}
	}
	
	private static void moveFile(File file, String dir) throws Exception {
		File d = new File(dir);
		
		if (!d.isDirectory()) {
			d.mkdirs();
		}
		
		File toFile = new File(dir + file.getName());
		if (!file.renameTo(toFile)) {
			copyFile(file, toFile);
		}
	}
	
	// Useful Helpers
	
	public static long getExternalStorageSpace() throws Exception {
	    long availableSpace = -1L;
	    
	    StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
	    stat.restat(Environment.getExternalStorageDirectory().getPath());
	    availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();

	    return availableSpace;
	}
	
	public static String generateMD5Hash(File file) throws IOException, NoSuchAlgorithmException {
		FileInputStream fs = null;
		try {
			fs = new FileInputStream(file);
			
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
		} catch (Exception e) {
			FLog.e("error converting stream to string", e);
		} finally {
			try {
				if (br != null) br.close();
			} catch (IOException e) {
				FLog.e("error closing reader", e);
			}
		}
		return null;
	}

	public static List<File> listDirectory(File directory) {
		ArrayList<File> fileList = new ArrayList<File>();
		getDirectoryList(directory, fileList);
		return fileList;
	}
	
	private static void getDirectoryList(File dir, List<File> fileList) {
		if (dir == null || dir.listFiles() == null) return;
		
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				getDirectoryList(f, fileList);
			} else {
				fileList.add(f);
			}
		}
	}
	
	public static String addOriginalExtToFile(File file) {
		String filename = file.getName();
		int index = filename.lastIndexOf('.');
		if (index > 0) {
			String fileNoExt = filename.substring(0, index);
			String fileExt = filename.substring(index);
			return fileNoExt + ORIGINAL_EXT + fileExt;
		} else if (index == 0) {
			return ORIGINAL_EXT + filename; 
		} else {
			return filename + ORIGINAL_EXT;
		}
	}
	
	public static File getThumbnailFileFor(File file) {
		String filename = file.getPath().replace(ORIGINAL_EXT, THUMBNAIL_EXT);
		int index = filename.lastIndexOf('.');
		if (index > 0) {
			String fileNoExt = filename.substring(0, index);
			index = fileNoExt.lastIndexOf('.');
			if (index > 0) {
				return new File(fileNoExt + ".jpg");
			} else {
				return new File(filename);
			}
		} 
		return null;
	}

	public static ArrayList<String> sortArch16nFiles(ArrayList<String> toSort) {
		ArrayList<String> toReturn = new ArrayList<String>(toSort); 
		// Remove .properties from all filenames
		ListIterator<String> iterator = toReturn.listIterator();
		while(iterator.hasNext()) {
			String file = iterator.next();
			iterator.set(file.substring(0, file.lastIndexOf(".")));
		}
		Collections.sort(toReturn, new Comparator<String>() {

			@Override
			public int compare(String lhs, String rhs) {
				int leftIndex = lhs.lastIndexOf(".");
				String leftSort = leftIndex == -1 ? "" : lhs.substring(leftIndex);
				int rightIndex = rhs.lastIndexOf(".");
				String rightSort = rightIndex == -1 ? "" : rhs.substring(rightIndex);
				return leftSort.compareTo(rightSort);
			}
		});
		
		// Remove sort order from all filenames if exist
		ListIterator<String> i = toReturn.listIterator();
		while(i.hasNext()) {
		    String file = i.next();
		    if (file.lastIndexOf(".") != -1) {
		    	i.set(file.substring(0, file.lastIndexOf(".")));
		    }
		}
		return toReturn;
	}
	
	// Tar Helpers
	
//	public static TarArchiveOutputStream createTarOutputStream(String filename) throws IOException {
//		FileOutputStream out = new FileOutputStream(filename);
//		return new TarArchiveOutputStream(
//				 new GzipCompressorOutputStream(out));
//	}
//	
//	public static TarArchiveInputStream createTarInputStream(String filename) throws IOException {
//		FileInputStream in = new FileInputStream(filename);
//		return new TarArchiveInputStream(
//				 new GzipCompressorInputStream(in));
//	}
//	
//	public static void tarFile(String dir, TarArchiveOutputStream os) throws IOException, FileNotFoundException {
//		tarFile(dir, new File(dir).getName(), os, null);
//	}
//	
//	public static void tarFile(String dir, String baseDir, TarArchiveOutputStream os, List<String> excludeFiles) throws IOException, FileNotFoundException {
//		FLog.c();
//		
//		try {		 
//		 tarDirToStream(dir, baseDir, os, excludeFiles);
//		}
//		finally {
//			if (os != null) os.close();
//		}
//	}
//	
//	private static void tarDirToStream(String dir, String tarname, TarArchiveOutputStream ts, List<String> excludeFiles) throws IOException, FileNotFoundException {
//		File d = new File(dir);
//		if (d.isDirectory()) {
//			String[] fileList = d.list();
//			
//			for (int i = 0; i < fileList.length; i++) {
//				File f = new File(d, fileList[i]);
//				if (f.isDirectory()) {
//					tarDirToStream(f.getPath(), tarname + f.getName() + "/", ts, excludeFiles);
//				} else {
//					
//					String tarFile = tarname + f.getName();
//					
//					boolean fileExcluded = false;
//					if (excludeFiles != null) {
//						
//						for (String exf : excludeFiles) {
//							if (exf.equals(tarFile)) {
//								fileExcluded = true;
//								break;
//							}
//						}
//					}
//					
//					if (!fileExcluded) {
//						tarFileToStream(f.getPath(), tarFile, ts);
//					}
//				}
//			}
//		} else {
//			tarFileToStream(d.getPath(), tarname, ts);
//		}
//	}
//	
//	private static void tarFileToStream(String filename, String tarname, TarArchiveOutputStream ts) throws IOException {
//		FileInputStream fs = null;
//		try {
//			File f = new File(filename);
//			fs = new FileInputStream(f);
//			TarArchiveEntry te = new TarArchiveEntry(f);
//			te.setName(tarname);
//			te.setSize(f.length());
//			ts.putArchiveEntry(te);
//			IOUtils.copy(fs, ts);
//			ts.closeArchiveEntry();
//		}
//		finally {
//			if (fs != null) fs.close();
//		}
//	}
//	
//	public static void untarFile(String dir, TarArchiveInputStream is) throws IOException {
//		try {
//			TarArchiveEntry e;
//			while((e = is.getNextTarEntry()) != null) {
//				if (e.isDirectory()) {
//					makeDirs(new File(dir + "/" + e.getName()));
//				} else {
//					writeTarFile(is, e, new File(dir + "/" + e.getName()));
//				}
//			}
//		} finally {
//			if (is != null) is.close();
//		}
//	}
//	
//	private static void writeTarFile(TarArchiveInputStream ts, TarArchiveEntry entry, File file) throws IOException {
//		// make sure directory path exists
//		makeDirs(file);
//		
//		FileOutputStream os = null;
//		try {
//			os = new FileOutputStream(file);
//	        IOUtils.copy(ts, os);
//		} finally {
//			if (os != null) os.close();
//		}
//	}
	
}


