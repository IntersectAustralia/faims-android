package au.org.intersect.faims.android.ui.activity.handlers;

import java.lang.ref.WeakReference;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;

import com.google.inject.Inject;

public class WifiBroadcastReceiver extends BroadcastReceiver {
	
	@Inject
	ServerDiscovery serverDiscovery;

	private WeakReference<ShowModuleActivity> activityRef;

	public WifiBroadcastReceiver(ShowModuleActivity activity) {
		this.activityRef = new WeakReference<ShowModuleActivity>(activity);
		FAIMSApplication.getInstance().injectMembers(this);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		ShowModuleActivity activity = this.activityRef.get();
		if (activity == null) {
			FLog.d("WifiBroadcastReceiver cannot get activity");
			return;
		}

		if (serverDiscovery.isServerHostFixed()) {
			FLog.d("Ignoring WifiBroadcastReceiver as server host is fixed");
			return;
		}

		final String action = intent.getAction();
		FLog.d("WifiBroadcastReceiver action " + action);

		if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
			if (intent.getBooleanExtra(
					WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
				activity.setWifiConnected(true);
				if (activity.getActivityData().isSyncEnabled()
						&& activity.isActivityShowing()
						&& !activity.isSyncActive()) {
					activity.startSync();
				}
			} else {
				activity.setWifiConnected(false);
				if (activity.isSyncActive()) {
					activity.stopSync();
				}
			}
		}
	}
}
