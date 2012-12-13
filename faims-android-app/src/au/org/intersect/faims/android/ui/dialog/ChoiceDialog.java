package au.org.intersect.faims.android.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import au.org.intersect.faims.android.R;

public class ChoiceDialog extends AlertDialog {
	
	private DialogTypes type;
	private IFAIMSDialogListener listener;
	
	public ChoiceDialog(Activity activity, DialogTypes type, String title, String message) {
		super(activity);
		this.type = type;
		this.listener = (IFAIMSDialogListener) activity;
		setTitle(title);
		setMessage(message);
		setButton(BUTTON_NEGATIVE, activity.getString(R.string.choice_negative_button), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(DialogResultCodes.SELECT_YES, null, ChoiceDialog.this.type, ChoiceDialog.this);
			}
		});
		setButton(BUTTON_POSITIVE, activity.getString(R.string.choice_positive_button), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(DialogResultCodes.SELECT_NO, null, ChoiceDialog.this.type, ChoiceDialog.this);
			}
		});
	}
	
	public static ChoiceDialog create(Activity activity, DialogTypes type, String title, String message) {
		return new ChoiceDialog(activity, type, title, message);
	}
	
}
