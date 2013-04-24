package au.org.intersect.faims.android.services;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.util.FileUtil;

public class UploadDatabaseService extends UploadService {

	private File tempDB;
	
	public UploadDatabaseService() {
		super("UploadDatabaseService");
	}
	
	@Override
	public void onDestroy() {
		if (tempDB != null) {
			tempDB.delete();
		}
		super.onDestroy();
	}
	
	@Override
	protected Result doUpload(Intent intent) throws Exception {
		try {
			String userId = intent.getStringExtra("userId");
			Bundle extras = intent.getExtras();
			Project project = (Project) extras.get("project");
			String database = Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key + "/db.sqlite3";
			
			// create temp database to upload
			databaseManager.init(database);
			
			tempDB = File.createTempFile("temp_", ".sqlite3", new File(Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir));
	    	
	    	dumpDatabase(tempDB, project);
	    	
	    	// check if database is empty
	    	if (databaseManager.isEmpty(tempDB)) {
	    		FLog.d("database is empty");
	    		return Result.SUCCESS;
	    	}
	    	
	    	if (uploadStopped) {
	    		FLog.d("upload cancelled");
	    		return Result.INTERRUPTED;
	    	}
	    	
	    	// tar file
	    	tempFile = File.createTempFile("temp_", ".tar.gz", new File(Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir));
	    	
	    	os = FileUtil.createTarOutputStream(tempFile.getAbsolutePath());
	    	
	    	FileUtil.tarFile(tempDB.getAbsolutePath(), os);
	    	
	    	if (uploadStopped) {
	    		FLog.d("upload cancelled");
	    		return Result.INTERRUPTED;
	    	}
	    	
	    	// upload database
			return faimsClient.uploadDatabase(project, tempFile, userId);
		} finally {
			if (tempDB != null) tempDB.delete();

			if (tempFile != null) tempFile.delete();
			
			// TODO check if this is necessary as file util also closes the stream
			if (os != null) {
				try {
					os.close();
				} catch (Exception e) {
					FLog.e("error closing steam", e);
				}
			}
		}
	}

	protected void dumpDatabase(File tempFile, Project project) throws Exception {
    	databaseManager.dumpDatabaseTo(tempFile);
	}

}
