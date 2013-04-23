package au.org.intersect.faims.android.services;

import roboguice.RoboGuice;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.DownloadResult;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

import com.google.inject.Inject;

public abstract class DownloadService extends IntentService {

	@Inject
	FAIMSClient faimsClient;

	protected boolean downloadStopped;

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
		faimsClient.interrupt();
		downloadStopped = true;
		FLog.d("stopping download service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		FLog.d("starting download service");
		
		DownloadResult result = null;
		try {
			result = doDownload(intent);
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				faimsClient.invalidate();
				FLog.d("download failure");
			}
			
		} catch (Exception e) {
			FLog.e("error in download service", e);
			result = DownloadResult.FAILURE;
		} finally {
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
	}
	
	protected abstract DownloadResult doDownload(Intent intent);

}
