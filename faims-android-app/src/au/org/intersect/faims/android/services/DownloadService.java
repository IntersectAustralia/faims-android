package au.org.intersect.faims.android.services;

import roboguice.RoboGuice;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

import com.google.inject.Inject;

public abstract class DownloadService extends IntentService {

	@Inject
	FAIMSClient faimsClient;

	private boolean downloadStopped;

	public DownloadService(String name) {
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
		Log.d("FAIMS", "stopping download service");
		faimsClient.interrupt();
		downloadStopped = true;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("FAIMS", "starting download service");
		
		try {
			FAIMSClientResultCode resultCode = doDownload(intent);
			
			if (downloadStopped) {
				Log.d("FAIMS", "cancelled download");
				return;
			}
		
			if (resultCode != FAIMSClientResultCode.SUCCESS) {
				faimsClient.invalidate();
			}
			
			Bundle extras = intent.getExtras();
			Messenger messenger = (Messenger) extras.get("MESSENGER");
			Message msg = Message.obtain();
			msg.obj = resultCode;
			messenger.send(msg);
		} catch (Exception e) {
			Log.e("FAIMS", "download service failed", e);
		}
	}
	
	protected abstract FAIMSClientResultCode doDownload(Intent intent);

}
