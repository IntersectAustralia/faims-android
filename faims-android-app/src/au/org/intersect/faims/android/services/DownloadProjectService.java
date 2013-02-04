package au.org.intersect.faims.android.services;

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

public class DownloadProjectService extends IntentService {

	@Inject
	FAIMSClient faimsClient;
	
	private Thread downloadThread;
	
	private FAIMSClientResultCode resultCode;

	public DownloadProjectService() {
		super("DownloadProjectService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		RoboGuice.getBaseApplicationInjector(this.getApplication()).injectMembers(this);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("FAIMS", "stopping download service");
		downloadThread.interrupt();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("FAIMS", "starting download service");
		
		final String projectId = intent.getStringExtra("project");
		
		downloadThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					resultCode = faimsClient.downloadProjectArchive(projectId);
				} catch (Exception e) {
					Log.e("FAIMS", "download service failed", e);
				}
			}
			
		});
		downloadThread.start();
		
		try {
			// wait for project to download
			while(resultCode == null) {
				Thread.sleep(1000);
			}
			
			if (resultCode != FAIMSClientResultCode.SUCCESS) {
				faimsClient.invalidate();
			}
			
			Bundle extras = intent.getExtras();
			if (extras != null) {
				Messenger messenger = (Messenger) extras.get("MESSENGER");
				Message msg = Message.obtain();
				msg.obj = resultCode;
				try {
					messenger.send(msg);
				} catch (RemoteException e) {
					Log.e("FAIMS", "Cannot send download project message", e);
				}
			}
		} catch (Exception e) {
			Log.e("FAIMS", "download service failed", e);
		}
	}

}
