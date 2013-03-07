package au.org.intersect.faims.android.services;

import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

public class UploadDirectoryService extends UploadService {

	protected String uploadDir;
	protected String requestExcludePath;
	protected String uploadPath;

	public UploadDirectoryService(String name) {
		super(name);
	}

	@Override
	protected FAIMSClientResultCode doUpload(Intent intent) throws Exception {
		try {
			Project project = (Project) intent.getExtras().get("project");
			Log.d("FAIMS", "uploading directory for project " + project.name);
			
			String projectDir = Environment.getExternalStorageDirectory() + "/faims/projects/" + project.key;
			return faimsClient.uploadDirectory(projectDir, uploadDir, "/android/project/" + project.key + "/" + requestExcludePath, "/android/project/" + project.key + "/" + uploadPath);
		} catch (Exception e) {
			Log.e("FAIMS", "could not upload directory for project", e);
		}
		return null;
	}

}
