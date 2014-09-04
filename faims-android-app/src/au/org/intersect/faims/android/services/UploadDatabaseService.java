package au.org.intersect.faims.android.services;

import java.io.File;
import java.util.HashMap;

import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;

import android.content.Intent;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.Request;
import au.org.intersect.faims.android.util.FileUtil;

public class UploadDatabaseService extends DownloadUploadService {
	
	protected File tempDB;
	
	public UploadDatabaseService() {
		super("UploadDatabaseService");
	}
	
	public UploadDatabaseService(String name) {
		super(name);
	}
	
	@Override
	public void onDestroy() {
		if (tempDB != null) {
			FileUtil.delete(tempDB);
		}
		super.onDestroy();
	}

	protected void initService(Intent intent) {
    	super.initService(intent);
	}

	@Override
	protected void performService() throws Exception {
		uploadDatabase();
	}
	
	private void uploadDatabase() throws Exception {
		tempDB = File.createTempFile("temp_", ".sqlite", serviceModule.getDirectoryPath());
    	
		databaseManager.mergeRecord().dumpDatabaseTo(tempDB);
		
    	// check if database is empty
    	if (databaseManager.mergeRecord().isEmpty(tempDB)) {
    		FLog.d("database is empty");
    		return;
    	}
    	
		HashMap<String, ContentBody> extraParts = new HashMap<String, ContentBody>();
		extraParts.put("user", new StringBody(databaseManager.getUserId()));
		
    	if (!uploadFile("db", 
    			Request.DATABASE_UPLOAD_REQUEST(serviceModule), tempDB, serviceModule.getDirectoryPath(), extraParts)) {
    		FLog.d("Failed to upload database");
			return;
    	}
	}

}
