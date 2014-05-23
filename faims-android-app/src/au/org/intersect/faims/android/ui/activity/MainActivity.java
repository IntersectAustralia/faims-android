package au.org.intersect.faims.android.ui.activity;

import java.util.List;

import roboguice.activity.RoboActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.ui.dialog.AboutDialog;
import au.org.intersect.faims.android.ui.view.NameValuePair;
import au.org.intersect.faims.android.util.ModuleUtil;

public class MainActivity extends RoboActivity {

	private ArrayAdapter<NameValuePair> moduleListAdapter;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        FAIMSApplication.getInstance().setApplication(getApplication());
        
        setContentView(R.layout.activity_main);
        
        ListView moduleList = (ListView) findViewById(R.id.module_list);
        
        moduleListAdapter = new ArrayAdapter<NameValuePair>(this,android.R.layout.simple_list_item_1);
        moduleList.setAdapter(moduleListAdapter);
        
        moduleList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        		final String selectedItem = moduleListAdapter.getItem(arg2).getValue();
        		
        		Intent showModulesIntent = new Intent(MainActivity.this, ShowModuleActivity.class);
				showModulesIntent.putExtra("key", selectedItem);
				MainActivity.this.startActivityForResult(showModulesIntent, 1);
        	}
        });
        
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	
    	readStoredModules();
    	
    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	if (sp.getInt("launched", 0) == 0) {
    		SharedPreferences.Editor editor = sp.edit();
    		editor.putInt("launched", 1);
    		editor.commit();
    		
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle("Welcome to FAIMS");
    		builder.setMessage("Do you want to download demo module?");

    		builder.setPositiveButton("Yes", new OnClickListener() {
    			
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				fetchModulesFromDemoServer();
    			}
    		});
    		
    		builder.setNegativeButton("No", new OnClickListener() {
    			
    			@Override
    			public void onClick(DialogInterface dialog, int which) {
    				// ignore
    			}
    		});
    		
    		builder.create().show();
    	}
		
    }
    
    private void fetchModulesFromDemoServer() {
    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	String host = getResources().getString(R.string.demo_server_host);
		String port = getResources().getString(R.string.demo_server_port);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("pref_server_ip", host);
		editor.putString("pref_server_port", port);
		editor.commit();
		
		int duration = Toast.LENGTH_LONG;
		Toast toast = Toast.makeText(getApplicationContext(),
				"Select a module to download", duration);
		toast.show();
		
		fetchModulesFromServer();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.fetch_module_list:
				fetchModulesFromServer();
				return (true);
			case R.id.faims_server_setting:
				showFaimsServerSettings();
				return true;
			case R.id.faims_about:
				showFaimsAboutDialog();
				return true;
			default:
				return (super.onOptionsItemSelected(item));
		}
	}
	
	private void showFaimsAboutDialog() {
		AboutDialog about = new AboutDialog(this);
		about.setTitle("About FAIMS");
		about.show();
	}

	private void showFaimsServerSettings() {
		Intent faimsServerIntent = new Intent(MainActivity.this, FaimsServerSettingsActivity.class);
		MainActivity.this.startActivityForResult(faimsServerIntent, 2);
	}

	/**
	 * Open a new activity and shows a list of modules from the server
	 */
	private void fetchModulesFromServer(){
		
		Intent fetchModulesIntent = new Intent(MainActivity.this, FetchModulesActivity.class);
		MainActivity.this.startActivityForResult(fetchModulesIntent,1);
	}
    
	private void readStoredModules() {
		moduleListAdapter.clear();
		List<Module> modules = ModuleUtil.getModules();
		if (modules != null) {
			for (Module p : modules) {
				moduleListAdapter.add(new NameValuePair(p.name,p.key));
			}
		}
	}
	
}
