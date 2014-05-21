package au.org.intersect.faims.android.services;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;

import android.content.Intent;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.Request;
import au.org.intersect.faims.android.util.FileUtil;

import com.google.inject.Inject;

public class UploadDatabaseService extends DownloadUploadService {

	@Inject
	DatabaseManager databaseManager;
	
	protected String userId;
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
		databaseManager.interrupt();
		super.onDestroy();
	}

	protected void initService(Intent intent) {
    	super.initService(intent);
    	userId = intent.getStringExtra("userId");
    	databaseManager.init(serviceModule.getDirectoryPath("db.sqlite").getPath());
	}

	@Override
	protected void performService() throws Exception {
		uploadDatabase();
	}
	
	private void uploadDatabase() throws Exception {
		tempDB = File.createTempFile("temp_", ".sqlite", serviceModule.getDirectoryPath());
    	
		databaseManager.dumpDatabaseTo(tempDB);
		
    	// check if database is empty
    	if (databaseManager.isEmpty(tempDB)) {
    		FLog.d("database is empty");
    		return;
    	}
    	
    	ArrayList<File> files = new ArrayList<File>();
    	files.add(tempDB);
    	
		HashMap<String, ContentBody> extraParts = new HashMap<String, ContentBody>();
		extraParts.put("user", new StringBody(userId));
		
    	if (!uploadFiles("db", 
    			Request.DATABASE_UPLOAD_REQUEST(serviceModule), files, serviceModule.getDirectoryPath(), extraParts)) {
    		FLog.d("Failed to upload database");
			return;
    	}
	}

}
