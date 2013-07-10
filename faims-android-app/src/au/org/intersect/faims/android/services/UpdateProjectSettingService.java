package au.org.intersect.faims.android.services;

import android.content.Intent;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.DownloadResult;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.ProjectUtil;

public class UpdateProjectSettingService extends DownloadService {

	public UpdateProjectSettingService() {
		super("UpdateProjectSettingService");
	}

	@Override
	protected DownloadResult doDownload(Intent intent) {
		try {
			Project project = (Project) intent.getExtras().get("project");
			
			FLog.d("update project setting for project " + project.name);
			
			// 1. download settings (ui schema, ui logic, project settings, properties file(s))
			
			// 1.
			DownloadResult result = faimsClient.downloadSettings(project);
			
			if (downloadStopped) {
				FLog.d("update project setting cancelled");
				return DownloadResult.INTERRUPTED;
			}
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				FLog.d("update project setting failure");
				return result;
			}
			
			String version = result.info.version;
			
			// if result is success then update the project settings with version and timestamp
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				project = ProjectUtil.getProject(project.key); // get the latest settings
				project.version = version;
				ProjectUtil.saveProject(project);
			}
			
			return result;
		} catch (Exception e) {
			
			FLog.e("error updating project", e);
		}
		return null;
	}

}
