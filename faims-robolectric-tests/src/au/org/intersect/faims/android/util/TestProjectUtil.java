package au.org.intersect.faims.android.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.os.Environment;

import com.google.gson.JsonObject;

public class TestProjectUtil {

	public static void createProject(String name, String key) {
		try {
			String dir = Environment.getExternalStorageDirectory() + "/faims/projects/" + name.replaceAll("\\s+", "_");
			File file = new File(dir);
			if (!file.isDirectory())
				file.mkdirs();
			
			copyFile(new File("assets/data_schema.xml"), new File(dir + "/data_schema.xml"));
			copyFile(new File("assets/ui_schema.xml"), new File(dir + "/ui_schema.xml"));
			copyFile(new File("assets/ui_logic.bsh"), new File(dir + "/ui_logic.bsh"));
			copyFile(new File("assets/db.sqlite3"), new File(dir + "/db.sqlite3"));
			
			JsonObject object = new JsonObject();
	    	object.addProperty("name", name);
	    	object.addProperty("key", key);
	    	
	    	BufferedWriter writer = new BufferedWriter(new FileWriter(dir + "/project.settings"));
	    	writer.write(object.toString());
	    	writer.flush();
	    	writer.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	public static void createProjectFrom(String name, String dirname, String key) {
		try {
			String dir = Environment.getExternalStorageDirectory() + "/faims/projects/" + name.replaceAll("\\s+", "_");
			File file = new File(dir);
			if (!file.isDirectory())
				file.mkdirs();
			
			String dirpath = "test_data/" + dirname;
			
			copyFile(new File(dirpath + "/data_schema.xml"), new File(dir + "/data_schema.xml"));
			copyFile(new File(dirpath + "/ui_schema.xml"), new File(dir + "/ui_schema.xml"));
			copyFile(new File(dirpath + "/ui_logic.bsh"), new File(dir + "/ui_logic.bsh"));
			copyFile(new File(dirpath + "/db.sqlite3"), new File(dir + "/db.sqlite3"));
			
			JsonObject object = new JsonObject();
	    	object.addProperty("name", name);
	    	object.addProperty("key", key);
	    	
	    	BufferedWriter writer = new BufferedWriter(new FileWriter(dir + "/project.settings"));
	    	writer.write(object.toString());
	    	writer.flush();
	    	writer.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}

}
