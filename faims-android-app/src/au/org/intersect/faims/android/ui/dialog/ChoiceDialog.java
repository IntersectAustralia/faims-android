package au.org.intersect.faims.android.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import au.org.intersect.faims.android.R;

public class ChoiceDialog extends AlertDialog {
	
	public ChoiceDialog(Context context, String title, String message, final IDialogListener listener) {
		super(context);
		setTitle(title);
		setMessage(message);
		setButton(BUTTON_NEGATIVE, context.getString(R.string.choice_negative_button), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(DialogResultCode.SELECT_NO);
			}
		});
		setButton(BUTTON_POSITIVE, context.getString(R.string.choice_positive_button), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(DialogResultCode.SELECT_YES);
			}
		});
		setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				listener.handleDialogResponse(DialogResultCode.CANCEL);
			}
		});
	}
	
}
