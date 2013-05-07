package au.org.intersect.faims.android.tasks;

import java.net.InetSocketAddress;
import java.net.Socket;

import android.os.AsyncTask;
import au.org.intersect.faims.android.log.FLog;

/**
 * Async Task to test the connection for faims server
 * @author danielt
 *
 */
public class FaimsServerConnectionTestingTask extends AsyncTask<Void, Void, Void> {

	private ITaskListener listener;
	private String ipAddress;
	private String port;
	private boolean success;
	private Socket s;

	public FaimsServerConnectionTestingTask(String ipAddress, String port, ITaskListener listener){
		this.ipAddress = ipAddress;
		this.port = port;
		this.listener = listener;
	}
	
	@Override
	protected Void doInBackground(Void... arg0) {
		try{
			s = new Socket();
			s.connect(new InetSocketAddress(ipAddress, Integer.parseInt(port)));
			s.close();
			success = true;
		}catch(Exception e){
			FLog.e("invalid ip address or port", e);
			success = false;
		}
		return null;
	}

	@Override
	protected void onCancelled() {
		if(s != null){
			try {
				s.close();
			} catch(Exception e){
				FLog.e("error when cancelling the connection test", e);
			}
		}
	}

	@Override
	protected void onPostExecute(Void v) {
		listener.handleTaskCompleted(success);
	}
}
