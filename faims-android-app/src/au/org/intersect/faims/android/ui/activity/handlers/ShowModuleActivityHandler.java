package au.org.intersect.faims.android.ui.activity.handlers;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;

public abstract class ShowModuleActivityHandler extends Handler {

	private WeakReference<ShowModuleActivity> activityRef;

	public ShowModuleActivityHandler(ShowModuleActivity activity) {
		this.activityRef = new WeakReference<ShowModuleActivity>(activity);
	}

	public void handleMessage(Message message) {
		ShowModuleActivity activity = activityRef.get();
		if (activity == null) {
			FLog.d("ShowModuleActivityHandler cannot get activity");
			return;
		}

		handleMessageSafe(activity, message);
	}

	public abstract void handleMessageSafe(ShowModuleActivity activity,
			Message message);

}
