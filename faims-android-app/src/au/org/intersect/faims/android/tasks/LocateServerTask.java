package au.org.intersect.faims.android.tasks;

import roboguice.util.RoboAsyncTask;
import android.app.Activity;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.net.ServerDiscovery.ServerDiscoveryListener;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.IFAIMSDialogListener;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;

import com.google.inject.Inject;

public class LocateServerTask extends RoboAsyncTask<Void> implements ServerDiscoveryListener {
	
	@Inject
	ServerDiscovery serverDiscovery;
	
	private Activity activity;
	private BusyDialog dialog;
	private boolean searching;
	private TaskType taskType;
	
	public LocateServerTask(Activity activity, TaskType taskType) {
		super(activity);
		this.activity = activity;
		this.taskType = taskType;
	}
	
	@Override 
	protected void onPreExecute() {
		FAIMSLog.log();
		
		dialog = DialogFactory.createBusyDialog(activity, 
				ActionType.LOCATE_SERVER, 
				activity.getString(R.string.locate_server_title), 
				activity.getString(R.string.locate_server_message));
		dialog.show();
	}
	
	@Override
	public Void call() throws Exception {
		FAIMSLog.log();
		
		try {
			searching = true;
			
			serverDiscovery.startDiscovery(this);
			
			// wait for search to finish
			while(searching) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			FAIMSLog.log(e);
		}
		
		return null;
	}
	
	@Override
	protected void onInterrupted(Exception e) {
		FAIMSLog.log();
		
		// cleanup to avoid memory leaks
		dialog.cleanup();
		activity = null;
		
		serverDiscovery.stopDiscovery();
		
		searching = false;
	}
	
	@Override
	protected void onSuccess(Void v) {
		FAIMSLog.log();
		
		IFAIMSDialogListener listener = (IFAIMSDialogListener) activity;
		listener.handleDialogResponse(
				serverDiscovery.isServerHostValid() ? 
						ActionResultCode.SUCCESS : ActionResultCode.FAILURE, 
				taskType, 
				ActionType.LOCATE_SERVER, 
				dialog);
	}
	
	@Override
	public void handleDiscoveryResponse(boolean success) {
		FAIMSLog.log();
		
		searching = false;
	}
	
}
