package au.org.intersect.faims.android.ui.activity;

import roboguice.activity.RoboActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.util.FAIMSLog;
import bsh.EvalError;
import bsh.Interpreter;

import com.google.inject.Inject;

public class MainActivity extends RoboActivity {

	@Inject
	ServerDiscovery serverDiscovery;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FAIMSLog.log();
        
        setContentView(R.layout.activity_main);
        
        // Need to set the application to get state information
        serverDiscovery.setApplication(getApplication());
        
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	FAIMSLog.log();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	FAIMSLog.log();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	FAIMSLog.log();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	FAIMSLog.log();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.fetch_project_list:
				fetchProjectsFromServer();
				return (true);
			default:
				return (super.onOptionsItemSelected(item));
		}
	}
	
	/**
	 * Open a new activity and shows a list of projects from the server
	 */
	private void fetchProjectsFromServer(){
		FAIMSLog.log();
		
		Intent fetchProjectsIntent = new Intent(MainActivity.this, FetchProjectsActivity.class);
		MainActivity.this.startActivityForResult(fetchProjectsIntent,1);
	}
    
}
