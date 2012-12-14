package au.org.intersect.faims.android.ui.dialog;

import android.app.Activity;
import android.app.ProgressDialog;
import au.org.intersect.faims.android.tasks.ActionType;

public class DownloadDialog extends ProgressDialog {

	//private DialogTypes type;
	//private IFAIMSDialogListener listener;
	
	public DownloadDialog(Activity activity, ActionType type, String title, String message) {
		super(activity);
		//this.type = type;
		//this.listener = (IFAIMSDialogListener) activity;
		setTitle(title);
		setMessage(message);
		setCancelable(false);
		setIndeterminate(false);
		setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	}
	
	public static DownloadDialog create(Activity activity, ActionType type, String title, String message) {
		return new DownloadDialog(activity, type, title, message);
	}
	
}
