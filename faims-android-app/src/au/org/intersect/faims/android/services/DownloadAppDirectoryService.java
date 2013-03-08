package au.org.intersect.faims.android.services;

import au.org.intersect.faims.android.R;

public class DownloadAppDirectoryService extends DownloadDirectoryService {

	public DownloadAppDirectoryService() {
		super("DownloadAppDirectoryService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.downloadDir = this.getResources().getString(R.string.app_dir);
		this.requestExcludePath = "app_file_list";
		this.infoPath = "app_file_archive";
		this.downloadPath = "app_file_download";
	}
}
