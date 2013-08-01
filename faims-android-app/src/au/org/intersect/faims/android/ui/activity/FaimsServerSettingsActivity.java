package au.org.intersect.faims.android.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.tasks.FaimsServerConnectionTestingTask;
import au.org.intersect.faims.android.tasks.ITaskListener;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCode;
import au.org.intersect.faims.android.ui.dialog.IDialogListener;

public class FaimsServerSettingsActivity extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getFragmentManager().beginTransaction()
        	.replace(android.R.id.content, new FaimsServerSettingsFragment()).commit();
	}
	
	@SuppressLint("ValidFragment")
	private static class FaimsServerSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener{
		
		private FaimsServerConnectionTestingTask connectionTestingTask;
		private BusyDialog busyDialog;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			addPreferencesFromResource(R.xml.faims_settings);
			SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
			Preference p = findPreference("pref_server_ip");
			String value = sp.getString("pref_server_ip", null);
			if(value == null || value.isEmpty()){
				p.setSummary("No IP address selected");
			}else{
				p.setSummary("Selected IP address is : " + value);
			}
			p = findPreference("pref_server_port");
			value = sp.getString("pref_server_port", null);
			if(value == null || value.isEmpty()){
				p.setSummary("No port selected");
			}else{
				p.setSummary("Selected port is : " + value);
			}
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
		}

		protected void testConnection(final SharedPreferences sp, String ipAddress, String port) {
			showBusyTestingConnectionDialog();
			connectionTestingTask = new FaimsServerConnectionTestingTask(ipAddress, port, new ITaskListener() {
				
				@Override
				public void handleTaskCompleted(Object result) {
					busyDialog.dismiss();
					if(result instanceof Boolean){
						if((Boolean)result){
							showWarning("Connection test", "Connection test succeeded");
						}else{
							showWarning("Connection test", "There is no server available with the provided ip address and port");
						}
					}
					
				}
			});
			connectionTestingTask.execute();
		}

		private void showBusyTestingConnectionDialog() {
			busyDialog = new BusyDialog(this.getActivity(), 
					"Connection Testing",
					"Testing connection",
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
				if(value == null || value.isEmpty()){
					p.setSummary("No IP address selected");
				}else{
					p.setSummary("Selected IP address is : " + value);
				}
			}
			if("pref_server_port".equals(key)){
				if(value == null || value.isEmpty()){
					p.setSummary("No port selected");
				}else{
					p.setSummary("Selected port is : " + value);
				}
			}
		}
		
	}
}
