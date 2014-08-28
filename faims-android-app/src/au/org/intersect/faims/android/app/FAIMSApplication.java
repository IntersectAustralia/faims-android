package au.org.intersect.faims.android.app;

import roboguice.RoboGuice;
import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
	
	public void saveModuleKey(String key) {
    	SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext()).edit();
    	editor.putString("module-key", key);
    	editor.apply();
	}
	
	public String getSessionModuleKey() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());
		return prefs.getString("module-key", null);
	}

}
