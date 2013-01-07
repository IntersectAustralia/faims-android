package au.org.intersect.faims.android.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import au.org.intersect.faims.android.R;

public class ChoiceDialog extends AlertDialog implements IDialog {
	
	private DialogType type;
	private IDialogListener listener;
	
	public ChoiceDialog(Context context, DialogType type, String title, String message) {
		super(context);
		this.type = type;
		this.listener = (IDialogListener) context;
		setTitle(title);
		setMessage(message);
		setButton(BUTTON_NEGATIVE, context.getString(R.string.choice_negative_button), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(DialogResultCode.SELECT_NO, null, ChoiceDialog.this.type, ChoiceDialog.this);
			}
		});
		setButton(BUTTON_POSITIVE, context.getString(R.string.choice_positive_button), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listener.handleDialogResponse(DialogResultCode.SELECT_YES, null, ChoiceDialog.this.type, ChoiceDialog.this);
			}
		});
		setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				listener.handleDialogResponse(DialogResultCode.CANCEL, null, ChoiceDialog.this.type, ChoiceDialog.this);
			}
		});
	}
	
	public static ChoiceDialog create(Context context, DialogType type, String title, String message) {
		return new ChoiceDialog(context, type, title, message);
	}

	@Override
	public void cleanup() {
		listener = null; // avoid memory leaks
	}
	
}
