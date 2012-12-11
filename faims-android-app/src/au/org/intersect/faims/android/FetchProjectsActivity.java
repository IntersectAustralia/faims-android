package au.org.intersect.faims.android;

import java.util.ArrayList;
import java.util.List;

import roboguice.activity.RoboActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import au.org.intersect.faims.android.net.FAIMSResponseHandler;
import au.org.intersect.faims.android.net.IFAIMSClient;
import au.org.intersect.faims.android.projects.ProjectInfo;

import com.google.inject.Inject;


public class FetchProjectsActivity extends RoboActivity {
	
	@Inject private IFAIMSClient faimsClient;
	
	private ArrayAdapter<String> projectListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fetch_projects);
        
        ListView projectList = (ListView) findViewById(R.id.project_list);
        
        projectListAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        projectList.setAdapter(projectListAdapter);
        
        /*
        projectList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        		TextView sel = (TextView) arg1;
        		final String selectedItem = sel.getText().toString();
        		new AlertDialog.Builder(FetchProjectsActivity.this).setTitle("Selection Information")
        		                                                  .setMessage("You have selected " + selectedItem)
        		                                                  .setNeutralButton("OK", new DialogInterface.OnClickListener() {
        		                                                                              public void onClick(DialogInterface dialog,
        		                                                                                                  int whichButton) {
        		                                                                            	  Intent intent=new Intent();
        		                                                                            	  intent.putExtra("PROJECT", selectedItem);
        		                                                                            	  setResult(RESULT_OK, intent);
        		                                                                            	  finish();
        		                                                                              }
        		                                                }).show();
        	}
        		    
        });
		*/
        
        fetchProjectList();
    }
    
    /**
     * Fetch projects from the server to load into list
     */
    private void fetchProjectList() {
    	faimsClient.fetchProjectList(new FAIMSResponseHandler<List<ProjectInfo>>() {

    		@Override 
    		public void handleResponse(boolean success, List<ProjectInfo> content) {
    			for(ProjectInfo project : content) {
    	    		projectListAdapter.add(project.name);
    	    	}
    		}
    		
    	});
    }
    
    
}
