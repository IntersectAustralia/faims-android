package au.org.intersect.faims.android.services;

import java.io.File;

import au.org.intersect.faims.android.two.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.Request;
import au.org.intersect.faims.android.util.FileUtil;

public class UpdateModuleDataService extends DownloadUploadService {

	public UpdateModuleDataService() {
		super("UpdateModuleDataService");
	}
	
	@Override
	protected void performService() throws Exception {
		File dataDir = serviceModule.getDirectoryPath(this.getResources().getString(R.string.data_dir));
		if (overwrite) {
			FileUtil.delete(dataDir);
		}
		
		if (!downloadFiles("data", Request.DATA_FILES_INFO_REQUEST(serviceModule), 
				Request.DATA_FILE_DOWNLOAD_REQUEST(serviceModule), dataDir)) {
			FLog.d("Failed to download data files");
			return;
		}
	}
}
