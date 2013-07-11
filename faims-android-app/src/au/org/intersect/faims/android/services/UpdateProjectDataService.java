package au.org.intersect.faims.android.services;

import java.io.File;

import android.content.Intent;
import android.os.Environment;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.DownloadResult;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.FileUtil;

public class UpdateProjectDataService extends DownloadService {

	public UpdateProjectDataService() {
		super("UpdateProjectDataService");
	}

	@Override
	protected DownloadResult doDownload(Intent intent) {
		try {
			Project project = (Project) intent.getExtras().get("project");
			String projectDir = Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key;
			
			FLog.d("update project data for project " + project.name);
			
			// 1. download project data
			
			// 1.
			DownloadResult result = downloadDataDirectory(intent);
			
			if (downloadStopped) {
				FileUtil.deleteDirectory(new File(projectDir + "/" + this.getResources().getString(R.string.data_dir)));
				FLog.d("update project data cancelled");
				return DownloadResult.INTERRUPTED;
			}
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				FileUtil.deleteDirectory(new File(projectDir + "/" + this.getResources().getString(R.string.data_dir)));
				FLog.d("update project data directory failure");
				return result;
			}
			
			return result;
		} catch (Exception e) {
			
			FLog.e("error updating project", e);
		}
		return null;
	}

	private DownloadResult downloadDataDirectory(Intent intent) {
		FLog.d("updating data directory");
		return downloadDirectory(intent,
				this.getResources().getString(R.string.data_dir),
				"data_file_list",
				"data_file_archive",
				"data_file_download");
	}

	private DownloadResult downloadDirectory(Intent intent, String downloadDir, String requestExcludePath, String infoPath, String downloadPath) {
		try {
			Project project = (Project) intent.getExtras().get("project");
			String projectDir = Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key;
			
			DownloadResult downloadResult = faimsClient.downloadDirectory(projectDir, downloadDir, 
					"/android/project/" + project.key + "/" + requestExcludePath, 
					"/android/project/" + project.key + "/" + infoPath,
					"/android/project/" + project.key + "/" + downloadPath);
		
			if (downloadResult.resultCode == FAIMSClientResultCode.FAILURE) {
				faimsClient.invalidate();
				FLog.d("download failure");
				return downloadResult;
			}
			
			FLog.d("downloading dir " + downloadDir + " success");
			return downloadResult;
		} catch (Exception e) {
			FLog.e("downloading dir " + downloadDir + " error");
			return DownloadResult.FAILURE;
		} finally {
			
		}
	}
}
