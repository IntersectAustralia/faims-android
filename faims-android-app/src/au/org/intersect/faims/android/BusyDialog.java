package au.org.intersect.faims.android;

import android.app.Activity;
import android.app.ProgressDialog;

public class BusyDialog extends ProgressDialog {

	private String type;
	private IFAIMSDialogListener listener;
	
	public static final String TYPE = "BUSY_DIALOG";
	
	public BusyDialog(Activity activity, String type, String title, String message) {
		super(activity);
		this.type = type;
		this.listener = (IFAIMSDialogListener) activity;
		setTitle(title);
		setMessage(message);
		setCancelable(false);
		setIndeterminate(true);
	}
	
	public static BusyDialog create(Activity activity, String type, String title, String message) {
		return new BusyDialog(activity, type, title, message);
	}
	
}
