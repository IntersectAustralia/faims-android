package au.org.intersect.faims.android.services;

import java.io.File;
import java.util.UUID;

import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import au.org.intersect.faims.android.data.DownloadResult;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.ProjectUtil;

public class SyncDownloadDatabaseService extends DownloadDatabaseService {

	public SyncDownloadDatabaseService() {
		super("SyncDownloadDatabaseService");
	}
	
	@Override
	protected FAIMSClientResultCode doDownload(Intent intent) {
		File tempDir = null;
		try {
			Project project = (Project) intent.getExtras().get("project");
			Log.d("FAIMS", "downloading database for " + project.name);
			
			tempDir = new File(Environment.getExternalStorageDirectory() + "/faims/projects/" + project.key + "/" + UUID.randomUUID());
			
			DownloadResult result = faimsClient.downloadDatabase(project, project.version, tempDir.getAbsolutePath());
			
			// if result is success then update the project settings with version and timestamp
			if (result.code == FAIMSClientResultCode.SUCCESS) {
				project = ProjectUtil.getProject(project.key); // get the latest settings
				project.version = result.info.version;
				project.timestamp = DateUtil.getCurrentTimestampGMT();
				ProjectUtil.saveProject(project);
			}
			
			return result.code;
		} catch (Exception e) {
			Log.d("FAIMS", "could not download database");
		} finally {
			if (tempDir != null) tempDir.delete();
		}
		return null;
	}
}
