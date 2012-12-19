package au.org.intersect.faims.android.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.IFAIMSDialogListener;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;


public class DownloadProjectTask extends AsyncTask<Project, Void, Void> {

	private Activity activity;
	private FAIMSClient faimsClient;
	private BusyDialog dialog;
	private FAIMSClientResultCode errorCode;
	
	public DownloadProjectTask(Activity activity, FAIMSClient faimsClient) {
		this.activity = activity;
		this.faimsClient = faimsClient;
	}
	
	@Override 
	protected void onPreExecute() {
		FAIMSLog.log();
		
		dialog = DialogFactory.createBusyDialog(activity, 
				ActionType.DOWNLOAD_PROJECT, 
				activity.getString(R.string.download_project_title), 
				activity.getString(R.string.download_project_message));
		dialog.show();
	}
	
	@Override
	protected Void doInBackground (Project... values) {
		FAIMSLog.log();
		
		errorCode = faimsClient.downloadProjectArchive(values[0]);

		return null;
	}
	
	@Override
	protected void onCancelled() {
		FAIMSLog.log();
		
		// cleanup to avoid memory leaks
		dialog.cleanup();
		activity = null;
		
	}
	
	@Override
	protected void onPostExecute(Void v) {
		FAIMSLog.log();
		
		IFAIMSDialogListener listener = (IFAIMSDialogListener) activity;
		ActionResultCode code = null;
		if (errorCode == FAIMSClientResultCode.SUCCESS)
			code = ActionResultCode.SUCCESS;
		else
			code = ActionResultCode.FAILURE;
		listener.handleDialogResponse(code, 
				errorCode, 
				ActionType.DOWNLOAD_PROJECT,
				dialog);
	}
	
}
