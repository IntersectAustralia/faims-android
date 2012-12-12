package au.org.intersect.faims.android;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.util.Log;
import au.org.intersect.faims.android.net.DiscoveryServer;

public class LocateServerDialog extends ProgressDialog implements DiscoveryServer.ServerFoundHandler {
	
	public interface LocateServerDialogListener {
		
		public void onServerLocated(boolean success, Dialog dialog);
		
	}
	
	public static final int SERVER_BROADCAST_ATTEMPTS = 5;
	
	private int attempts = 0;
	
	private LocateServerDialogListener listener;
	
	public LocateServerDialog(Activity activity, LocateServerDialogListener listener) {
		super(activity);
		setTitle(activity.getString(R.string.locate_server_dialog_title));
    	setMessage(activity.getString(R.string.locate_server_dialog_message));
    	setIcon(R.drawable.ic_launcher);
    	setIndeterminate(true);
    	setCancelable(false);
    	
    	this.listener = listener;
	}
	
	@Override 
	public void show() {
		super.show();

		DiscoveryServer.getInstance().findServer(this);
	}
	
	@Override
	public void handleServerFound(boolean success) {
		if (success) {
			listener.onServerLocated(true, LocateServerDialog.this);
		} else {
			attempts++;
			Log.d("debug", "LocateServerDialog.attempt :" + String.valueOf(attempts));
			if (attempts < SERVER_BROADCAST_ATTEMPTS) {
				DiscoveryServer.getInstance().findServer(this);
			} else {
				listener.onServerLocated(false, LocateServerDialog.this);
			}
		}
	}
	
	
}

