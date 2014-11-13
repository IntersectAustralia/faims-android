package au.org.intersect.faims.android.services;

import org.json.JSONObject;

import au.org.intersect.faims.android.two.R;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.Request;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ModuleUtil;

public class DownloadModuleService extends DownloadUploadService {

	public DownloadModuleService() {
		super("DownloadModuleService");
	}
	
	@Override
	protected void performService() throws Exception {
		// 0. delete module if it exists and we want to override everything
		if (overwrite) {
			FileUtil.delete(serviceModule.getDirectoryPath());
		}
		
		// 1. download settings (ui schema, ui logic, module settings, properties file(s))
		if (!downloadFiles("settings", Request.SETTINGS_INFO_REQUEST(serviceModule), 
				Request.SETTINGS_DOWNLOAD_REQUEST(serviceModule), 
				serviceModule.getDirectoryPath())) {
			FLog.d("Failed to download settings");
			return;
		}
		addHostToModuleSettings();
			
		JSONObject jsonInfo = (JSONObject) serviceResult.data;
		
		// 2. download database
		if (!downloadFiles("db", Request.DATABASE_INFO_REQUEST(serviceModule), 
				Request.DATABASE_DOWNLOAD_REQUEST(serviceModule), 
				serviceModule.getDirectoryPath())) {
			FLog.d("Failed to download database");
			return;
		}
		
		// 3. download data files
		if (!downloadFiles("data", Request.DATA_FILES_INFO_REQUEST(serviceModule), 
				Request.DATA_FILE_DOWNLOAD_REQUEST(serviceModule), 
				serviceModule.getDirectoryPath(this.getResources().getString(R.string.data_dir)))) {
			FLog.d("Failed to download data files");
			return;
		}
		
		// 4. download app files
		if (!downloadFiles("app", Request.APP_FILES_INFO_REQUEST(serviceModule), 
				Request.APP_FILE_DOWNLOAD_REQUEST(serviceModule), 
				serviceModule.getDirectoryPath(this.getResources().getString(R.string.app_dir)))) {
			FLog.d("Failed to download app files");
			return;
		}
		
		Module module = ModuleUtil.getModule(serviceModule.key); // get the latest settings
		module.dbVersion = jsonInfo.getString("dbVersion");
		module.timestamp = DateUtil.getCurrentTimestampGMT();
		module.fileSyncTimeStamp = DateUtil.getCurrentTimestampGMT();
		ModuleUtil.saveModule(module);
	}

}
