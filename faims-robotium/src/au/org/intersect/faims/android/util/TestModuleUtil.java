package au.org.intersect.faims.android.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.utils.IOUtils;

import android.content.res.AssetManager;
import android.os.Environment;
import au.org.intersect.faims.android.constants.FaimsSettings;

import com.google.gson.JsonObject;

public class TestModuleUtil {
	
	public static void createModuleFrom(String name, String key, String dirname, AssetManager assetManager) {
		try {
			String dir = Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir + key;
			File file = new File(dir);
			if (!file.isDirectory())
				file.mkdirs();
			
			copyFile(assetManager.open(dirname + "/data_schema.xml"), new File(dir + "/data_schema.xml"));
			copyFile(assetManager.open(dirname + "/ui_schema.xml"), new File(dir + "/ui_schema.xml"));
			copyFile(assetManager.open(dirname + "/ui_logic.bsh"), new File(dir + "/ui_logic.bsh"));
			copyFile(assetManager.open(dirname + "/db.sqlite3"), new File(dir + "/db.sqlite3"));
			
			JsonObject object = new JsonObject();
	    	object.addProperty("name", name);
	    	object.addProperty("key", key);
	    	
	    	BufferedWriter writer = new BufferedWriter(new FileWriter(dir + "/module.settings"));
	    	writer.write(object.toString());
	    	writer.flush();
	    	writer.close();
		} catch (IOException e) {
			System.out.println(e);
			e.printStackTrace();
		}
	}
	
	public static void copyFile(InputStream source, File destFile) throws IOException {
		if(!destFile.exists()) {
			destFile.createNewFile();
	    }
		
		OutputStream destination = null;
		
		try {
			destination = new FileOutputStream(destFile);
			IOUtils.copy(source, destination);
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
	
	public static String getNewModuleName(String baseName) {
		return baseName + "-" + System.currentTimeMillis();
	}
}
