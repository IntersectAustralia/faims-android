package au.org.intersect.faims.android.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import au.org.intersect.faims.android.R;

public class ConfirmDialog extends AlertDialog implements IDialog {

	private DialogType type;
	private IDialogListener listener;
	
	public ConfirmDialog(Context context, DialogType type, String title, String message) {
		super(context);
		this.type = type;
		this.listener = (IDialogListener) context;
		setTitle(title);
		setMessage(message);
		setButton(BUTTON_NEUTRAL, context.getString(R.string.confirm_dialog_button), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(DialogResultCode.SELECT_OK, null, ConfirmDialog.this.type, ConfirmDialog.this);
			}
		});
		setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				listener.handleDialogResponse(DialogResultCode.CANCEL, null, ConfirmDialog.this.type, ConfirmDialog.this);
			}
		});
	}
	
	public static ConfirmDialog create(Context context, DialogType type, String title, String message) {
		return new ConfirmDialog(context, type, title, message);
	}

	@Override
	public void cleanup() {
		listener = null; // avoid memory leaks
	}
	
}
