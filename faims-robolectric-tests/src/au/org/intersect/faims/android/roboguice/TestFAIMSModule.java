package au.org.intersect.faims.android.roboguice;

import roboguice.RoboGuice;
import roboguice.config.DefaultRoboModule;
import roboguice.inject.RoboInjector;
import android.app.Application;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.net.TestFAIMSClient;
import au.org.intersect.faims.android.net.TestServerDiscovery;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.xtremelabs.robolectric.Robolectric;

public class TestFAIMSModule implements Module {
	
	@Override
	public void configure(Binder binder) {
		binder.bind(ServerDiscovery.class).to(TestServerDiscovery.class);
		binder.bind(FAIMSClient.class).to(TestFAIMSClient.class);
	}
	
	public static void setUp(Object testObject, TestFAIMSModule module) {
		Module roboGuiceModule = RoboGuice
				.newDefaultRoboModule(Robolectric.application);
		Module productionModule = Modules.override(roboGuiceModule).with(
				new FAIMSModule());
		Module testModule = Modules.override(productionModule).with(module);
		RoboGuice.setBaseApplicationInjector(Robolectric.application,
				RoboGuice.DEFAULT_STAGE, testModule);
		RoboInjector injector = RoboGuice.getInjector(Robolectric.application);
		injector.injectMembers(testObject);
	}

	public static void tearDown() {
		RoboGuice.util.reset();
		Application app = Robolectric.application;
		DefaultRoboModule defaultModule = RoboGuice.newDefaultRoboModule(app);
		RoboGuice.setBaseApplicationInjector(app, RoboGuice.DEFAULT_STAGE,
				defaultModule);
	}

}
