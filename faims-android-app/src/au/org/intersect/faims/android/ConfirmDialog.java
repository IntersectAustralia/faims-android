package au.org.intersect.faims.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class ConfirmDialog extends AlertDialog {

	public static final int YES = 0;
	public static final int NO = 1;
	
	public static final String SERVER_DISCOVERY_FAILURE = "SERVER_DISCOVERY_FAILURE";
	public static final String SERVER_REQUEST_FAILURE = "SERVER_REQUEST_FAILURE";
	public static final String DOWNLOAD_PROJECT_FAILURE = "DOWNLOAD_PROJECT_FAILURE";
	public static final String DOWNLOAD_PROJECT = "DOWNLOAD_PROJECT";
	
	private String type;
	private IFAIMSDialogListener listener;
	
	public ConfirmDialog(Activity activity, String type, String title, String message) {
		super(activity);
		this.type = type;
		this.listener = (IFAIMSDialogListener) activity;
		setTitle(title);
		setMessage(message);
		setButton(BUTTON_NEGATIVE, activity.getString(R.string.confirm_negative_button), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(ConfirmDialog.NO, ConfirmDialog.this.type, ConfirmDialog.this);
			}
		});
		setButton(BUTTON_POSITIVE, activity.getString(R.string.confirm_positive_button), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(ConfirmDialog.YES, ConfirmDialog.this.type, ConfirmDialog.this);
			}
		});
	}
	
	public static ConfirmDialog create(Activity activity, String type, String title, String message) {
		return new ConfirmDialog(activity, type, title, message);
	}
	
}
