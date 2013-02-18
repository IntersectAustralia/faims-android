package au.org.intersect.faims.android.services;

import android.content.Intent;
import android.util.Log;
import au.org.intersect.faims.android.data.DownloadResult;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.ProjectUtil;

public class DownloadDatabaseService extends DownloadService {

	public DownloadDatabaseService() {
		super("DownloadDatabaseService");
	}
	
	@Override
	protected FAIMSClientResultCode doDownload(Intent intent) {
		try {
			Project project = (Project) intent.getExtras().get("project");
			Log.d("FAIMS", "downloading database for " + project.name);
			DownloadResult result = faimsClient.downloadDatabase(project);
			
			// if result is success then update the project settings with version and timestamp
			if (result.code == FAIMSClientResultCode.SUCCESS) {
				project = ProjectUtil.getProject(project.key); // get the latest settings
				project.version = result.info.version;
				project.timestamp = result.info.timestamp;
				ProjectUtil.saveProject(project);
			}
			
			return result.code;
		} catch (Exception e) {
			Log.d("FAIMS", "could not download database");
		}
		return null;
	}

}
