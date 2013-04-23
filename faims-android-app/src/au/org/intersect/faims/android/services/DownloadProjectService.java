package au.org.intersect.faims.android.services;

import android.content.Intent;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.DownloadResult;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.ProjectUtil;

public class DownloadProjectService extends DownloadService {

	public DownloadProjectService() {
		super("DownloadProjectService");
	}
	
	@Override
	protected DownloadResult doDownload(Intent intent) {
		try {
			Project project = (Project) intent.getExtras().get("project");
			
			FLog.d("downloading project " + project.name);
			
			DownloadResult result = faimsClient.downloadProject(project);
			
			// if result is success then update the project settings with version and timestamp
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				project = ProjectUtil.getProject(project.key); // get the latest settings
				project.version = result.info.version;
				project.timestamp = DateUtil.getCurrentTimestampGMT(); // note: updating timestamp as database is overwritten
				ProjectUtil.saveProject(project);
			}
			
			return result;
		} catch (Exception e) {
			FLog.e("error downloading project", e);
		}
		return null;
	}

}
