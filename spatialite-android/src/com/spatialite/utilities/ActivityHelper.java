package com.spatialite.utilities;

import java.io.File;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

public class ActivityHelper {
	private static final String TAG = "ActivityHelper";

	static public void showAlert(Context ctx, final String message) {
		AlertDialog alertDialog = new AlertDialog.Builder(ctx).create();
		alertDialog.setTitle("Application Error");
		alertDialog.setMessage(message);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Dismiss",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Do nothing
					}
				});
		alertDialog.show();
	}

	static public String getDataBase(Context ctx, String filename) {
		File db = null;

		// Check application storage first
		db = new File(getPath(ctx, false), filename);
		Log.d(TAG, "Checking: " + db.toString());
		if (db.exists()) {
			return db.toString();
		}

		// Check external storage second
		db = new File(getPath(ctx, true), filename);
		Log.d(TAG, "Checking: " + db.toString());
		if (db.exists()) {
			return db.toString();
		}

		return null;
	}

	static public File getPath(Context ctx, boolean externalStorage) {
		if (externalStorage) {
			return ctx.getExternalFilesDir(null);
		} else {
			return ctx.getFilesDir();
		}
	}
}
