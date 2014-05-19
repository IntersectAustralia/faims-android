package au.org.intersect.faims.android.services;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONObject;

import android.os.Environment;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.Request;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ModuleUtil;

public class SyncDatabaseService extends UploadDatabaseService {

	private String dumpTimestamp;
	private File tempDir;

	public SyncDatabaseService() {
		super("SyncDatabaseService");
	}
	
	@Override
	public void onDestroy() {
		if (tempDir != null) {
			FileUtil.delete(tempDir);
		}
		super.onDestroy();
	}
	
	@Override
	protected void performService() throws Exception {
		uploadDatabase();
		downloadDatabase();
	}
	
	protected void uploadDatabase() throws Exception {
		tempDB = File.createTempFile("temp_", ".sqlite", serviceModule.getDirectoryPath());
    	
		dumpTimestamp = DateUtil.getCurrentTimestampGMT();
		if (serviceModule.timestamp == null) {
			databaseManager.dumpDatabaseTo(tempDB);
		} else {
			databaseManager.dumpDatabaseTo(tempDB, serviceModule.timestamp); 
		}
		
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
    			Request.DATABASE_UPLOAD_REQUEST(serviceModule), files, serviceModule.getDirectoryPath(), extraParts)){
    		FLog.d("Failed to upload database");
			return;
    	}    	
    	
    	Module module = ModuleUtil.getModule(serviceModule.key); // get the latest settings
		module.timestamp = dumpTimestamp;
		ModuleUtil.saveModule(module);
	}

	private void downloadDatabase() throws Exception {		
		// check if there is a new version to download
		Result infoResult = faimsClient.fetchRequestObject(Request.DATABASE_INFO_REQUEST(serviceModule));		
		if (infoResult.resultCode != FAIMSClientResultCode.SUCCESS) {
			serviceResult = infoResult;
			return;
		} 

		JSONObject jsonInfo = (JSONObject) infoResult.data;		
		int serverVersion = Integer.parseInt(jsonInfo.optString("version") == null ? "0" : jsonInfo.optString("version"));
		int moduleVersion = Integer.parseInt(serviceModule.version == null ? "0" : serviceModule.version);
		if (serverVersion == moduleVersion) {
			FLog.d("database is up to date");
			serviceResult = Result.SUCCESS;
			return;
		}
		
		// download database changes
		int syncVersion = moduleVersion + 1;
		
		tempDir = new File(Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir + "temp_" + UUID.randomUUID());
		tempDir.mkdirs();
		
		if (!downloadFiles("db", 
				Request.DATABASE_INFO_REQUEST(serviceModule, syncVersion), 
				Request.DATABASE_DOWNLOAD_REQUEST(serviceModule, syncVersion), tempDir)) {
			FLog.d("Failed to download database");
			return;
		}
		
		// merge database 
		databaseManager.mergeDatabaseFrom(new File(tempDir.getAbsoluteFile() + "/db.sqlite"));
		
		// update settings
		Module module = ModuleUtil.getModule(serviceModule.key); // get the latest settings
		jsonInfo = (JSONObject) serviceResult.data;
		module.version = jsonInfo.getString("version");
		ModuleUtil.saveModule(module);
	}

}
