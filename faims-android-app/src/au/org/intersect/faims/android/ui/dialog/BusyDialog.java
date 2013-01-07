package au.org.intersect.faims.android.ui.dialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

public class BusyDialog extends ProgressDialog implements IDialog {

	private DialogType type;
	private IDialogListener listener;
	
	public BusyDialog(Context context, DialogType type, String title, String message) {
		super(context);
		this.type = type;
		this.listener = (IDialogListener) context;
		setTitle(title);
		setMessage(message);
		setCancelable(true);
		setIndeterminate(true);
		setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				listener.handleDialogResponse(DialogResultCode.CANCEL, null, BusyDialog.this.type, BusyDialog.this);
			}
			
		});
	}
	
	public static BusyDialog create(Context context, DialogType type, String title, String message) {
		return new BusyDialog(context, type, title, message);
	}

	@Override
	public void cleanup() {
		listener = null; // avoid memory leaks
	}
	
}
