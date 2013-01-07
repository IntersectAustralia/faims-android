package au.org.intersect.faims.android.util;

import android.content.Context;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.ChoiceDialog;
import au.org.intersect.faims.android.ui.dialog.ConfirmDialog;
import au.org.intersect.faims.android.ui.dialog.DialogType;
import au.org.intersect.faims.android.ui.dialog.DownloadDialog;

public class DialogFactory {
	
	public static ChoiceDialog createChoiceDialog(Context context, DialogType type, String title, String message) {
		return ChoiceDialog.create(context, type, title, message);
	}
	
	public static ConfirmDialog createConfirmDialog(Context context, DialogType type, String title, String message) {
		return ConfirmDialog.create(context, type, title, message);
	}
	
	public static BusyDialog createBusyDialog(Context context, DialogType type, String title, String message) {
		return BusyDialog.create(context, type, title, message);
	}
	
	public static DownloadDialog createDownloadDialog(Context context, DialogType type, String title, String message) {
		return DownloadDialog.create(context, type, title, message);
	}
	
}
