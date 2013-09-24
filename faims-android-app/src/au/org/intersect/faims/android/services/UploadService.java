package au.org.intersect.faims.android.services;

import java.io.File;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import roboguice.RoboGuice;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.util.FileUtil;

import com.google.inject.Inject;

public abstract class UploadService extends IntentService {

	@Inject
	FAIMSClient faimsClient;
	
	@Inject
	DatabaseManager databaseManager;
	
	protected boolean uploadStopped;

	protected File tempFile;
	
	protected TarArchiveOutputStream os;

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
		uploadStopped = true;
		faimsClient.interrupt();
		if (os != null) {
			try {
				os.close();
			} catch (Exception e) {
				FLog.e("error closing steam", e);
			}
		}
		if (tempFile != null) {
			FileUtil.delete(tempFile);
		}
		FLog.d("stopping upload service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		FLog.d("starting upload service");
		
		Result result = null;
		try {
			result = doUpload(intent);
			
			if (uploadStopped) {
				FLog.d("upload cancelled");
				result.resultCode = FAIMSClientResultCode.INTERRUPTED;
			}
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				faimsClient.invalidate();
				FLog.d("upload failure");
			}
			
		} catch (Exception e) {
			FLog.e("error in upload service", e);
			result = Result.FAILURE;
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
	
	protected abstract Result doUpload(Intent intent) throws Exception;

}
