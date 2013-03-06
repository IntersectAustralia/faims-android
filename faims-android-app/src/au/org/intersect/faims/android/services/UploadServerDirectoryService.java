package au.org.intersect.faims.android.services;

import android.content.Intent;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

public class UploadServerDirectoryService extends UploadService {

	public UploadServerDirectoryService() {
		super("UploadServerDirectoryService");
	}

	@Override
	protected FAIMSClientResultCode doUpload(Intent intent) throws Exception {
		try {
			Project project = (Project) intent.getExtras().get("project");
			Log.d("FAIMS", "uploading server directory for project " + project.name);
			
			return faimsClient.uploadServerDirectory(project);
		} catch (Exception e) {
			Log.e("FAIMS", "could not upload server directory for project", e);
		}
		return null;
	}

}
