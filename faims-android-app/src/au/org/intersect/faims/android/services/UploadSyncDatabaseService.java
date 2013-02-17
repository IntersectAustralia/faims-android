package au.org.intersect.faims.android.services;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.FileUtil;

public class UploadSyncDatabaseService extends UploadService {
	
	public UploadSyncDatabaseService() {
		super("UploadSyncDatabaseService");
	}
	
	@Override
	protected FAIMSClientResultCode doUpload(Intent intent) throws Exception {
		File tempFile = null;
		
		try {
			String database = intent.getStringExtra("database");
			String userId = intent.getStringExtra("userId");
			Bundle extras = intent.getExtras();
			Project project = (Project) extras.get("project");
			
			// create temp database to upload
			databaseManager.init(database);
			
			File outputDir = new File(Environment.getExternalStorageDirectory() + "/faims/projects/" + project.dir);
			
	    	tempFile = File.createTempFile("temp_", ".sqlite3", outputDir);
	    	
	    	if (project.timestamp != null) {
	    		databaseManager.dumpDatabaseTo(tempFile, project.timestamp);
	    	} else {
	    		databaseManager.dumpDatabaseTo(tempFile);
	    	}
	    	
	    	if (uploadStopped) {
	    		Log.d("FAIMS", "cancelled upload");
	    		return null; // note: this doesn't matter as upload is cancelled
	    	}
	    	
	    	// tar file
	    	file = File.createTempFile("temp_", ".tar.gz", outputDir);
	    	FileUtil.tarFile(tempFile.getAbsolutePath(), file.getAbsolutePath());
	    	
	    	if (uploadStopped) {
	    		Log.d("FAIMS", "cancelled upload");
	    		return null;
	    	}
	    	
	    	// upload database
			return faimsClient.uploadDatabase(project, file, userId);
			
		} finally {
			if (tempFile != null) tempFile.delete();
		}
	}

}
