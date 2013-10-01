package au.org.intersect.faims.android.services;

import java.io.File;

import android.content.Intent;
import android.os.Environment;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.DownloadResult;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ModuleUtil;

public class DownloadModuleService extends DownloadService {

	public DownloadModuleService() {
		super("DownloadModuleService");
	}
	
	@Override
	protected DownloadResult doDownload(Intent intent) {
		File moduleDir = null;
		try {
			Module module = (Module) intent.getExtras().get("module");
			
			FLog.d("downloading module " + module.name);
			
			// 0. delete module if it exists
			// 1. download settings (ui schema, ui logic, module settings, properties file(s))
			// 2. download database
			// 3. download data files
			// 4. download app files
			
			// 0.
			
			moduleDir = new File(Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir + module.key);
			FileUtil.delete(moduleDir);
			
			// 1.
			DownloadResult result = faimsClient.downloadSettings(module);
			
			if (downloadStopped) {
				FileUtil.delete(moduleDir);
				
				FLog.d("download cancelled");
				return DownloadResult.INTERRUPTED;
			}
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				FileUtil.delete(moduleDir);
				
				FLog.d("download settings failure");
				return result;
			}
			
			String version = result.info.version;
			
			// 2.
			result = faimsClient.downloadDatabase(module);
			
			if (downloadStopped) {
				FileUtil.delete(moduleDir);
				
				FLog.d("download cancelled");
				return DownloadResult.INTERRUPTED;
			}
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				FileUtil.delete(moduleDir);
				
				FLog.d("download database failure");
				return result;
			}
			
			// 3.
			result = downloadDataDirectory(intent);
			
			if (downloadStopped) {
				FileUtil.delete(moduleDir);
				
				FLog.d("download cancelled");
				return DownloadResult.INTERRUPTED;
			}
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				FileUtil.delete(moduleDir);
				
				FLog.d("download data directory failure");
				return result;
			}
			
			// 4.
			result = downloadAppDirectory(intent);
			
			if (downloadStopped) {
				FileUtil.delete(moduleDir);
				
				FLog.d("download cancelled");
				return DownloadResult.INTERRUPTED;
			}
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				FileUtil.delete(moduleDir);
				
				FLog.d("download app directory failure");
				return result;
			}
			
			// if result is success then update the module settings with version and timestamp
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				module = ModuleUtil.getModule(module.key); // get the latest settings
				module.version = version;
				module.timestamp = DateUtil.getCurrentTimestampGMT(); // note: updating timestamp as database is overwritten
				ModuleUtil.saveModule(module);
			}
			
			return result;
		} catch (Exception e) {
			if (moduleDir != null) {
				FileUtil.delete(moduleDir);
			}
			
			FLog.e("error downloading module", e);
		}
		return null;
	}
	
	private DownloadResult downloadAppDirectory(Intent intent) {
		FLog.d("downloading app directory");
		return downloadDirectory(intent,
				this.getResources().getString(R.string.app_dir),
				"app_file_list",
				"app_file_archive",
				"app_file_download");
	}
	
	private DownloadResult downloadDataDirectory(Intent intent) {
		FLog.d("downloading data directory");
		return downloadDirectory(intent,
				this.getResources().getString(R.string.data_dir),
				"data_file_list",
				"data_file_archive",
				"data_file_download");
	}
	
	private DownloadResult downloadDirectory(Intent intent, String downloadDir, String requestExcludePath, String infoPath, String downloadPath) {
		try {
			Module module = (Module) intent.getExtras().get("module");
			String moduleDir = Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir + module.key;
			
			DownloadResult downloadResult = faimsClient.downloadDirectory(moduleDir, downloadDir, 
					"/android/module/" + module.key + "/" + requestExcludePath, 
					"/android/module/" + module.key + "/" + infoPath,
					"/android/module/" + module.key + "/" + downloadPath);
		
			if (downloadResult.resultCode == FAIMSClientResultCode.FAILURE) {
				faimsClient.invalidate();
				FLog.d("download failure");
				return downloadResult;
			}
			
			FLog.d("downloading dir " + downloadDir + " success");
			return downloadResult;
		} catch (Exception e) {
			FLog.e("downloading dir " + downloadDir + " error");
			return DownloadResult.FAILURE;
		} finally {
			
		}
	}

}
