package au.org.intersect.faims.android;

import android.app.Dialog;

public interface IFAIMSDialogListener {

	public void handleDialogResponse(int resultCode, String type, Dialog dialog);
	
}
