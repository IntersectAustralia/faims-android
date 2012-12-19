package au.org.intersect.faims.android.roboguice;

import au.org.intersect.faims.android.net.FAIMSClient;

import com.google.inject.Binder;
import com.google.inject.Module;

public class FAIMSModule implements Module {

	@Override
	public void configure(Binder binder) {
		
		binder.bind(FAIMSClient.class).to(FAIMSClient.class);
		
	}

}
