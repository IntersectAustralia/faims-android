package au.org.intersect.faims.android.ui.activity.handlers;

import android.os.Message;
import au.org.intersect.faims.android.net.FAIMSClientErrorCode;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;

public class SyncFilesHandler extends ShowModuleActivityHandler {

	public SyncFilesHandler(ShowModuleActivity activity) {
		super(activity);
	}

	@Override
	public void handleMessageSafe(ShowModuleActivity activity,
			Message message) {
		Result result = (Result) message.obj;
		if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
			activity.resetSyncInterval();
			activity.waitForNextSync();
			activity.callSyncSuccess(ShowModuleActivity.FILES);
		} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
			if (result.errorCode == FAIMSClientErrorCode.BUSY_ERROR) {
				activity.resetSyncInterval();
				activity.waitForNextSync();
				activity.callSyncSuccess(ShowModuleActivity.FILES);
			} else {
				// failure
				activity.delaySyncInterval();
				activity.waitForNextSync();
				activity.callSyncFailure();
			}
		} else {
			// cancelled
		}
		activity.releaseSyncLock();
	}

}
