package au.org.intersect.faims.android.test.helper;

import android.app.Application;
import au.org.intersect.faims.android.net.IServerDiscovery;

public class TestServerDiscovery implements IServerDiscovery {

	@Override
	public void setApplication(Application app) {
	}

	@Override
	public boolean isServerHostValid() {
		return true;
	}

	@Override
	public void invalidateServerHost() {
		//hostValid = false;
	}

	@Override
	public String getServerHost() {
		return "http://thisdoesnotmatter";
	}

	@Override
	public void startDiscovery(ServerDiscoveryListener listener) {
		listener.handleDiscoveryResponse(true);
	}

	@Override
	public void stopDiscovery() {
		// TODO Auto-generated method stub
		
	}

}
