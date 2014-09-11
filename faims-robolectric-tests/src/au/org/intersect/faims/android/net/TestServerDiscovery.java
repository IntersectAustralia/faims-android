package au.org.intersect.faims.android.net;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.inject.Provider;

public class TestServerDiscovery extends ServerDiscovery {
	
	private boolean hostValid = true;
	
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
	
	public void setHostValid(boolean value) {
		hostValid = value;
	}
	
	public static Provider<TestServerDiscovery> createProvider(final boolean hostValid)
	{
		return new Provider<TestServerDiscovery>() {

			@Override
			public TestServerDiscovery get() {
				TestServerDiscovery ds = new TestServerDiscovery();
				ds.setHostValid(hostValid);
				return ds;
			}
			
		};
	}
	
	public void mockSharedPreferences(Activity activity) {
		SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).edit();
		// not used, as long as it's not empty 
		prefs.putString("pref_server_ip", "test_server");
		prefs.apply();
	}

}
