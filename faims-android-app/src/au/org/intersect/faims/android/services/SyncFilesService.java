package au.org.intersect.faims.android.services;

import roboguice.RoboGuice;
import android.content.Intent;
import android.os.Environment;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

import com.google.inject.Inject;

public class SyncFilesService extends MessageIntentService {

	@Inject
	FAIMSClient faimsClient;
	
	private boolean syncStopped;
	
	public SyncFilesService() {
		super("SyncFilesService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		RoboGuice.getBaseApplicationInjector(this.getApplication()).injectMembers(this);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		syncStopped = true;
		faimsClient.interrupt();
		FLog.d("stopping service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		FLog.d("starting service");
		
		// 1. upload server directory
		// 2. upload app directory
		// 3. download app directory
		
		if (!uploadServerDirectory(intent)) return;
		if (!uploadAppDirectory(intent)) return;
		if (!downloadAppDirectory(intent)) return;
	}
	
	private boolean uploadServerDirectory(Intent intent) {
		FLog.d("uploading server directory");
		return uploadDirectory(intent, 
				this.getResources().getString(R.string.server_dir),
				"server_file_list",
				"server_file_upload",
				MessageType.SYNC_UPLOAD_SERVER_FILES);
	}
	
	private boolean uploadAppDirectory(Intent intent) {
		FLog.d("uploading app directory");
		return uploadDirectory(intent, 
				this.getResources().getString(R.string.app_dir),
				"app_file_list",
				"app_file_upload",
				MessageType.SYNC_UPLOAD_APP_FILES);
	}

	private boolean downloadAppDirectory(Intent intent) {
		FLog.d("downloading app directory");
		return downloadDirectory(intent,
				this.getResources().getString(R.string.app_dir),
				"app_file_list",
				"app_file_archive",
				"app_file_download", 
				MessageType.SYNC_DOWNLOAD_APP_FILES);
	}

	private boolean uploadDirectory(Intent intent, String uploadDir, String requestExcludePath, String uploadPath, MessageType type) {
		FAIMSClientResultCode result = null;
		try {
			Project project = (Project) intent.getExtras().get("project");
			String projectDir = Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key;
			
			result = faimsClient.uploadDirectory(projectDir, 
					uploadDir, 
					"/android/project/" + project.key + "/" + requestExcludePath, 
					"/android/project/" + project.key + "/" + uploadPath);

			if (syncStopped) {
				result = null;
				return false;
			}
			
			if (result != FAIMSClientResultCode.SUCCESS) {
				faimsClient.invalidate();
				return false;
			}
			
			FLog.d("uploaded dir " + uploadDir + " success");
			return true;
		} catch (Exception e) {
			FLog.e("uploading dir " + uploadDir + " error");
			result = FAIMSClientResultCode.SERVER_FAILURE;
		} finally {
			try {
				sendMessage(intent, type, result);
			} catch (Exception me) {
				FLog.e("error sending message", me);
			}
		}
		return false;
	}
	
	private boolean downloadDirectory(Intent intent, String downloadDir, String requestExcludePath, String infoPath, String downloadPath, MessageType type) {
		FAIMSClientResultCode result = null;
		try {
			Project project = (Project) intent.getExtras().get("project");
			
			String projectDir = Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key;
			result = faimsClient.downloadDirectory(projectDir, downloadDir, 
					"/android/project/" + project.key + "/" + requestExcludePath, 
					"/android/project/" + project.key + "/" + infoPath,
					"/android/project/" + project.key + "/" + downloadPath);
			
			if (syncStopped) {
				result = null;
				return false;
			}
		
			if (result != FAIMSClientResultCode.SUCCESS) {
				faimsClient.invalidate();
				return false;
			}
			
			FLog.d("downloading dir " + downloadDir + " success");
			return true;
		} catch (Exception e) {
			FLog.e("downloading dir " + downloadDir + " error");
			result = FAIMSClientResultCode.SERVER_FAILURE;
		} finally {
			try {
				sendMessage(intent, type, result);
				
			} catch (Exception me) {
				FLog.e("error sending message", me);
			}
		}
		return false;
	}

}
