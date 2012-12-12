package au.org.intersect.faims.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.Log;
import au.org.intersect.faims.android.LocateServerDialog;
import au.org.intersect.faims.android.R;

public class DialogCreatorUtil {
	
	public static LocateServerDialog createLocateServerDialog(Activity activity, LocateServerDialog.LocateServerDialogListener listener) {
		LocateServerDialog dialog = new LocateServerDialog(activity, listener);
		return dialog;
	}
	
	public static AlertDialog createlocateServerFailureDialog(Activity activity, DialogInterface.OnClickListener listener) {
		return new AlertDialog.Builder(activity)
			.setTitle(activity.getString(R.string.locate_server_failure_title))
			.setMessage(activity.getString(R.string.locate_server_failure_message))
			.setNegativeButton(activity.getString(R.string.failure_negative_button), 
					new DialogInterface.OnClickListener() {
				
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Log.d("debug", "locateServerFailureDialog.dismiss");
							dialog.dismiss();
						}
			})
			.setPositiveButton(activity.getString(R.string.failure_positive_button), listener)
			.create();
	}
	
	public static ProgressDialog createServerRequestDialog(Activity activity, String title, String message) {
		ProgressDialog dialog = new ProgressDialog(activity);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		return dialog;
	}
	
	public static AlertDialog createServerRequestFailureDialog(Activity activity, DialogInterface.OnClickListener listener) {
		return new AlertDialog.Builder(activity)
			.setTitle(activity.getString(R.string.server_request_failure_title))
			.setMessage(activity.getString(R.string.server_request_failure_message))
			.setNegativeButton(activity.getString(R.string.failure_negative_button), 
					new DialogInterface.OnClickListener() {
				
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Log.d("debug", "serverRequestFailure.dismiss");
							dialog.dismiss();
						}
			})
			.setPositiveButton(activity.getString(R.string.failure_positive_button), listener)
			.create();
	}
	
	public static void createFetchProjectDialog(Activity activity, DialogInterface.OnDismissListener listener) {
		
	}
	
	public static void createFetchProjectFailureDialog(Activity activity, DialogInterface.OnClickListener listener) {
		
	}
	
	public static void createFetchProjectErrorDialog(Activity activity, DialogInterface.OnDismissListener listener) {
		
	}
	
}
