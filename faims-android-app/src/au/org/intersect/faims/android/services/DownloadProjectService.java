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
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ProjectUtil;

public class DownloadProjectService extends DownloadService {

	public DownloadProjectService() {
		super("DownloadProjectService");
	}
	
	@Override
	protected DownloadResult doDownload(Intent intent) {
		File projectDir = null;
		try {
			Project project = (Project) intent.getExtras().get("project");
			
			FLog.d("downloading project " + project.name);
			
			// 0. delete project if it exists
			// 1. download settings (ui schema, ui logic, project settings, properties file(s))
			// 2. download database
			// 3. download data files
			// 4. download app files
			
			// 0.
			
			projectDir = new File(Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key);
			FileUtil.deleteDirectory(projectDir);
			
			// 1.
			DownloadResult result = faimsClient.downloadSettings(project);
			
			if (downloadStopped) {
				FileUtil.deleteDirectory(projectDir);
				
				FLog.d("download cancelled");
				return DownloadResult.INTERRUPTED;
			}
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				FileUtil.deleteDirectory(projectDir);
				
				FLog.d("download settings failure");
				return result;
			}
			
			String version = result.info.version;
			
			// 2.
			result = faimsClient.downloadDatabase(project);
			
			if (downloadStopped) {
				FileUtil.deleteDirectory(projectDir);
				
				FLog.d("download cancelled");
				return DownloadResult.INTERRUPTED;
			}
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				FileUtil.deleteDirectory(projectDir);
				
				FLog.d("download database failure");
				return result;
			}
			
			// 3.
			result = downloadDataDirectory(intent);
			
			if (downloadStopped) {
				FileUtil.deleteDirectory(projectDir);
				
				FLog.d("download cancelled");
				return DownloadResult.INTERRUPTED;
			}
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				FileUtil.deleteDirectory(projectDir);
				
				FLog.d("download data directory failure");
				return result;
			}
			
			// 4.
			result = downloadAppDirectory(intent);
			
			if (downloadStopped) {
				FileUtil.deleteDirectory(projectDir);
				
				FLog.d("download cancelled");
				return DownloadResult.INTERRUPTED;
			}
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				FileUtil.deleteDirectory(projectDir);
				
				FLog.d("download app directory failure");
				return result;
			}
			
			// if result is success then update the project settings with version and timestamp
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				project = ProjectUtil.getProject(project.key); // get the latest settings
				project.version = version;
				project.timestamp = DateUtil.getCurrentTimestampGMT(); // note: updating timestamp as database is overwritten
				ProjectUtil.saveProject(project);
			}
			
			return result;
		} catch (Exception e) {
			if (projectDir != null)
				FileUtil.deleteDirectory(projectDir);
			
			FLog.e("error downloading project", e);
		}
		return null;
	}
	
	private DownloadResult downloadAppDirectory(Intent intent) {
		FLog.d("downloading app directory");
		return downloadDirectory(intent,
				this.getResources().getString(R.string.app_dir),
				"app_file_list",
				"app_file_archive",
				"app_file_download");
	}
	
	private DownloadResult downloadDataDirectory(Intent intent) {
		FLog.d("downloading data directory");
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
