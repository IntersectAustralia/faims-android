package au.org.intersect.faims.android.ui.activity;

import java.io.File;
import java.io.FilenameFilter;

import roboguice.activity.RoboActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.util.FAIMSLog;

import com.google.inject.Inject;

public class MainActivity extends RoboActivity {

	@Inject
	ServerDiscovery serverDiscovery;
	private ArrayAdapter<String> projectListAdapter;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FAIMSLog.log();
        
        setContentView(R.layout.activity_main);
        
        // Need to set the application to get state information
        serverDiscovery.setApplication(getApplication());
        
        ListView projectList = (ListView) findViewById(R.id.project_list);
        
        projectListAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        projectList.setAdapter(projectListAdapter);
        
        projectList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        		TextView sel = (TextView) arg1;
        		final String selectedItem = sel.getText().toString();
        		
        		Intent showProjectsIntent = new Intent(MainActivity.this, ShowProjectActivity.class);
				showProjectsIntent.putExtra("name", selectedItem);
				showProjectsIntent.putExtra("directory", "/faims/projects/" + selectedItem.replaceAll("\\s", "_"));
				MainActivity.this.startActivityForResult(showProjectsIntent, 1);
        	}
        });
        
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	FAIMSLog.log();
    	
    	readStoredProjects();
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
    
	private void readStoredProjects() {
		File file = new File(Environment.getExternalStorageDirectory() + "/faims/projects");
		if (!file.isDirectory()) return;
		String[] directories = file.list(new FilenameFilter() {

			@Override
			public boolean accept(File file, String arg1) {
				return file.isDirectory();
			}
			
		});
		
		projectListAdapter.clear();
		for (String dir : directories) {
			projectListAdapter.add(dir);
		}
	}
}
