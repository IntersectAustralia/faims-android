package au.org.intersect.faims.android.roboguice;

import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.ServerDiscovery;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class FAIMSModule extends AbstractModule implements Module {

	@Override
	public void configure() {
		bind(FAIMSClient.class);
		bind(ServerDiscovery.class);
	}

}
