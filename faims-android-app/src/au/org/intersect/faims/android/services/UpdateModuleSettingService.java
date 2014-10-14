package au.org.intersect.faims.android.services;

import java.io.File;
import java.util.Arrays;

import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.Request;
import au.org.intersect.faims.android.util.FileUtil;

public class UpdateModuleSettingService extends DownloadUploadService {

	public UpdateModuleSettingService() {
		super("UpdateModuleSettingService");
	}
	
	@Override
	protected void performService() throws Exception {
		if (overwrite) {
			deleteSettingsFiles();
		}

		if (!downloadFiles("settings", Request.SETTINGS_INFO_REQUEST(serviceModule), 
				Request.SETTINGS_DOWNLOAD_REQUEST(serviceModule), 
				serviceModule.getDirectoryPath())) {
			FLog.d("Failed to download settings");
			return;
		}
		addHostToModuleSettings();
	}

	private void deleteSettingsFiles() {
		for (String filename : Arrays.asList("ui_schema.xml", "ui_logic.bsh",
				"module.settings", "style.css")) {
			File file = serviceModule.getDirectoryPath(filename);
			if (file.exists()) {
				FileUtil.delete(file);
			}
		}
		// Find all arch16n files
		for (String filename : serviceModule.getArch16nFiles()) {
			File file = serviceModule.getDirectoryPath(filename);
			if (file.exists()) {
				FileUtil.delete(file);
			}
		}
		
	}

}
