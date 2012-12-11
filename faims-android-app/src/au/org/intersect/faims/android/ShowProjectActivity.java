package au.org.intersect.faims.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ShowProjectActivity extends Activity {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_project);
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	   if (requestCode == 1) {
	      if (resultCode == RESULT_OK) {
	        //String project = data.getStringExtra("PROJECT");
	      }
	   }
	}
	
}
