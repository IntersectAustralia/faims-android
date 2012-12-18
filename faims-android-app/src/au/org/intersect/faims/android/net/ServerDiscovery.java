package au.org.intersect.faims.android.net;

import android.app.Application;


public class ServerDiscovery implements IServerDiscovery {

	@Override
	public void setApplication(Application app) {
		ServerDiscoveryMaster.getInstance().setApplication(app);
	}

	@Override
	public boolean isServerHostValid() {
		return ServerDiscoveryMaster.getInstance().isServerHostValid();
	}

	@Override
	public void invalidateServerHost() {
		ServerDiscoveryMaster.getInstance().invalidateServerHost();
	}

	@Override
	public String getServerHost() {
		return ServerDiscoveryMaster.getInstance().getServerHost();
	}

	@Override
	public void startDiscovery(ServerDiscoveryListener listener) {
		ServerDiscoveryMaster.getInstance().startDiscovery(listener);
	}

	@Override
	public void stopDiscovery() {
		ServerDiscoveryMaster.getInstance().stopDiscovery();
	}
	
	
	
}
