package au.org.intersect.faims.android;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
				showProjectList();
				return (true);
			default:
				return (super.onOptionsItemSelected(item));
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		   if (requestCode == 1) {
		      if (resultCode == RESULT_OK) {
		        String project = data.getStringExtra("PROJECT");
		        
		        TextView tv = (TextView)findViewById(R.id.hello_text);
		        tv.setText("You chose project: " + project);
		      }
		   }
	}
	
	/**
	 * Open a new activity to show a list of projects from the server
	 */
	private void showProjectList(){
		Intent showProjectsIntent = new Intent(MainActivity.this, ShowProjectsActivity.class);
		MainActivity.this.startActivityForResult(showProjectsIntent,1);
	}
    
}
