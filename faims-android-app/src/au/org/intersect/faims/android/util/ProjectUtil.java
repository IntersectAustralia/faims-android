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

import android.os.Environment;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.log.FLog;

import com.google.gson.JsonObject;

public class ProjectUtil {

	public static List<Project> getProjects() {
		final File dir = new File(Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir);
		if (!dir.isDirectory()) return null;
		
		String[] directories = dir.list(new FilenameFilter() {

			@Override
			public boolean accept(File file, String arg1) {
				return dir.equals(file) && file.isDirectory();
			}
			
		});
		
		ArrayList<Project> list = new ArrayList<Project>();
		FileInputStream is = null;
		
		for (String dirname : directories) {
			File f = new File(Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + dirname + "/project.settings");
			if (f.exists()) {
				try {
					is = new FileInputStream(f);
					String config = FileUtil.convertStreamToString(is);
					JsonObject object = JsonUtil.deserializeJson(config);
					Project project = Project.fromJson(object);	
					list.add(project);
				} catch (Exception e) {
					FLog.w("cannot read projects settings " + FaimsSettings.projectsDir + dirname + "/project.settings", e);
					
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
		
		Collections.sort(list, new Comparator<Project>() {

			@Override
			public int compare(Project arg0, Project arg1) {
				return arg0.name.compareToIgnoreCase(arg1.name);
			}
			
		});
		
		return list;
	}

	public static Project getProject(
			String key) {
		List<Project> projects = getProjects();
		if (projects != null) {
			for (Project p : projects) {
				if (p.key.equals(key)) return p;
			}
		}
		return null;
	}

	public static void saveProject(Project project) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key + "/project.settings"));
	    	writer.write(project.toJson().toString());
	    	writer.flush();
	    	writer.close();
		} catch (IOException e) {
			FLog.e("error saving project", e);
		}
	}
	
}
