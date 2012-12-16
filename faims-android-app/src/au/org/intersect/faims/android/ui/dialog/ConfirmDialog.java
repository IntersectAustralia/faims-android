package au.org.intersect.faims.android.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.tasks.ActionResultCode;
import au.org.intersect.faims.android.tasks.ActionType;

public class ConfirmDialog extends AlertDialog implements IFAIMSDialog {

	private ActionType type;
	private IFAIMSDialogListener listener;
	
	public ConfirmDialog(Activity activity, ActionType type, String title, String message) {
		super(activity);
		this.type = type;
		this.listener = (IFAIMSDialogListener) activity;
		setTitle(title);
		setMessage(message);
		setButton(BUTTON_NEUTRAL, activity.getString(R.string.confirm_dialog_button), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(ActionResultCode.SELECT_OK, null, ConfirmDialog.this.type, ConfirmDialog.this);
			}
		});
		setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				listener.handleDialogResponse(ActionResultCode.CANCEL, null, ConfirmDialog.this.type, ConfirmDialog.this);
			}
		});
	}
	
	public static ConfirmDialog create(Activity activity, ActionType type, String title, String message) {
		return new ConfirmDialog(activity, type, title, message);
	}

	@Override
	public void cleanup() {
		listener = null; // avoid memory leaks
	}
	
}
