package au.org.intersect.faims.android.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import au.org.intersect.faims.android.two.R;

public class ChoiceDialog extends AlertDialog {
	
	public ChoiceDialog(Context context, String title, String message, IDialogListener listener) {
		this(context, title, message, listener, context.getString(R.string.choice_negative_button), context.getString(R.string.choice_positive_button));
	}
	
	public ChoiceDialog(Context context, String title, String message, final IDialogListener listener, final String noString, final String yesString) {
		super(context);
		setTitle(title);
		setMessage(message);
		setButton(BUTTON_NEGATIVE, noString, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(DialogResultCode.SELECT_NO);
			}
		});
		setButton(BUTTON_POSITIVE, yesString, new DialogInterface.OnClickListener() {
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
