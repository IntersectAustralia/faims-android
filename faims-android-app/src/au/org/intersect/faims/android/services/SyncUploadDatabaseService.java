package au.org.intersect.faims.android.services;

import java.io.File;

import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.ProjectUtil;

public class SyncUploadDatabaseService extends UploadDatabaseService {
	
	private String dumpTimestamp;
	
	public SyncUploadDatabaseService() {
		super("SyncUploadDatabaseService");
	}
	
	protected void dumpDatabase(File tempFile, Project project) throws Exception {
		dumpTimestamp = DateUtil.getCurrentTimestampGMT();
		if (project.timestamp == null) {
			databaseManager.dumpDatabaseTo(tempFile);
		} else {
			Log.d("FAIMS", "dumping database from " + project.timestamp);
			databaseManager.dumpDatabaseTo(tempFile, project.timestamp); 
		}
	}
	
	@Override
	protected void doComplete(FAIMSClientResultCode resultCode, Project project) {
		if (resultCode == FAIMSClientResultCode.SUCCESS) {
			project = ProjectUtil.getProject(project.key); // get the latest settings
			project.timestamp = dumpTimestamp;
			ProjectUtil.saveProject(project);
		}
	}
	

}
