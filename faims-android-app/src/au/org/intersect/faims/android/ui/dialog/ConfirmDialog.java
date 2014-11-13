package au.org.intersect.faims.android.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import au.org.intersect.faims.android.R;

public class ConfirmDialog extends AlertDialog {
	
	public ConfirmDialog(Context context, String title, String message, final IDialogListener listener) {
		super(context);
		setTitle(title);
		setMessage(message);
		setButton(BUTTON_NEUTRAL, context.getString(R.string.confirm_dialog_button), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(DialogResultCode.SELECT_OK);
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
