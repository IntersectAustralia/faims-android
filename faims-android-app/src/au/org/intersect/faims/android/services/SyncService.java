package au.org.intersect.faims.android.services;

import android.app.IntentService;
import android.content.Intent;

public class SyncService extends IntentService {

	public SyncService() {
		super("SyncService");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		
		// check if service server is available
		
		// start upload sync service
		
	}

}
