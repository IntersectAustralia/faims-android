package au.org.intersect.faims.android.services;

import au.org.intersect.faims.android.R;

public class UploadAppDirectoryService extends UploadDirectoryService {

	public UploadAppDirectoryService() {
		super("UploadAppDirectoryService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.uploadDir = this.getResources().getString(R.string.app_dir);
		this.requestExcludePath = "app_file_list";
		this.uploadPath = "app_file_upload";
	}

}
