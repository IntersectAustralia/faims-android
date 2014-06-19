package au.org.intersect.faims.android.ui.activity;

import java.lang.ref.WeakReference;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.tasks.FaimsServerConnectionTestingTask;
import au.org.intersect.faims.android.tasks.ITaskListener;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCode;
import au.org.intersect.faims.android.ui.dialog.IDialogListener;

public class FaimsServerSettingsActivity extends Activity{

	private FaimsServerSettingsFragment settingsFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		

        FAIMSApplication.getInstance().setApplication(getApplication());
		
        settingsFragment = new FaimsServerSettingsFragment();
        settingsFragment.setActivity(this);
		getFragmentManager().beginTransaction().replace(android.R.id.content, settingsFragment).commit();
	}
	
	@SuppressLint("ValidFragment")
	public static class FaimsServerSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener{
		
		private FaimsServerConnectionTestingTask connectionTestingTask;
		private BusyDialog busyDialog;
		private WeakReference<FaimsServerSettingsActivity> activityRef;
		
		public FaimsServerSettingsFragment(){	
		}

		public void setActivity(
				FaimsServerSettingsActivity activity) {
			this.activityRef = new WeakReference<FaimsServerSettingsActivity>(activity);
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			addPreferencesFromResource(R.xml.faims_settings);
			SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
			Preference p = findPreference("pref_server_ip");
			String value = sp.getString("pref_server_ip", null);
			setHost(p, value);
			p = findPreference("pref_server_port");
			value = sp.getString("pref_server_port", null);
			setPort(p, value);
			Preference button = findPreference("button");
			button.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
					String ipAddress = sp.getString("pref_server_ip", null);
					String port = sp.getString("pref_server_port", null);
					if (ipAddress != null || port != null){
						testConnection(sp,ipAddress,port);
					}
					return false;
				}
			});
			Preference demoButton = findPreference("demoButton");
			demoButton.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
					String host = getResources().getString(R.string.demo_server_host);
					String port = getResources().getString(R.string.demo_server_port);
					SharedPreferences.Editor editor = sp.edit();
					editor.putString("pref_server_ip", host);
					editor.putString("pref_server_port", port);
					editor.commit();
					connectToDemoServer(sp, host, port);
					return false;
				}
			});
		}

		protected void testConnection(final SharedPreferences sp, String ipAddress, String port) {
			showBusyTestingConnectionDialog();
			connectionTestingTask = new FaimsServerConnectionTestingTask(ipAddress, port, new ITaskListener() {
				
				@Override
				public void handleTaskCompleted(Object result) {
					busyDialog.dismiss();
					if(result instanceof Boolean){
						if((Boolean)result){
							showWarning("Settings", "Connection test succeeded");
						}else{
							showWarning("Settings", "There is no server available with the provided host and port");
						}
					}
					
				}
			});
			connectionTestingTask.execute();
		}
		
		protected void connectToDemoServer(final SharedPreferences sp, String ipAddress, String port) {
			showBusyTestingConnectionDialog();
			connectionTestingTask = new FaimsServerConnectionTestingTask(ipAddress, port, new ITaskListener() {
				
				@Override
				public void handleTaskCompleted(Object result) {
					busyDialog.dismiss();
					if(result instanceof Boolean){
						if((Boolean)result){
							
							FaimsServerSettingsActivity activity = activityRef.get();
							if (activity != null) {
								Intent fetchModulesIntent = new Intent(activity, FetchModulesActivity.class);
								activity.startActivityForResult(fetchModulesIntent,1);
								activity.finish();
							} else {
								FLog.e("error could not find activity");
								showWarning("Settings", "There was an error trying to perform this action.");
							}
						}else{
							showWarning("Settings", "There is no server available with the provided host and port");
						}
					}
					
				}
			});
			connectionTestingTask.execute();
		}

		private void showBusyTestingConnectionDialog() {
			busyDialog = new BusyDialog(this.getActivity(), 
					"Settings",
					"Connecting to server",
					new IDialogListener() {

						@Override
						public void handleDialogResponse(
								DialogResultCode resultCode) {
							if (resultCode == DialogResultCode.CANCEL) {
								FaimsServerSettingsFragment.this.connectionTestingTask.cancel(true);
							}
						}
				
			});
			busyDialog.show();
	    }
		
		private void showWarning(final String title, final String message){
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
			
			builder.setTitle(title);
			builder.setMessage(message);
			builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			               // User clicked OK button
			           }
			       });
			builder.create().show();
			
		}
		
		@Override
		public void onResume() {
		    super.onResume();
		    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		}

		@Override
		public void onPause() {
		    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		    super.onPause();
		}
		
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			Preference p = findPreference(key);
			String value = sharedPreferences.getString(key, null);
			if("pref_server_ip".equals(key)){
				setHost(p, value);
			}
			if("pref_server_port".equals(key)){
				setPort(p, value);
			}
		}
		
		private void setHost(Preference p, String value) {
			if(value == null || value.isEmpty()){
				p.setSummary("No IP address selected");
			}else{
				p.setSummary("Selected host is : " + value);
			}
		}
		
		private void setPort(Preference p, String value) {
			if(value == null || value.isEmpty()){
				p.setSummary("No port selected");
			}else{
				p.setSummary("Selected port is : " + value);
			}
		}
		
	}
}
