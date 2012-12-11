package au.org.intersect.faims.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import au.org.intersect.faims.android.net.DiscoverServer;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Need to set the application to get state information
        DiscoverServer.getInstance().setApplication(getApplication());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.fetch_project:
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
		Intent fetchProjectsIntent = new Intent(MainActivity.this, FetchProjectsActivity.class);
		MainActivity.this.startActivityForResult(fetchProjectsIntent,1);
	}
    
}
