package au.org.intersect.faims.android.services;

import android.content.Intent;
import au.org.intersect.faims.android.net.FAIMSClient;

public class TestDownloadModuleService extends DownloadModuleService {
	
	@Override
	public void onHandleIntent(Intent intent) {
		super.onHandleIntent(intent);
	}

	public void setFaimsClient(FAIMSClient faimsClient) {
		this.faimsClient = faimsClient;
	}

}
