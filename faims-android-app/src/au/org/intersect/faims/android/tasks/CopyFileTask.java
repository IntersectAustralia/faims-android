package au.org.intersect.faims.android.tasks;

import java.io.File;

import android.os.AsyncTask;
import android.os.Environment;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.util.FileUtil;

public class CopyFileTask extends AsyncTask<Void, Void, Void> {
	
	private File fromPath;
	private File toPath;
	private File tempPath;
	private ITaskListener listener;

	public CopyFileTask(File fromPath, File toPath, ITaskListener listener) {
		this.fromPath = fromPath;
		this.toPath = toPath;
		this.listener = listener;
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		
		try {
			// copy file to temp path and move file to new location
			tempPath = File.createTempFile("file", ".tmp", new File(Environment.getExternalStorageDirectory() + FaimsSettings.modulesDir));
			FileUtil.copyFile(fromPath, tempPath);
			tempPath.renameTo(toPath);
		} catch (Exception e) {
			FLog.e("error copying file", e);
		}
		
		return null;
	}
	
	@Override
	protected void onCancelled() {
		if (tempPath.exists()) {
			FileUtil.delete(tempPath);
		}
	}
	
	@Override
	protected void onPostExecute(Void v) {
		listener.handleTaskCompleted(null);
	}

}
