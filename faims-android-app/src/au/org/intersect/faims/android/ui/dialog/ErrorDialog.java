package au.org.intersect.faims.android.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import au.org.intersect.faims.android.R;

public class ErrorDialog extends AlertDialog {
	
	public ErrorDialog(Context context, String title, String message) {
		super(context);
		setTitle(title);
		setMessage(message);
		setButton(BUTTON_NEUTRAL, context.getString(R.string.confirm_dialog_button), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// ignore
			}
		});
	}
	
}
