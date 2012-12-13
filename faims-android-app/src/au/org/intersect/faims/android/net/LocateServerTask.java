package au.org.intersect.faims.android.net;

import android.app.Activity;
import android.os.AsyncTask;
import au.org.intersect.faims.android.LocateServerDialog;

public class LocateServerTask extends AsyncTask<Void, Integer, Void> {
	
	private Activity activity;
	private LocateServerDialog dialog;
	
	public LocateServerTask(Activity activity) {
		this.activity = activity;
	}
	
	@Override 
	protected void onPreExecute() {
		
	}
	
	@Override
	protected Void doInBackground (Void... values) {
		return null;
		
	}
	
	@Override
	protected void onCancelled() {
		
	}
	
	@Override
	protected void onPostExecute(Void v) {
		
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		
	}
}
