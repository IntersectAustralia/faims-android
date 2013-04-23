package au.org.intersect.faims.android.tasks;

import java.io.File;

import android.os.AsyncTask;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.util.FileUtil;

public class CopyFileTask extends AsyncTask<Void, Void, Void> {
	
	private String fromPath;
	private String toPath;
	private ITaskListener listener;

	public CopyFileTask(String fromPath, String toPath, ITaskListener listener) {
		this.fromPath = fromPath;
		this.toPath = toPath;
		this.listener = listener;
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		
		try {
			FileUtil.copyFile(fromPath, toPath);
		} catch (Exception e) {
			FLog.e("error copying file", e);
		}
		
		return null;
	}
	
	@Override
	protected void onCancelled() {
		File file = new File(toPath);
		if (file.exists()) file.delete();
	}
	
	@Override
	protected void onPostExecute(Void v) {
		listener.handleTaskCompleted(null);
	}

}
