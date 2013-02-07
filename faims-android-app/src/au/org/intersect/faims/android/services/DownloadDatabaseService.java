package au.org.intersect.faims.android.services;

import android.content.Intent;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

public class DownloadDatabaseService extends DownloadService {

	public DownloadDatabaseService() {
		super("DownloadDatabaseService");
	}
	
	@Override
	protected FAIMSClientResultCode doDownload(Intent intent) {
		try {
			Project project = (Project) intent.getExtras().get("project");
			Log.d("FAIMS", "downloading database for " + project.name);
			return faimsClient.downloadDatabase(project);
		} catch (Exception e) {
			Log.d("FAIMS", "could not download database");
		}
		return null;
	}

}
