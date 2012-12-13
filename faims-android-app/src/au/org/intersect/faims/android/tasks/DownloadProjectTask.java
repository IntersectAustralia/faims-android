package au.org.intersect.faims.android.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCodes;
import au.org.intersect.faims.android.net.IFAIMSClient;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCodes;
import au.org.intersect.faims.android.ui.dialog.DialogTypes;
import au.org.intersect.faims.android.ui.dialog.IFAIMSDialogListener;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;


public class DownloadProjectTask extends AsyncTask<Project, Void, Void> {

	private Activity activity;
	private IFAIMSClient faimsClient;
	private BusyDialog dialog;
	private FAIMSClientResultCodes errorCode;
	
	public DownloadProjectTask(Activity activity, IFAIMSClient faimsClient) {
		this.activity = activity;
		this.faimsClient = faimsClient;
	}
	
	@Override 
	protected void onPreExecute() {
		FAIMSLog.log();
		
		dialog = DialogFactory.createBusyDialog(activity, 
				DialogTypes.DOWNLOAD_PROJECT, 
				activity.getString(R.string.download_project_title), 
				activity.getString(R.string.download_project_message));
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
		
		dialog.dismiss();
	}
	
	@Override
	protected void onPostExecute(Void v) {
		FAIMSLog.log();
		
		IFAIMSDialogListener listener = (IFAIMSDialogListener) activity;
		DialogResultCodes code = null;
		if (errorCode == FAIMSClientResultCodes.SUCCESS)
			code = DialogResultCodes.SUCCESS;
		else
			code = DialogResultCodes.FAILURE;
		listener.handleDialogResponse(code, 
				errorCode, 
				DialogTypes.FETCH_PROJECT_LIST,
				dialog);
	}
	
}
