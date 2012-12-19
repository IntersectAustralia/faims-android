package au.org.intersect.faims.android.test.helper;

import android.app.Application;
import au.org.intersect.faims.android.net.ServerDiscovery;

import com.google.inject.Provider;

public class TestServerDiscovery extends ServerDiscovery {
	
	private boolean hostValid;

	@Override
	public void setApplication(Application app) {
	}

	@Override
	public boolean isServerHostValid() {
		return hostValid;
	}

	@Override
	public void invalidateServerHost() {
		hostValid = false;
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
	}
	
	protected void setHostInvalid(boolean value) {
		hostValid = value;
	}
	
	public static Provider<TestServerDiscovery> createProvider(final boolean hostValid)
	{
		return new Provider<TestServerDiscovery>() {

			@Override
			public TestServerDiscovery get() {
				TestServerDiscovery ds = new TestServerDiscovery();
				ds.setHostInvalid(hostValid);
				return ds;
			}
			
		};
	}

}
