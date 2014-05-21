package au.org.intersect.faims.android.app;

import roboguice.RoboGuice;
import android.app.Application;
import au.org.intersect.faims.android.log.FLog;

public class FAIMSApplication {
	
	private static FAIMSApplication instance;
	private Application application;
	
	public static FAIMSApplication getInstance() {
		if (instance == null) {
			instance = new FAIMSApplication();
		}
		return instance;
	}
	
	public Application getApplication() {
		return application;
	}
	
	public void setApplication(Application application) {
		this.application = application;
	}
	
	public void injectMembers(Object obj) {
		if (application == null) {
			FLog.e("Cannot find application");
			return;
		}
		RoboGuice.getBaseApplicationInjector(application).injectMembers(obj);
	}

}
