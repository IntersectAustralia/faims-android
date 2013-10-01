package au.org.intersect.faims.android.services;

import android.content.Intent;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.DownloadResult;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.ModuleUtil;

public class DownloadDatabaseService extends DownloadService {

	public DownloadDatabaseService() {
		super("DownloadDatabaseService");
	}
	
	@Override
	protected DownloadResult doDownload(Intent intent) {
		try {
			Module module = (Module) intent.getExtras().get("module");
			
			FLog.d("downloading database for " + module.name);
			
			DownloadResult result = faimsClient.downloadDatabase(module);
			
			// if result is success then update the module settings with version and timestamp
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				module = ModuleUtil.getModule(module.key); // get the latest settings
				module.version = result.info.version;
				module.timestamp = DateUtil.getCurrentTimestampGMT(); // note: updating timestamp as database is overwritten
				ModuleUtil.saveModule(module);
			}
			
			return result;
		} catch (Exception e) {
			FLog.e("error downloading database", e);
		}
		return null;
	}

}
