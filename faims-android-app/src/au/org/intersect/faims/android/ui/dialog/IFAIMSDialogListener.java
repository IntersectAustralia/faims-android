package au.org.intersect.faims.android.ui.dialog;

import android.app.Dialog;
import au.org.intersect.faims.android.tasks.ActionResultCode;
import au.org.intersect.faims.android.tasks.ActionType;

public interface IFAIMSDialogListener {

	public void handleDialogResponse(ActionResultCode resultCode, Object data, ActionType type, Dialog dialog);
	
}
