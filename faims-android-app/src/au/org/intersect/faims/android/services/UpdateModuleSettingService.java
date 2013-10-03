package au.org.intersect.faims.android.services;

import android.content.Intent;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.DownloadResult;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.ModuleUtil;

public class UpdateModuleSettingService extends DownloadService {

	public UpdateModuleSettingService() {
		super("UpdateModuleSettingService");
	}

	@Override
	protected DownloadResult doDownload(Intent intent) {
		try {
			Module module = (Module) intent.getExtras().get("module");
			
			FLog.d("update module setting for module " + module.name);
			
			// 1. download settings (ui schema, ui logic, module settings, properties file(s))
			
			// 1.
			DownloadResult result = faimsClient.downloadSettings(module);
			
			if (downloadStopped) {
				FLog.d("update module setting cancelled");
				return DownloadResult.INTERRUPTED;
			}
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				FLog.d("update module setting failure");
				return result;
			}
			
			String version = result.info.version;
			
			// if result is success then update the module settings with version and timestamp
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				module = ModuleUtil.getModule(module.key); // get the latest settings
				module.version = version;
				ModuleUtil.saveModule(module);
			}
			
			return result;
		} catch (Exception e) {
			
			FLog.e("error updating module", e);
		}
		return null;
	}

}
