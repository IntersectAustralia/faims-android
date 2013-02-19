package au.org.intersect.faims.android.services;

import java.io.File;

import roboguice.RoboGuice;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.managers.DatabaseManager;
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
		Log.d("FAIMS", "stopping upload service");
		faimsClient.interrupt();
		uploadStopped = true;
		if (file != null) {
			file.delete();
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("FAIMS", "starting upload service");
		
		FAIMSClientResultCode resultCode = null;
		try {
			resultCode = doUpload(intent);
			
			if (uploadStopped) {
				Log.d("FAIMS", "cancelled upload");
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
			Log.e("FAIMS", "upload service failed", e);
			resultCode = FAIMSClientResultCode.SERVER_FAILURE;
		} finally {
			try {
				Bundle extras = intent.getExtras();
				Messenger messenger = (Messenger) extras.get("MESSENGER");
				Message msg = Message.obtain();
				msg.obj = resultCode;
				messenger.send(msg);
			} catch (Exception me) {
				Log.e("FAIMS", "upload service messenger failed", me);
			}
		}
	}
	
	protected abstract FAIMSClientResultCode doUpload(Intent intent) throws Exception;
	
	protected void doComplete(FAIMSClientResultCode resultCode, Project project) throws Exception {
		
	}

}
