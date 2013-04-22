package au.org.intersect.faims.android.tasks;

import java.util.LinkedList;

import android.os.AsyncTask;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;


public class FetchProjectsListTask extends AsyncTask<Void, Void, Void> {

	private FAIMSClient faimsClient;
	private IActionListener listener;
	
	private LinkedList<Project> projects;
	private FAIMSClientResultCode errorCode;
	
	public FetchProjectsListTask(FAIMSClient faimsClient, IActionListener listener) {
		this.faimsClient = faimsClient;
		this.listener = listener;
	}
	
	@Override
	protected Void doInBackground(Void... values) {
		
		projects = new LinkedList<Project>();
		errorCode = faimsClient.fetchProjectList(projects);
	
		return null;
	}
	
	@Override
	protected void onPostExecute(Void v) {
		
		ActionResultCode code = null;
		
		if (errorCode == FAIMSClientResultCode.SUCCESS) {
			code = ActionResultCode.SUCCESS;
		} else {
			code = ActionResultCode.FAILURE;
			faimsClient.invalidate();
		}		
		
		listener.handleActionResponse(code, projects);
	}
	
}
