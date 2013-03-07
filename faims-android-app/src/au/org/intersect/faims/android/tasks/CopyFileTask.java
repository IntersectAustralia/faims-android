package au.org.intersect.faims.android.tasks;

import java.io.File;

import android.os.AsyncTask;
import android.util.Log;
import au.org.intersect.faims.android.util.FileUtil;

public class CopyFileTask extends AsyncTask<Void, Void, Void> {
	
	private String fromPath;
	private String toPath;

	public CopyFileTask(String fromPath, String toPath) {
		this.fromPath = fromPath;
		this.toPath = toPath;
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		
		try {
			FileUtil.copyFile(fromPath, toPath);
		} catch (Exception e) {
			Log.e("FAIMS", "Failed to copy file", e);
		}
		
		return null;
	}
	
	@Override
	protected void onCancelled() {
		File file = new File(toPath);
		if (file.exists()) file.delete();
		Log.d("FAIMS", "Cancelled copy file");
	}

}
