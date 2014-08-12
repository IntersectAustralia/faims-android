package au.org.intersect.faims.android.services;

import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.Request;

public class UpdateModuleSettingService extends DownloadUploadService {

	public UpdateModuleSettingService() {
		super("UpdateModuleSettingService");
	}
	
	@Override
	protected void performService() throws Exception {
		if (!downloadFiles("settings", Request.SETTINGS_INFO_REQUEST(serviceModule), 
				Request.SETTINGS_DOWNLOAD_REQUEST(serviceModule), 
				serviceModule.getDirectoryPath())) {
			FLog.d("Failed to download settings");
			return;
		}
	}

}
