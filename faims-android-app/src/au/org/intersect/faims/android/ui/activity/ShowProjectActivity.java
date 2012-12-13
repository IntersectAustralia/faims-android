package au.org.intersect.faims.android.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.util.FAIMSLog;

public class ShowProjectActivity extends Activity {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FAIMSLog.log();
        
        setContentView(R.layout.activity_show_project);
        
        Intent data = getIntent();
        setTitle(data.getStringExtra("name"));
  	  
  	  	String directory = data.getStringExtra("directory");
  	  	FAIMSLog.log("directory is " + directory);
    }
    
	
}
