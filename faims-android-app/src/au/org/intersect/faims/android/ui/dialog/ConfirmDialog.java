package au.org.intersect.faims.android.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import au.org.intersect.faims.android.R;

public class ConfirmDialog extends AlertDialog {

	public static final int OK = 0;
	
	private DialogTypes type;
	private IFAIMSDialogListener listener;
	
	public ConfirmDialog(Activity activity, DialogTypes type, String title, String message) {
		super(activity);
		this.type = type;
		this.listener = (IFAIMSDialogListener) activity;
		setTitle(title);
		setMessage(message);
		setButton(BUTTON_NEUTRAL, activity.getString(R.string.confirm_dialog_button), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(DialogResultCodes.SELECT_OK, null, ConfirmDialog.this.type, ConfirmDialog.this);
			}
		});
	}
	
	public static ConfirmDialog create(Activity activity, DialogTypes type, String title, String message) {
		return new ConfirmDialog(activity, type, title, message);
	}
	
}
