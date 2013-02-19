package au.org.intersect.faims.android.services;

import android.content.Intent;
import android.util.Log;
import au.org.intersect.faims.android.data.DownloadResult;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.ProjectUtil;

public class DownloadProjectService extends DownloadService {

	public DownloadProjectService() {
		super("DownloadProjectService");
	}
	
	@Override
	protected FAIMSClientResultCode doDownload(Intent intent) {
		try {
			Project project = (Project) intent.getExtras().get("project");
			Log.d("FAIMS", "downloading project " + project.name);
			DownloadResult result = faimsClient.downloadProject(project);
			
			// if result is success then update the project settings with version and timestamp
			if (result.code == FAIMSClientResultCode.SUCCESS) {
				project = ProjectUtil.getProject(project.key); // get the latest settings
				project.version = result.info.version;
				project.timestamp = DateUtil.getCurrentTimestampGMT();
				ProjectUtil.saveProject(project);
			}
			
			return result.code;
		} catch (Exception e) {
			Log.d("FAIMS", "could not download project");
		}
		return null;
	}

}
