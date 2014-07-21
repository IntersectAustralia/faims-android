package au.org.intersect.faims.android.ui.activity.handlers;

import android.os.Message;
import au.org.intersect.faims.android.net.FAIMSClientErrorCode;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;

public class SyncDatabaseHandler extends ShowModuleActivityHandler {

	public SyncDatabaseHandler(ShowModuleActivity activity) {
		super(activity);
	}

	@Override
	public void handleMessageSafe(ShowModuleActivity activity,
			Message message) {
		Result result = (Result) message.obj;
		if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
			if (activity.getActivityData().isFileSyncEnabled()) {
				activity.startSyncingFiles();
			} else {
				activity.resetSyncInterval();
				activity.waitForNextSync();
				activity.callSyncSuccess();
				activity.releaseSyncLock();
			}
		} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
			if (result.errorCode == FAIMSClientErrorCode.BUSY_ERROR) {
				activity.resetSyncInterval();
				activity.waitForNextSync();
				activity.callSyncSuccess();
				activity.releaseSyncLock();
			} else {
				// failure
				activity.delaySyncInterval();
				activity.waitForNextSync();
				activity.callSyncFailure();
				activity.releaseSyncLock();
			}
		} else {
			// cancelled
			activity.releaseSyncLock();
		}
	}
}
