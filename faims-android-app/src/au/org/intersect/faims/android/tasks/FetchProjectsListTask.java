package au.org.intersect.faims.android.tasks;

import java.util.LinkedList;

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


public class FetchProjectsListTask extends AsyncTask<Void, Void, Void> {

	FAIMSClient faimsClient;
	ServerDiscovery serverDiscovery;
	
	private Context context;
	
	private BusyDialog dialog;
	private LinkedList<Project> projects;
	private FAIMSClientResultCode errorCode;

	public FetchProjectsListTask(Context context, FAIMSClient faimsClient, ServerDiscovery serverDiscovery) {
		this.context = context;
		this.faimsClient = faimsClient;
		this.serverDiscovery = serverDiscovery;
	}
	
	@Override 
	protected void onPreExecute() {
		FAIMSLog.log();
		
		dialog = DialogFactory.createBusyDialog(context, 
				DialogType.BUSY_FETCHING_PROJECT_LIST, 
				context.getString(R.string.fetch_projects_title), 
				context.getString(R.string.fetch_projects_message));
		dialog.show();
	} 
	
	@Override
	protected Void doInBackground(Void... values) {
		FAIMSLog.log();
		
		projects = new LinkedList<Project>();
		errorCode = faimsClient.fetchProjectList(projects);
	
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
				projects, 
				ActionType.FETCH_PROJECT_LIST);
	}
	
}
