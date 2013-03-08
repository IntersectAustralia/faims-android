package au.org.intersect.faims.android.services;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.FileUtil;

public class UploadDatabaseService extends UploadService {

	public UploadDatabaseService() {
		super("UploadDatabaseService");
	}
	
	public UploadDatabaseService(String name) {
		super(name);
	}
	
	@Override
	protected FAIMSClientResultCode doUpload(Intent intent) throws Exception {
		File tempFile = null;
		
		try {
			String userId = intent.getStringExtra("userId");
			Bundle extras = intent.getExtras();
			Project project = (Project) extras.get("project");
			String database = Environment.getExternalStorageDirectory() + "/faims/projects/" + project.key + "/db.sqlite3";
			
			// create temp database to upload
			databaseManager.init(database);
			
			File outputDir = new File(Environment.getExternalStorageDirectory() + "/faims/projects/" + project.key);
			
	    	tempFile = File.createTempFile("temp_", ".sqlite3", outputDir);
	    	
	    	dumpDatabase(tempFile, project);
	    	
	    	// check if database is empty
	    	if (databaseManager.isEmpty(tempFile)) {
	    		Log.d("FAIMS", "database is empty");
	    		return FAIMSClientResultCode.SUCCESS;
	    	}
	    	
	    	if (uploadStopped) {
	    		Log.d("FAIMS", "upload cancelled");
	    		return null; 
	    	}
	    	
	    	// tar file
	    	file = File.createTempFile("temp_", ".tar.gz", outputDir);
	    	FileUtil.tarFile(tempFile.getAbsolutePath(), file.getAbsolutePath());
	    	
	    	if (uploadStopped) {
	    		Log.d("FAIMS", "upload cancelled");
	    		return null;
	    	}
	    	
	    	// upload database
			return faimsClient.uploadDatabase(project, file, userId);
			
		} finally {
			if (tempFile != null) tempFile.delete();
		}
	}

	protected void dumpDatabase(File tempFile, Project project) throws Exception {
    	databaseManager.dumpDatabaseTo(tempFile);
	}

}
