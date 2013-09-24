package au.org.intersect.faims.android.services;

import java.io.File;

import roboguice.RoboGuice;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.LockManager;
import au.org.intersect.faims.android.net.DownloadResult;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ProjectUtil;

import com.google.inject.Inject;

public class SyncFilesService extends IntentService {

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
		Bundle extras = intent.getExtras();
		Project project = (Project) extras.get("project");
		String dumpTimeStamp = DateUtil.getCurrentTimestampGMT();
		project.fileSyncTimeStamp = dumpTimeStamp;
		FLog.d("set fileSyncTimeStamp");
		ProjectUtil.saveProject(project);
		Result uploadServerResult = uploadServerDirectory(intent);
		if (uploadServerResult.resultCode != FAIMSClientResultCode.SUCCESS) {
			sendMessage(intent, uploadServerResult);
			return;
		}
		
		Result uploadAppResult = uploadAppDirectory(intent);
		if (uploadAppResult.resultCode != FAIMSClientResultCode.SUCCESS) {
			sendMessage(intent, uploadAppResult);
			return;
		}
		
		Result downloadAppResult = downloadAppDirectory(intent);
		sendMessage(intent, downloadAppResult);
	}
	
	private void sendMessage(Intent intent, Result result) {
		try {
			Bundle extras = intent.getExtras();
			Messenger messenger = (Messenger) extras.get("MESSENGER");
			Message msg = Message.obtain();
			msg.obj = result;
			messenger.send(msg);
		} catch (Exception me) {
			FLog.e("error sending message", me);
		}
	}
	
	private Result uploadServerDirectory(Intent intent) {
		FLog.d("uploading server directory");
		return uploadDirectory(intent, 
				this.getResources().getString(R.string.server_dir),
				"server_file_list",
				"server_file_upload");
	}
	
	private Result uploadAppDirectory(Intent intent) {
		FLog.d("uploading app directory");
		return uploadDirectory(intent, 
				this.getResources().getString(R.string.app_dir),
				"app_file_list",
				"app_file_upload");
	}

	private Result downloadAppDirectory(Intent intent) {
		FLog.d("downloading app directory");
		return downloadDirectory(intent,
				this.getResources().getString(R.string.app_dir),
				"app_file_list",
				"app_file_archive",
				"app_file_download");
	}

	private Result uploadDirectory(Intent intent, String uploadDir, String requestExcludePath, String uploadPath) {
		String lock = null;
		try {
			Project project = (Project) intent.getExtras().get("project");
			String projectDir = Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key;
			
			lock = projectDir + "/.lock";
			waitForLock(lock);
			
			Result uploadResult = faimsClient.uploadDirectory(projectDir, 
					uploadDir, 
					"/android/project/" + project.key + "/" + requestExcludePath, 
					"/android/project/" + project.key + "/" + uploadPath);
			
			if (syncStopped) {
	    		FLog.d("sync cancelled");
	    		return Result.INTERRUPTED;
	    	}
			
			if (uploadResult.resultCode == FAIMSClientResultCode.FAILURE) {
				faimsClient.invalidate();
				FLog.d("upload failure");
				return uploadResult;
			}
			
			FLog.d("uploaded dir " + uploadDir + " success");
			return uploadResult;
		} catch (Exception e) {
			FLog.e("uploading dir " + uploadDir + " error");
			return Result.FAILURE;
		} finally {
			if (lock != null) LockManager.clearLock(lock);
		}
	}
	
	private Result downloadDirectory(Intent intent, String downloadDir, String requestExcludePath, String infoPath, String downloadPath) {
		String lock = null;
		try {
			Project project = (Project) intent.getExtras().get("project");
			String projectDir = Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key;
			
			lock = projectDir + "/.lock";
			waitForLock(lock);
			
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
			if (lock != null) LockManager.clearLock(lock);
		}
	}
	
	// note: cannot use lock manager as we need to check if service has stopped
	private void waitForLock(String filename) throws Exception {
		while(!syncStopped && new File(filename).exists()) {
			Thread.sleep(1000);
		}
		FileUtil.touch(new File(filename));
	}

}
