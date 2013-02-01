package au.org.intersect.faims.android.services;

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

	private FAIMSClient faimsClient;

	public DownloadProjectService(FAIMSClient faimsClient) {
		super("DownloadProjectService");
		this.faimsClient = faimsClient;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String projectId = intent.getStringExtra("project");
		FAIMSClientResultCode resultCode = faimsClient.downloadProjectArchive(projectId);
		
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
	}

}
