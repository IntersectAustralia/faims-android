package au.org.intersect.faims.android.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Environment;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;

import com.google.gson.JsonObject;

public class ProjectUtil {

	public static List<Project> getProjects() {
		final File dir = new File(Environment.getExternalStorageDirectory() + "/faims/projects");
		if (!dir.isDirectory()) return null;
		
		String[] directories = dir.list(new FilenameFilter() {

			@Override
			public boolean accept(File file, String arg1) {
				return dir.equals(file) && file.isDirectory();
			}
			
		});
		
		Arrays.sort(directories);
		ArrayList<Project> list = new ArrayList<Project>();
		FileInputStream is = null;
		try {
			for (String dirname : directories) {
				is = new FileInputStream(
							Environment.getExternalStorageDirectory() + "/faims/projects/" + dirname + "/project.settings");
				String config = FileUtil.convertStreamToString(is);
				if (config == null) {
					FAIMSLog.log("project " + "/faims/projects/" + dirname + "/project.settings" + " settings malformed");
					continue;
				}
				JsonObject object = JsonUtil.deserializeJson(config);
				Project project = Project.fromJson(dirname, object);	
				list.add(project);
			}
		} catch (IOException e) {
			FAIMSLog.log(e);
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
				FAIMSLog.log(e);
			}
		}
		return list;
	}

	public static Project getProject(
			String projectName) {
		List<Project> projects = getProjects();
		if (projects != null) {
			for (Project p : projects) {
				if (p.name.equals(projectName)) return p;
			}
		}
		return null;
	}

	public static void saveProject(Project project) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(Environment.getExternalStorageDirectory() + "/faims/projects/" + project.dir + "/project.settings"));
	    	writer.write(project.toJson().toString());
	    	writer.flush();
	    	writer.close();
		} catch (IOException e) {
			Log.d("FAIMS", "Error saving project");
		}
	}
	
}
