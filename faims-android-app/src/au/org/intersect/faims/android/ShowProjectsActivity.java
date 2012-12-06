package au.org.intersect.faims.android;

import com.google.inject.Inject;

import roboguice.activity.RoboActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import au.org.intersect.faims.android.projects.IProjectUtils;


public class ShowProjectsActivity extends RoboActivity {
	
	@Inject private IProjectUtils projectUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_projects);
        
        ListView projectList = (ListView) findViewById(R.id.project_list);
        ArrayAdapter<String> projectListAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        
        projectList.setAdapter(projectListAdapter);
        
        projectList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        		TextView sel = (TextView) arg1;
        		final String selectedItem = sel.getText().toString();
        		new AlertDialog.Builder(ShowProjectsActivity.this).setTitle("Selection Information")
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

        populateProjectList(projectListAdapter);

    }
    
    /**
     * Call something to get some projects & populate the list
     * @param adapter The array adapter backing the list of projects
     */
    private void populateProjectList(ArrayAdapter<String> adapter){
    	
	    	for(String project : projectUtils.getProjectList()) {
	    		adapter.add(project);
	    	}
    	
    }
    
}
