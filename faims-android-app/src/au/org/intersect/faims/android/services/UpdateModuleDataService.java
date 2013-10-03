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
import au.org.intersect.faims.android.util.FileUtil;

public class UpdateModuleDataService extends DownloadService {

	public UpdateModuleDataService() {
		super("UpdateModuleDataService");
	}

	@Override
	protected DownloadResult doDownload(Intent intent) {
		try {
			Module module = (Module) intent.getExtras().get("module");
			String moduleDir = Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir + module.key;
			
			FLog.d("update module data for module " + module.name);
			
			// 0. delete data directory
			// 1. download module data
			
			// 0.
			File dataDir = new File(moduleDir + "/" + this.getResources().getString(R.string.data_dir));
			FileUtil.delete(dataDir);
			dataDir.mkdirs();
			
			// 1.
			DownloadResult result = downloadDataDirectory(intent);
			
			if (downloadStopped) {
				FileUtil.delete(dataDir);
				dataDir.mkdirs();
				FLog.d("update module data cancelled");
				return DownloadResult.INTERRUPTED;
			}
			
			if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				FileUtil.delete(dataDir);
				dataDir.mkdirs();
				FLog.d("update module data directory failure");
				return result;
			}
			
			return result;
		} catch (Exception e) {
			
			FLog.e("error updating module", e);
		}
		return null;
	}

	private DownloadResult downloadDataDirectory(Intent intent) {
		FLog.d("updating data directory");
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
