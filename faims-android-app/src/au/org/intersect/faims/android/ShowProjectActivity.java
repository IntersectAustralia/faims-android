package au.org.intersect.faims.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ShowProjectActivity extends Activity {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_project);
        
        Intent data = getIntent();
        setTitle(data.getStringExtra("name"));
  	  
  	  	String directory = data.getStringExtra("directory");
  	  	Log.d("debug", "ShowProjectActivity.directory: " + directory);
    }
    
	
}
