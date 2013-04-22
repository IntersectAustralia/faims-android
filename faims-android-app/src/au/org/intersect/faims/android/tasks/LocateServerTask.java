package au.org.intersect.faims.android.tasks;

import android.os.AsyncTask;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.net.ServerDiscovery.ServerDiscoveryListener;

import com.google.inject.Inject;

public class LocateServerTask extends AsyncTask<Void, Void, Void> implements ServerDiscoveryListener {

	@Inject
	ServerDiscovery serverDiscovery;
	
	private IActionListener listener;
	
	private boolean searching;
	
	public LocateServerTask(ServerDiscovery serverDiscovery, IActionListener listener) {
		this.serverDiscovery = serverDiscovery;
		this.listener = listener;
	}
	
	@Override
	public Void doInBackground(Void... values) {
		
		searching = true;
		serverDiscovery.startDiscovery(this);
		
		try {
			// wait for search to finish
			while(searching) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			
		}
		
		return null;
	}
	
	@Override
	protected void onCancelled() {
		
		serverDiscovery.stopDiscovery();
		searching = false;
	}
	
	@Override
	protected void onPostExecute(Void v) {
		
		listener.handleActionResponse(
				serverDiscovery.isServerHostValid() ? 
						ActionResultCode.SUCCESS : ActionResultCode.FAILURE, null);
	}
	
	@Override
	public void handleDiscoveryResponse(boolean success) {
		
		searching = false;
	}
	
}
