package au.org.intersect.faims.android.ui.dialog;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import au.org.intersect.faims.android.tasks.ActionResultCode;
import au.org.intersect.faims.android.tasks.ActionType;

public class BusyDialog extends ProgressDialog {

	private ActionType type;
	private IFAIMSDialogListener listener;
	
	public BusyDialog(Activity activity, ActionType type, String title, String message) {
		super(activity);
		this.type = type;
		this.listener = (IFAIMSDialogListener) activity;
		setTitle(title);
		setMessage(message);
		setCancelable(true);
		setIndeterminate(true);
		setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				listener.handleDialogResponse(ActionResultCode.CANCEL, null, BusyDialog.this.type, BusyDialog.this);
			}
			
		});
	}
	
	public static BusyDialog create(Activity activity, ActionType type, String title, String message) {
		return new BusyDialog(activity, type, title, message);
	}
	
}
