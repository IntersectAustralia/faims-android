package au.org.intersect.faims.android.services;

import java.io.File;

import roboguice.RoboGuice;

import com.google.inject.Inject;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

public class UploadDatabaseService extends IntentService {

	@Inject
	FAIMSClient faimsClient;
	
	private Thread uploadThread;
	
	private FAIMSClientResultCode resultCode;

	public UploadDatabaseService() {
		super("UploadDatabaseService");
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
		uploadThread.interrupt();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("FAIMS", "starting upload service");
		
		Bundle extras = intent.getExtras();
		if (extras == null) {
			Log.d("FAIMS", "cannot find upload file");
			return;
		}
		
		final File file = (File) extras.get("database");
		final String projectId = intent.getStringExtra("projectId");
		uploadThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					resultCode = faimsClient.uploadDatabase(projectId, file);
				} catch (Exception e) {
					Log.e("FAIMS", "upload service failed", e);
				}
			}
			
		});
		uploadThread.start();
		
		try {
			// wait for database to upload
			while(resultCode == null) {
				Thread.sleep(1000);
			}
			
			if (resultCode != FAIMSClientResultCode.SUCCESS) {
				faimsClient.invalidate();
			}
			
			Messenger messenger = (Messenger) extras.get("MESSENGER");
			Message msg = Message.obtain();
			msg.obj = resultCode;
			try {
				messenger.send(msg);
			} catch (RemoteException e) {
				Log.e("FAIMS", "Cannot send upload service message", e);
			}
			
		} catch (Exception e) {
			Log.e("FAIMS", "upload service failed", e);
		}
	}

}
