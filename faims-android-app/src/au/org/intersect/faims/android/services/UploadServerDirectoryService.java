package au.org.intersect.faims.android.services;

import au.org.intersect.faims.android.R;


public class UploadServerDirectoryService extends UploadDirectoryService {

	public UploadServerDirectoryService() {
		super("UploadServerDirectoryService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.uploadDir = this.getResources().getString(R.string.server_dir);
		this.requestExcludePath = "server_file_list";
		this.uploadPath = "server_upload";
	}

}
