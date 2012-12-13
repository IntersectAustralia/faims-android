package au.org.intersect.faims.android.ui.dialog;

import android.app.Activity;
import android.app.ProgressDialog;

public class BusyDialog extends ProgressDialog {

	//private DialogTypes type;
	//private IFAIMSDialogListener listener;
	
	public BusyDialog(Activity activity, DialogTypes type, String title, String message) {
		super(activity);
		//this.type = type;
		//this.listener = (IFAIMSDialogListener) activity;
		setTitle(title);
		setMessage(message);
		setCancelable(false);
		setIndeterminate(true);
	}
	
	public static BusyDialog create(Activity activity, DialogTypes type, String title, String message) {
		return new BusyDialog(activity, type, title, message);
	}
	
}
