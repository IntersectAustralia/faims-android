package au.org.intersect.faims.android.services;

import java.io.File;

import roboguice.RoboGuice;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.managers.DatabaseManager;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.tasks.ActionResultCode;
import au.org.intersect.faims.android.tasks.IActionListener;
import au.org.intersect.faims.android.tasks.LocateServerTask;
import au.org.intersect.faims.android.util.FileUtil;

import com.google.inject.Inject;

public class SyncService extends IntentService {
	
	private static final int SYNC_MIN_INTERVAL = 5;
	private static final int SYNC_MAX_INTERVAL = 5;
	private static final int SYNC_DELAY_INTERVAL = 5;
	
	enum SyncMode {
		LOCATING_SERVER,
		UPLOADING,
		START
	}
	
	@Inject
	ServerDiscovery serverDiscovery;
	
	@Inject
	FAIMSClient faimsClient;
	
	@Inject
	DatabaseManager databaseManager;

	private boolean synching;
	private int syncInterval;
	private SyncMode syncMode;
	private Project project;
	private String userId;
	private String database;
	private File file;
	
	public SyncService() {
		super("SyncService");
	}
	
	@Override
	public void onCreate() {
		Log.d("FAIMS", "SyncService.onCreate");
		
		super.onCreate();
		RoboGuice.getBaseApplicationInjector(this.getApplication()).injectMembers(this);
		
		synching = true;
		syncInterval = SYNC_MIN_INTERVAL;
		syncMode = SyncMode.START;
	}
	
	@Override 
	public void onDestroy() {
		Log.d("FAIMS", "SyncService.onDestroy");
		
		super.onDestroy();
		synching = false;
		
		faimsClient.interrupt();
		
		if (file != null) {
			file.delete();
		}
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("FAIMS", "SyncService.onHandleIntent");
		
		try {
			Bundle extras = intent.getExtras();
			project = (Project) extras.get("project");
			userId = intent.getStringExtra("userId");
			database = Environment.getExternalStorageDirectory() + "/faims/projects/" + project.key + "/db.sqlite3";
			
			while(synching) {
				
				switch(syncMode) {
					case LOCATING_SERVER:
						doLocatingServer();
						break;
					case UPLOADING:
						doUploading();
						break;
					default:
						startSync();
						break;
				}
				
			}
			
		} catch (Exception e) {
			Log.e("FAIMS", "Error during sync", e);
		}
		
	}
	
	private void startSync() {
		if (!serverDiscovery.isServerHostValid()) {
			locateServer();
		} else {
			startUpload();
		}
	}
	
	private void doLocatingServer() throws InterruptedException {
		Thread.sleep(1000); // wait for server to be valid
	}
	
	private void doUploading() throws InterruptedException {
		Thread.sleep(1000); // wait upload to complete
	}
	
	private void locateServer() {
		Log.d("FAIMS", "locating server");
		
		syncMode = SyncMode.LOCATING_SERVER;
		
		new LocateServerTask(serverDiscovery, new IActionListener() {

			@Override
			public void handleActionResponse(ActionResultCode resultCode,
					Object data) {
				Log.d("FAIMS", "locating server completed");
				if (resultCode == ActionResultCode.SUCCESS) {
					SyncService.this.startUpload();
				} else {
					Log.d("FAIMS", "sync cannot find server");
					SyncService.this.retrySync();
				}
			}
    		
    	}).execute();
	}
	
	@SuppressLint("HandlerLeak")
	private void startUpload() {
		Log.d("FAIMS", "starting sync upload");
		
		syncMode = SyncMode.UPLOADING;

		File tempFile = null;
		try {
			// create temp database to upload
			databaseManager.init(database);
			
			File outputDir = new File(Environment.getExternalStorageDirectory() + "/faims/projects/" + project.key);
			
	    	tempFile = File.createTempFile("temp_", ".sqlite3", outputDir);
	    	
	    	if (project.timestamp != null) {
	    		databaseManager.dumpDatabaseTo(tempFile, project.timestamp);
	    	} else {
	    		databaseManager.dumpDatabaseTo(tempFile);
	    	}
	    	
	    	if (!synching) {
	    		Log.d("FAIMS", "sync upload cancelled");
	    		return;
	    	}
	    	
	    	// tar file
	    	file = File.createTempFile("temp_", ".tar.gz", outputDir);
	    	FileUtil.tarFile(tempFile.getAbsolutePath(), file.getAbsolutePath());
	    	
	    	if (!synching) {
	    		Log.d("FAIMS", "sync upload cancelled");
	    		return;
	    	}
	    	
	    	// upload database
			FAIMSClientResultCode resultCode = faimsClient.uploadDatabase(project, file, userId);
			
			if (resultCode == FAIMSClientResultCode.SUCCESS) {
				resetSync();
				startSync();
			} else {
				retrySync();
			}
			
		} catch (Exception e) {
			Log.e("FAIMS", "Error during sync upload", e);
		} finally {
			if (tempFile != null) tempFile.delete();
		}
	}
	
	private void delaySync() {
		try {
			syncInterval += SYNC_DELAY_INTERVAL;
			if (syncInterval > SYNC_MAX_INTERVAL) syncInterval = SYNC_MAX_INTERVAL;
			
			Log.d("FAIMS", "delaying sync to " + syncInterval);
			
			Thread.sleep(syncInterval * 1000);
			
		} catch (Exception e) {
			Log.e("FAIMS", "Error delaying sync", e);
		}
	}
	
	private void resetSync() {
		try {
			syncInterval = SYNC_MIN_INTERVAL;
			
			Log.d("FAIMS", "resetting sync to " + syncInterval);
			
			Thread.sleep(syncInterval * 1000);
			
		} catch (Exception e) {
			Log.e("FAIMS", "Error reseting sync", e);
		}
	}
	
	private void retrySync() {
		delaySync();
		locateServer();
	}

}
