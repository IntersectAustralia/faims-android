package au.org.intersect.faims.android.services;

import java.io.File;

import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.Request;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ModuleUtil;

public class SyncFilesService extends DownloadUploadService {

	public SyncFilesService() {
		super("SyncFilesService");
	}

	@Override
	protected void performService() throws Exception {
		File serverDirectory = serviceModule.getDirectoryPath(this.getResources().getString(R.string.server_dir));
		File appDirectory = serviceModule.getDirectoryPath(this.getResources().getString(R.string.app_dir));
		// upload server directory
		if (!uploadFiles("server", 
    			Request.SERVER_FILE_UPLOAD_REQUEST(serviceModule), 
    			FileUtil.listDirectory(serverDirectory),
    			serverDirectory,
    			null,
    			Request.SERVER_FILES_INFO_REQUEST(serviceModule))) {
			FLog.d("Failed to upload server files");
			return;
		}
		
		// upload app directory
		if (!uploadFiles("app", 
    			Request.APP_FILE_UPLOAD_REQUEST(serviceModule), 
    			FileUtil.listDirectory(appDirectory),
    			appDirectory,
    			null,
    			Request.APP_FILES_INFO_REQUEST(serviceModule))) {
			FLog.d("Failed to upload app files");
			return;
		}
		
		// download app directory
		if (!downloadFiles("app", 
				Request.APP_FILES_INFO_REQUEST(serviceModule), 
				Request.APP_FILE_DOWNLOAD_REQUEST(serviceModule), 
				appDirectory, 
				false)) {
			FLog.d("Failed to download app files");
			return;
		}
		
		Module module = ModuleUtil.getModule(serviceModule.key); // get the latest settings
		module.fileSyncTimeStamp = DateUtil.getCurrentTimestampGMT();
		ModuleUtil.saveModule(module); 
	}
	
}
