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
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.managers.DatabaseManager;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.FileUtil;

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
		
		File tempFile = null;
		try {
			String database = intent.getStringExtra("database");
			String userId = intent.getStringExtra("userId");
			Bundle extras = intent.getExtras();
			Project project = (Project) extras.get("project");
			
			// create temp database to upload
			DatabaseManager dbmgr = new DatabaseManager(database);
			
			File outputDir = new File(Environment.getExternalStorageDirectory() + "/faims/projects/" + project.dir);
			
	    	tempFile = File.createTempFile("temp_", ".sqlite3", outputDir);
	    	dbmgr.dumpDatabaseTo(tempFile);
	    	
	    	if (uploadStopped) {
	    		Log.d("FAIMS", "cancelled upload");
	    		return;
	    	}
	    	
	    	// tar file
	    	file = File.createTempFile("temp_", ".tar.gz", outputDir);
	    	FileUtil.tarFile(tempFile.getAbsolutePath(), file.getAbsolutePath());
	    	
	    	if (uploadStopped) {
	    		Log.d("FAIMS", "cancelled upload");
	    		return;
	    	}
	    	
	    	// upload database
			FAIMSClientResultCode resultCode = faimsClient.uploadDatabase(project, file, userId);
			
			if (uploadStopped) {
	    		Log.d("FAIMS", "cancelled upload");
	    		return;
	    	}
			
			if (resultCode != FAIMSClientResultCode.SUCCESS) {
				faimsClient.invalidate();
			}
			
			Messenger messenger = (Messenger) extras.get("MESSENGER");
			Message msg = Message.obtain();
			msg.obj = resultCode;
			messenger.send(msg);
			
		} catch (Exception e) {
			Log.e("FAIMS", "upload service failed", e);
		} finally {
			if (tempFile != null) tempFile.delete();
		}
	}

}
