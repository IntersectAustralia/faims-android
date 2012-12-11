package au.org.intersect.faims.android.roboguice;

import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.IFAIMSClient;

import com.google.inject.Binder;
import com.google.inject.Module;

public class FAIMSModule implements Module {

	@Override
	public void configure(Binder binder) {
		
		binder.bind(IFAIMSClient.class).to(FAIMSClient.class);
		
	}

}
