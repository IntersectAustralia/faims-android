package au.org.intersect.faims.android.net;

import android.app.Application;

public interface IServerDiscovery {
	
	public interface ServerDiscoveryListener {
		
		void handleDiscoveryResponse(boolean success);
		
	}

	public void setApplication(Application app);
	public boolean isServerHostValid();
	public void invalidateServerHost();
	public String getServerHost();
	
	public void startDiscovery(ServerDiscoveryListener listener);
	public void stopDiscovery();
	
}
