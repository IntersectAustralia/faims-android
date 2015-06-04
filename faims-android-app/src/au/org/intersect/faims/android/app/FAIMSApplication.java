package au.org.intersect.faims.android.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import roboguice.RoboGuice;
import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.log.FLog;

public class FAIMSApplication {
	
	private static final String PAST_SESSIONS_PREF = "pref_previous_sessions";
	private static final String MODULE_KEY_PREF = "module-key";
	private static final String MODULE_ARCH16N_PREF = "module-arch16n";
	
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
	
	public void destroyInjector() {
		RoboGuice.destroyInjector(application);
		RoboGuice.util.reset();
	}
	
	public void saveModuleKey(String key) {
    	SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext()).edit();
    	editor.putString(MODULE_KEY_PREF, key);
    	editor.apply();
	}
	
	public void saveModuleArch16n(String arch16n) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext()).edit();
    	editor.putString(MODULE_ARCH16N_PREF, arch16n);
    	editor.apply();
	}
	
	public String getSessionModuleKey() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());
		return prefs.getString(MODULE_KEY_PREF, null);
	}
	
	public String getSessionModuleArch16n() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());
		return prefs.getString(MODULE_ARCH16N_PREF, "faims.properties");
	}

	public void updateServerSettings(String host, String port, boolean autodiscover) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("pref_server_ip", host);
		editor.putString("pref_server_port", port);
		editor.apply();
		
		if (autodiscover) {
			updateServerList(sp, "autodiscover");
		} else {
			updateServerList(sp, host + ":" + port);
		}
	}

	private void updateServerList(SharedPreferences sp, String server) {
		String sessionsList = sp.getString(PAST_SESSIONS_PREF, "");
		List<String> pastServers;
		if (sessionsList.isEmpty()) {
			addDefaultServers(sp);
			pastServers = new ArrayList<String>(Arrays.asList(sp.getString(PAST_SESSIONS_PREF, "").split(",")));
		} else {
			pastServers = new ArrayList<String>(Arrays.asList(sessionsList.split(",")));
		}
		if (pastServers.contains(server)) {
			// move to start of list
			String existing = pastServers.remove(pastServers.indexOf(server));
			pastServers.add(0, existing);
		} else {
			pastServers.add(0, server);
		}
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(PAST_SESSIONS_PREF, TextUtils.join(",", pastServers));
		editor.apply();
	}

	public ArrayList<NameValuePair> getPastServers() {
		ArrayList<NameValuePair> servers = new ArrayList<NameValuePair>();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());
        String list = prefs.getString(PAST_SESSIONS_PREF, "");
        if (!list.isEmpty()) {
        	for (String server : list.split(",")) {
        		if (server.equals(application.getResources().getString(R.string.demo_server_host) + ":" +
				application.getResources().getString(R.string.demo_server_port))) {
        			servers.add(new NameValuePair("Demo Server", server));
        		} else if (server.equals("autodiscover")) {
        			servers.add(new NameValuePair("Auto Discover Server", server));
        		} else {
        			servers.add(new NameValuePair(server, server));
        		}
        	}
        	servers.add(new NameValuePair("New Server", ""));
        } else {
        	addDefaultServers(prefs);
        	return getPastServers();
        }
        return servers;
	}

	private void addDefaultServers(SharedPreferences sp) {
		SharedPreferences.Editor editor = sp.edit();
		// demo server
		String defaults = application.getResources().getString(R.string.demo_server_host) + ":" +
				application.getResources().getString(R.string.demo_server_port);
		// auto discovery
		defaults += "," + "autodiscover";
		editor.putString(PAST_SESSIONS_PREF, defaults);
		editor.apply();
	}

}
