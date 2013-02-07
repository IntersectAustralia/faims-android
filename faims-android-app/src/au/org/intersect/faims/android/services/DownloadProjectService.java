package au.org.intersect.faims.android.services;

import android.content.Intent;
import android.util.Log;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;

public class DownloadProjectService extends DownloadService {

	public DownloadProjectService() {
		super("DownloadProjectService");
	}
	
	@Override
	protected FAIMSClientResultCode doDownload(Intent intent) {
		try {
			Project project = (Project) intent.getExtras().get("project");
			Log.d("FAIMS", "downloading project " + project.name);
			return faimsClient.downloadProject(project);
		} catch (Exception e) {
			Log.d("FAIMS", "could not download project");
		}
		return null;
	}

}
