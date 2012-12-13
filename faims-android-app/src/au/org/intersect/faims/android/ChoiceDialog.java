package au.org.intersect.faims.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class ChoiceDialog extends AlertDialog {

	public static final int YES = 0;
	public static final int NO = 1;
	
	public static final String SERVER_DISCOVERY_FAILURE = "SERVER_DISCOVERY_FAILURE";
	public static final String SERVER_REQUEST_FAILURE = "SERVER_REQUEST_FAILURE";
	public static final String DOWNLOAD_PROJECT_FAILURE = "DOWNLOAD_PROJECT_FAILURE";
	public static final String DOWNLOAD_PROJECT = "DOWNLOAD_PROJECT";
	
	private String type;
	private IFAIMSDialogListener listener;
	
	public ChoiceDialog(Activity activity, String type, String title, String message) {
		super(activity);
		this.type = type;
		this.listener = (IFAIMSDialogListener) activity;
		setTitle(title);
		setMessage(message);
		setButton(BUTTON_NEGATIVE, activity.getString(R.string.choice_negative_button), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(ChoiceDialog.NO, ChoiceDialog.this.type, ChoiceDialog.this);
			}
		});
		setButton(BUTTON_POSITIVE, activity.getString(R.string.choice_positive_button), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(ChoiceDialog.YES, ChoiceDialog.this.type, ChoiceDialog.this);
			}
		});
	}
	
	public static ChoiceDialog create(Activity activity, String type, String title, String message) {
		return new ChoiceDialog(activity, type, title, message);
	}
	
}
