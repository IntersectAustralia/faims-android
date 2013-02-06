package au.org.intersect.faims.android.services;

import java.io.File;

import roboguice.RoboGuice;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import au.org.intersect.faims.android.managers.DatabaseManager;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

import com.google.inject.Inject;

public class UploadDatabaseService extends IntentService {

	@Inject
	FAIMSClient faimsClient;
	
	private boolean uploadStopped;

	private File file;

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
		faimsClient.interrupt();
		uploadStopped = true;
		if (file != null) {
			file.delete();
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("FAIMS", "starting upload service");
		
		try {
			// create temp database to upload
			DatabaseManager dbmgr = new DatabaseManager(intent.getStringExtra("database"));
			
	    	file = File.createTempFile("tempdb_", ".sqlite3", new File(Environment.getExternalStorageDirectory() + "/faims/projects/" + intent.getStringExtra("projectDir")));
	    	dbmgr.dumpDatabaseTo(file);
	    	
	    	if (uploadStopped) {
	    		Log.d("FAIMS", "cancelled upload");
	    		return;
	    	}
	    	
	    	String projectId = intent.getStringExtra("projectId");
			FAIMSClientResultCode resultCode = faimsClient.uploadDatabase(projectId, file);
			
			if (uploadStopped) {
	    		Log.d("FAIMS", "cancelled upload");
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
			Log.e("FAIMS", "upload service failed", e);
		}
	}

}
