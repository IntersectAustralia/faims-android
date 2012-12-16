package au.org.intersect.faims.android.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.IFAIMSDialogListener;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;

public class LocateServerTask extends AsyncTask<Void, Void, Void> implements ServerDiscovery.ServerDiscoveryListener {
	
	private Activity activity;
	private BusyDialog dialog;
	private boolean searching;
	private TaskType taskType;
	
	public LocateServerTask(Activity activity, TaskType taskType) {
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
	protected Void doInBackground (Void... values) {
		FAIMSLog.log();
		
		try {
			searching = true;
			
			ServerDiscovery.getInstance().startDiscovery(this);
			
			// wait for search to finish
			while(searching && !isCancelled()) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			FAIMSLog.log(e);
		}
		
		return null;
	}
	
	@Override
	protected void onCancelled() {
		FAIMSLog.log();
		
		// cleanup to avoid memory leaks
		dialog.cleanup();
		activity = null;
		
		ServerDiscovery.getInstance().stopDiscovery();
		
		searching = false;
	}
	
	@Override
	protected void onPostExecute(Void v) {
		FAIMSLog.log();
		
		IFAIMSDialogListener listener = (IFAIMSDialogListener) activity;
		listener.handleDialogResponse(
				ServerDiscovery.getInstance().isServerHostValid() ? 
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
