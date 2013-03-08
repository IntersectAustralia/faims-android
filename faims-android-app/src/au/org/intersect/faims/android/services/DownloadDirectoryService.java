package au.org.intersect.faims.android.services;

import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

public class DownloadDirectoryService extends DownloadService {

	protected String downloadDir;
	protected String requestExcludePath;
	protected String infoPath;
	protected String downloadPath;

	public DownloadDirectoryService(String name) {
		super(name);
	}

	@Override
	protected FAIMSClientResultCode doDownload(Intent intent) {
		try {
			Project project = (Project) intent.getExtras().get("project");
			Log.d("FAIMS", "downloading directory for project " + project.name);
			
			String projectDir = Environment.getExternalStorageDirectory() + "/faims/projects/" + project.key;
			return faimsClient.downloadDirectory(projectDir, downloadDir, 
					"/android/project/" + project.key + "/" + requestExcludePath, 
					"/android/project/" + project.key + "/" + infoPath,
					"/android/project/" + project.key + "/" + downloadPath);
		} catch (Exception e) {
			Log.e("FAIMS", "could not download directory for project", e);
		}
		return null;
	}

}
