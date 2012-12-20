package au.org.intersect.faims.android.tasks;

import android.content.Context;
import android.os.AsyncTask;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.DialogType;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;


public class DownloadProjectTask extends AsyncTask<Void, Void, Void> {

	FAIMSClient faimsClient;
	ServerDiscovery serverDiscovery;
	
	private Context context;
	private BusyDialog dialog;
	private FAIMSClientResultCode errorCode;
	private Project project;
	
	public DownloadProjectTask(Context context, Project project, FAIMSClient faimsClient, ServerDiscovery serverDiscovery) {
		this.context = context;
		this.project = project;
		this.faimsClient = faimsClient;
		this.serverDiscovery = serverDiscovery;
	}
	
	@Override 
	protected void onPreExecute() {
		FAIMSLog.log();
		
		dialog = DialogFactory.createBusyDialog(context, 
				DialogType.BUSY_DOWNLOADING_PROJECT, 
				context.getString(R.string.download_project_title), 
				context.getString(R.string.download_project_message));
		dialog.show();
	}
	
	@Override
	public Void doInBackground(Void... values){
		FAIMSLog.log();
		
		errorCode = faimsClient.downloadProjectArchive(project);

		return null;
	}
	
	@Override
	protected void onCancelled() {
		FAIMSLog.log();
		
		// cleanup to avoid memory leaks
		dialog.cleanup();
		context = null;
	}
	
	@Override
	protected void onPostExecute(Void v) {
		FAIMSLog.log();
		
		dialog.dismiss();
		
		IActionListener listener = (IActionListener) context;
		ActionResultCode code = null;
		
		if (errorCode == FAIMSClientResultCode.SUCCESS) {
			code = ActionResultCode.SUCCESS;
		} else {
			code = ActionResultCode.FAILURE;
			serverDiscovery.invalidateServerHost();
		}
		
		listener.handleActionResponse(code, 
				errorCode, 
				ActionType.DOWNLOAD_PROJECT);
	}
	
}
