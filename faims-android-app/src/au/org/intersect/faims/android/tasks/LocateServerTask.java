package au.org.intersect.faims.android.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.IFAIMSDialogListener;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;

public class LocateServerTask extends AsyncTask<Void, Integer, Void> implements ServerDiscovery.ServerDiscoveryListener {
	
	private Activity activity;
	private BusyDialog dialog;
	private int attempts;
	private boolean searching;
	private TaskType taskType;
	
	public LocateServerTask(Activity activity, TaskType taskType) {
		this.activity = activity;
		this.attempts = 0;
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
			
			ServerDiscovery.getInstance().findServer(this, attempts);
			
			publishProgress(attempts + 1);
			
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
		
		// TODO clear this listener only
		ServerDiscovery.getInstance().clearListeners();
		
		searching = false;
	}
	
	@Override
	protected void onPostExecute(Void v) {
		FAIMSLog.log();
		
		IFAIMSDialogListener listener = (IFAIMSDialogListener) activity;
		listener.handleDialogResponse(attempts < getMaxAttempts() ? ActionResultCode.SUCCESS : ActionResultCode.FAILURE, 
				taskType, 
				ActionType.LOCATE_SERVER, 
				dialog);
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		FAIMSLog.log();
		
		dialog.setMessage(activity.getString(R.string.locate_server_message) + " " + String.valueOf(values[0]) + "...");
	}

	@Override
	public void handleDiscoveryResponse(boolean success) {
		FAIMSLog.log();
		
		if (success) {
			FAIMSLog.log("found server");
			searching = false;
		} else {
			FAIMSLog.log("attempt " + String.valueOf(attempts));
			
			attempts++;
			if (attempts < getMaxAttempts()) {
				ServerDiscovery.getInstance().findServer(this, attempts);
				
				publishProgress(attempts + 1);
			} else {
				FAIMSLog.log("server discovery exhausted");
				
				searching = false;
			}
		}	
	}
	
	private int getMaxAttempts() {
		return activity.getResources().getInteger(R.integer.broadcast_attempts);
	}
}
