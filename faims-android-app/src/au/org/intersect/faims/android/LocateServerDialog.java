package au.org.intersect.faims.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import au.org.intersect.faims.android.net.ServerDiscovery;

public class LocateServerDialog extends ProgressDialog implements ServerDiscovery.ServerDiscoveryListener {
	
	public static final int SUCCESS = 0;
	public static final int FAILURE = 1;
	
	public static final String TYPE = "LOCATE_SERVER_DIALOG";
	
	public static final int SERVER_BROADCAST_ATTEMPTS = 5;
	
	private int attempts = 0;
	
	private IFAIMSDialogListener listener;
	
	public LocateServerDialog(Activity activity) {
		super(activity);
		setTitle(activity.getString(R.string.locate_server_dialog_title));
    	setMessage(activity.getString(R.string.locate_server_dialog_message));
    	setIcon(R.drawable.ic_launcher);
    	setIndeterminate(true);
    	setCancelable(false);
    	
    	this.listener = (IFAIMSDialogListener) activity;
	}
	
	@Override 
	public void show() {
		super.show();

		ServerDiscovery.getInstance().findServer(this, attempts);
	}
	
	@Override
	public void handleDiscoveryResponse(boolean success) {
		if (success) {
			listener.handleDialogResponse(LocateServerDialog.SUCCESS, LocateServerDialog.TYPE, this);
		} else {
			attempts++;
			Log.d("debug", "LocateServerDialog.attempt :" + String.valueOf(attempts));
			if (attempts < SERVER_BROADCAST_ATTEMPTS) {
				ServerDiscovery.getInstance().findServer(this, attempts);
			} else {
				listener.handleDialogResponse(LocateServerDialog.FAILURE, LocateServerDialog.TYPE, this);
			}
		}
	}
	
	public static LocateServerDialog create(Activity activity) {
		return new LocateServerDialog(activity);
	}
	
}

