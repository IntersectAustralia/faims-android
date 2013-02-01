package au.org.intersect.faims.android.ui.dialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

public class BusyDialog extends ProgressDialog {

	public BusyDialog(Context context, String title, String message, final IDialogListener listener) {
		super(context);
		setTitle(title);
		setMessage(message);
		setCancelable(true);
		setIndeterminate(true);
		setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				listener.handleDialogResponse(DialogResultCode.CANCEL);
			}
			
		});
	}
	
}
