package au.org.intersect.faims.android.tasks;

import android.content.Context;
import android.os.AsyncTask;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.net.ServerDiscovery.ServerDiscoveryListener;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.DialogType;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;

public class LocateServerTask extends AsyncTask<Void, Void, Void> implements ServerDiscoveryListener {

	ServerDiscovery serverDiscovery;
	
	private Context context;
	private BusyDialog dialog;
	private boolean searching;
	private TaskType taskType;
	
	public LocateServerTask(Context context, TaskType taskType, ServerDiscovery serverDiscovery) {
		this.context = context;
		this.taskType = taskType;
		this.serverDiscovery = serverDiscovery;
	}
	
	@Override 
	protected void onPreExecute() {
		FAIMSLog.log();
		
		dialog = DialogFactory.createBusyDialog(context, 
				DialogType.BUSY_LOCATING_SERVER, 
				context.getString(R.string.locate_server_title), 
				context.getString(R.string.locate_server_message));
		dialog.show();
	}
	
	@Override
	public Void doInBackground(Void... values) {
		FAIMSLog.log();
		
		searching = true;
		serverDiscovery.startDiscovery(this);
		
		try {
			// wait for search to finish
			while(searching) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			
		}
		
		return null;
	}
	
	@Override
	protected void onCancelled() {
		FAIMSLog.log();
		
		// cleanup to avoid memory leaks
		dialog.cleanup();
		context = null;
		
		serverDiscovery.stopDiscovery();
		searching = false;
	}
	
	@Override
	protected void onPostExecute(Void v) {
		FAIMSLog.log();
		
		dialog.dismiss();
		
		IActionListener listener = (IActionListener) context;
		listener.handleActionResponse(
				serverDiscovery.isServerHostValid() ? 
						ActionResultCode.SUCCESS : ActionResultCode.FAILURE, 
				taskType, 
				ActionType.LOCATE_SERVER);
	}
	
	@Override
	public void handleDiscoveryResponse(boolean success) {
		FAIMSLog.log();
		searching = false;
	}
	
}
