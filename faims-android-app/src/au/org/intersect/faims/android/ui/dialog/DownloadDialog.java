package au.org.intersect.faims.android.ui.dialog;

import android.app.ProgressDialog;
import android.content.Context;

public class DownloadDialog extends ProgressDialog implements IDialog {

	//private DialogTypes type;
	//private IFAIMSDialogListener listener;
	
	public DownloadDialog(Context context, DialogType type, String title, String message) {
		super(context);
		//this.type = type;
		//this.listener = (IFAIMSDialogListener) activity;
		setTitle(title);
		setMessage(message);
		setCancelable(false);
		setIndeterminate(false);
		setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	}
	
	public static DownloadDialog create(Context context, DialogType type, String title, String message) {
		return new DownloadDialog(context, type, title, message);
	}

	@Override
	public void cleanup() {
		//listener = null; // avoid memory leaks
	}
	
	
}
