package au.org.intersect.faims.android.services;

import org.json.JSONObject;

import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.Request;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.ModuleUtil;

public class DownloadDatabaseService extends DownloadUploadService {

	public DownloadDatabaseService() {
		super("DownloadDatabaseService");
	}
	
	@Override
	protected void performService() throws Exception {
		if (!downloadFiles("db", Request.DATABASE_INFO_REQUEST(serviceModule), 
				Request.DATABASE_DOWNLOAD_REQUEST(serviceModule), 
				serviceModule.getDirectoryPath())) {
			FLog.d("Failed to download database");
			return;
		}
		
		Module module = ModuleUtil.getModule(serviceModule.key); // get the latest settings
		JSONObject jsonInfo = (JSONObject) serviceResult.data;
		module.dbVersion = jsonInfo.getString("dbVersion");
		module.timestamp = DateUtil.getCurrentTimestampGMT(); // note: updating timestamp as database is overwritten
		ModuleUtil.saveModule(module);
	}

}
