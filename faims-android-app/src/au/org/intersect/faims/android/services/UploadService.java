package au.org.intersect.faims.android.services;

import java.io.File;

import roboguice.RoboGuice;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

import com.google.inject.Inject;

public abstract class UploadService extends IntentService {

	@Inject
	FAIMSClient faimsClient;
	
	@Inject
	DatabaseManager databaseManager;
	
	protected boolean uploadStopped;

	protected File file;

	public UploadService(String name) {
		super(name);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		RoboGuice.getBaseApplicationInjector(this.getApplication()).injectMembers(this);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		faimsClient.interrupt();
		uploadStopped = true;
		if (file != null) {
			file.delete();
		}
		FLog.d("stopping upload service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		FLog.d("starting upload service");
		
		FAIMSClientResultCode resultCode = null;
		try {
			resultCode = doUpload(intent);
			
			if (uploadStopped) {
				FLog.d("upload cancelled");
				resultCode = null;
				return;
			}
			
			if (resultCode != FAIMSClientResultCode.SUCCESS) {
				faimsClient.invalidate();
			}
			
			Bundle extras = intent.getExtras();
			Project project = (Project) extras.get("project");
			
			doComplete(resultCode, project);
			
		} catch (Exception e) {
			FLog.e("error in upload service", e);
			resultCode = FAIMSClientResultCode.SERVER_FAILURE;
		} finally {
			try {
				Bundle extras = intent.getExtras();
				Messenger messenger = (Messenger) extras.get("MESSENGER");
				Message msg = Message.obtain();
				msg.obj = resultCode;
				messenger.send(msg);
			} catch (Exception me) {
				FLog.e("error sending message", me);
			}
		}
	}
	
	protected abstract FAIMSClientResultCode doUpload(Intent intent) throws Exception;
	
	protected void doComplete(FAIMSClientResultCode resultCode, Project project) throws Exception {
		
	}

}
