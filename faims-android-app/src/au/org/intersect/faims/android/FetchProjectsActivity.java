package au.org.intersect.faims.android;

import java.util.LinkedList;

import roboguice.activity.RoboActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import au.org.intersect.faims.android.net.IFAIMSClient;
import au.org.intersect.faims.android.projects.ProjectInfo;
import au.org.intersect.faims.util.DialogCreatorUtil;

import com.google.inject.Inject;


public class FetchProjectsActivity extends RoboActivity implements LocateServerDialog.LocateServerDialogListener {
	
	@Inject private IFAIMSClient faimsClient;
	
	private ArrayAdapter<String> projectListAdapter;
	
	private Dialog requestDialog;

	private LinkedList<ProjectInfo> projects;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fetch_projects);
        
        ListView projectList = (ListView) findViewById(R.id.project_list);
        
        projectListAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        projectList.setAdapter(projectListAdapter);
        
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
        		                                                                            	  downloadProjectArchive(projects.get(0));
        		                                                                              }
        		                                                }).show();
        	}
        		    
        });
        
        fetchProjectList();
    }
    
    /**
     * Fetch projects from the server to load into list
     */
    private void fetchProjectList() {
    	Log.d("debug", "fetchProjectList");
    	
    	runOnUiThread(new Runnable() {
    		
    		@Override
    		public void run() {
    			LocateServerDialog dialog = 
    					DialogCreatorUtil.createLocateServerDialog(FetchProjectsActivity.this, 
    							FetchProjectsActivity.this);
    			dialog.show();
    		}
    	});
 
    }
    
    @Override
    public void onServerLocated(boolean success, Dialog dialog) {
    	Log.d("debug", "onServerLocated: " + String.valueOf(success));
    	dialog.dismiss();
    	
    	if (success) {
    		//showServerRequestDialog();
    		
    		faimsClient.fetchProjectList(new IFAIMSClient.FAIMClientListener<LinkedList<ProjectInfo>>() {
    			
    			@Override 
        		public void handleResponse(boolean success, LinkedList<ProjectInfo> content) {
    				
    				if (FetchProjectsActivity.this.requestDialog != null) 
    					FetchProjectsActivity.this.requestDialog.dismiss();
    				
    				FetchProjectsActivity.this.projects = content;
    				
        			if (success) {	
        				runOnUiThread(new Runnable() {
        					
        					@Override
        					public void run() {
        						for(ProjectInfo project : projects) {
        	        	    		projectListAdapter.add(project.name);
        	        	    	}
        	        			
        					}
        				});
        			} else {
        				showServerRequestFailureDialog();
        			}
        		}
        		
        	});
    	} else {
    		showLocateServerFailureDialog();
    	}
    }
    
    private void showServerRequestDialog() {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				requestDialog = 
						DialogCreatorUtil.createServerRequestDialog(FetchProjectsActivity.this, 
								getString(R.string.fetch_projects_dialog_title), 
								getString(R.string.fetch_projects_dialog_message));
				requestDialog.show();
			}
		});
    }
    
    private void showServerRequestFailureDialog() {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				AlertDialog dialog = DialogCreatorUtil.createServerRequestFailureDialog(FetchProjectsActivity.this,
						new DialogInterface.OnClickListener() {
			
							@Override
							public void onClick(DialogInterface dialog, int which) {
								FetchProjectsActivity.this.fetchProjectList();
							}
						});
				dialog.show();
			}
		});
    }
    
    private void showLocateServerFailureDialog() {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				AlertDialog dialog = DialogCreatorUtil.createlocateServerFailureDialog(FetchProjectsActivity.this,
						new DialogInterface.OnClickListener() {
			
							@Override
							public void onClick(DialogInterface dialog, int which) {
								FetchProjectsActivity.this.fetchProjectList();
							}
						});
				dialog.show();
			}
		});
    }
    
    private void downloadProjectArchive(ProjectInfo project) {
    	faimsClient.downloadProjectArchive(new IFAIMSClient.FAIMClientListener<ProjectInfo>() {
			
			@Override 
    		public void handleResponse(boolean success, ProjectInfo content) {
				
				if (FetchProjectsActivity.this.requestDialog != null) 
					FetchProjectsActivity.this.requestDialog.dismiss();
				
    			if (success) {	
    				Log.d("debug", "Downlaoded project archive");
    			} else {
    				showServerRequestFailureDialog();
    			}
    		}
    		
    	}, project);
    }
    
}
