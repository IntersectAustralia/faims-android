package au.org.intersect.faims.android.ui.dialog;

import android.app.Dialog;

public interface IFAIMSDialogListener {

	public void handleDialogResponse(DialogResultCodes resultCode, Object data, DialogTypes type, Dialog dialog);
	
}
