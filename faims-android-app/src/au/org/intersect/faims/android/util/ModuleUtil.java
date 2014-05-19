package au.org.intersect.faims.android.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONObject;

import android.os.Environment;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.log.FLog;

public class ModuleUtil {

	public static List<Module> getModules() {
		final File dir = new File(Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir);
		if (!dir.isDirectory()) return null;
		
		String[] directories = dir.list(new FilenameFilter() {

			@Override
			public boolean accept(File file, String arg1) {
				return dir.equals(file) && file.isDirectory();
			}
			
		});
		
		ArrayList<Module> list = new ArrayList<Module>();
		FileInputStream is = null;
		
		for (String dirname : directories) {
			File f = new File(Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir + dirname + "/module.settings");
			if (f.exists()) {
				try {
					is = new FileInputStream(f);
					String config = FileUtil.convertStreamToString(is);
					JSONObject object = JsonUtil.deserializeJsonString(config);
					Module module = Module.fromJson(object);	
					list.add(module);
				} catch (Exception e) {
					FLog.w("cannot read modules settings " + FaimsSettings.modulesDir + dirname + "/module.settings", e);
					
					try {
						if (is != null)
							is.close();
					} catch (IOException ioe) {
						FLog.e("error closing file stream", ioe);
					}
				}
			} else {
				FLog.i("ignoring directory " + dirname);
			}
		}
		
		Collections.sort(list, new Comparator<Module>() {

			@Override
			public int compare(Module arg0, Module arg1) {
				return arg0.name.compareToIgnoreCase(arg1.name);
			}
			
		});
		
		return list;
	}

	public static Module getModule(
			String key) {
		List<Module> modules = getModules();
		if (modules != null) {
			for (Module p : modules) {
				if (p.key.equals(key)) return p;
			}
		}
		return null;
	}

	public static void saveModule(Module module) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir + module.key + "/module.settings"));
	    	writer.write(module.toJson().toString());
	    	writer.flush();
	    	writer.close();
		} catch (Exception e) {
			FLog.e("error saving module", e);
		}
	}
	
}
