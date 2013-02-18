package au.org.intersect.faims.android.services;

import java.io.File;

import android.util.Log;
import au.org.intersect.faims.android.data.Project;

public class SyncUploadDatabaseService extends UploadDatabaseService {
	
	public SyncUploadDatabaseService() {
		super("SyncUploadDatabaseService");
	}
	
	protected void dumpDatabase(File tempFile, Project project) throws Exception {
		if (project.timestamp == null) {
			databaseManager.dumpDatabaseTo(tempFile);
		} else {
			Log.d("FAIMS", "Dumping database from " + project.timestamp);
			databaseManager.dumpDatabaseTo(tempFile, project.timestamp); 
		}
	}
	

}
