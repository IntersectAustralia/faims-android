package au.org.intersect.faims.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class ConfirmDialog extends AlertDialog {

	public static final int OK = 0;
	
	public static final String DOWNLOAD_PROJECT_TO_BIG_ERROR = "DOWNLOAD_PROJECT_TO_BIG_ERROR";
	
	private String type;
	private IFAIMSDialogListener listener;
	
	public ConfirmDialog(Activity activity, String type, String title, String message) {
		super(activity);
		this.type = type;
		this.listener = (IFAIMSDialogListener) activity;
		setTitle(title);
		setMessage(message);
		setButton(BUTTON_NEUTRAL, activity.getString(R.string.confirm_dialog_button), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(ConfirmDialog.OK, ConfirmDialog.this.type, ConfirmDialog.this);
			}
		});
	}
	
	public static ConfirmDialog create(Activity activity, String type, String title, String message) {
		return new ConfirmDialog(activity, type, title, message);
	}
	
}
