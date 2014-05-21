package au.org.intersect.faims.android.services;

import android.content.Intent;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.util.TestModuleUtil;

public class TestDownloadModuleService extends DownloadModuleService {
	
	@Override
	public void onHandleIntent(Intent intent) {
		// create test module
		TestModuleUtil.createModule("Module 0", "abcdefg");
	}

	public void setFaimsClient(FAIMSClient faimsClient) {
		this.faimsClient = faimsClient;
	}

}
