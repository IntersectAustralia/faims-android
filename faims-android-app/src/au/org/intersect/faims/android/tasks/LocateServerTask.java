package au.org.intersect.faims.android.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCodes;
import au.org.intersect.faims.android.ui.dialog.DialogTypes;
import au.org.intersect.faims.android.ui.dialog.IFAIMSDialogListener;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;

public class LocateServerTask extends AsyncTask<Void, Integer, Void> implements ServerDiscovery.ServerDiscoveryListener {
	
	private Activity activity;
	private BusyDialog dialog;
	private int attempts;
	private TaskTypes taskType;
	
	public LocateServerTask(Activity activity, TaskTypes taskType) {
		this.activity = activity;
		this.attempts = 0;
	}
	
	@Override 
	protected void onPreExecute() {
		FAIMSLog.log();
		
		dialog = DialogFactory.createBusyDialog(activity, 
				DialogTypes.LOCATE_SERVER, 
				activity.getString(R.string.locate_server_failure_title), 
				activity.getString(R.string.locate_server_failure_message));
	}
	
	@Override
	protected Void doInBackground (Void... values) {
		FAIMSLog.log();
		
		try {
			ServerDiscovery.getInstance().findServer(this, attempts);
			attempts++;
			publishProgress(attempts);
			wait();
		} catch (InterruptedException e) {
			FAIMSLog.log(e);
		}
		
		return null;
	}
	
	@Override
	protected void onCancelled() {
		FAIMSLog.log();
		
		dialog.dismiss();
	}
	
	@Override
	protected void onPostExecute(Void v) {
		FAIMSLog.log();
		
		IFAIMSDialogListener listener = (IFAIMSDialogListener) activity;
		listener.handleDialogResponse(attempts < getMaxAttempts() ? DialogResultCodes.SUCCESS : DialogResultCodes.FAILURE, 
				taskType, 
				DialogTypes.LOCATE_SERVER, 
				dialog);
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		FAIMSLog.log();
		
		dialog.setMessage(activity.getString(R.string.locate_server_failure_message) + " " + String.valueOf(values[0]) + "...");
	}

	@Override
	public void handleDiscoveryResponse(boolean success) {
		FAIMSLog.log();
		
		if (success) {
			FAIMSLog.log("found server");
			notify();
		} else {
			FAIMSLog.log("attempt " + String.valueOf(attempts));
			
			if (attempts < getMaxAttempts()) {
				ServerDiscovery.getInstance().findServer(this, attempts);
				
				attempts++;
				publishProgress(attempts);
			} else {
				FAIMSLog.log("server discovery exhausted");
				notify();
			}
		}	
	}
	
	private int getMaxAttempts() {
		return activity.getResources().getInteger(R.integer.broadcast_attempts);
	}
}
