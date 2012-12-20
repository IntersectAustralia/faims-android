package au.org.intersect.faims.android.ui.dialog;

import android.app.Dialog;

public interface IDialogListener {

	public void handleDialogResponse(DialogResultCode resultCode, Object data, DialogType type, Dialog dialog);
}
