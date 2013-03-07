package au.org.intersect.faims.android.services;

import android.content.Intent;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

public class DownloadAppDirectoryService extends DownloadService {

	public DownloadAppDirectoryService() {
		super("DownloadAppDirectoryService");
	}

	@Override
	protected FAIMSClientResultCode doDownload(Intent intent) {
		try {
			Project project = (Project) intent.getExtras().get("project");
			Log.d("FAIMS", "downloading app directory for project " + project.name);
			
			//return faimsClient.downloadAppDirectory(project);
		} catch (Exception e) {
			Log.e("FAIMS", "could not download app directory for project", e);
		}
		return null;
	}

}
