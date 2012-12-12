package au.org.intersect.faims.android;

import java.util.List;

import roboguice.activity.RoboActivity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.IFAIMSClient;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.projects.ProjectInfo;
import au.org.intersect.faims.util.DialogCreatorUtil;

import com.google.inject.Inject;


public class FetchProjectsActivity extends RoboActivity implements IFAIMSDialogListener {
	
	@Inject private IFAIMSClient faimsClient;
	
	private ArrayAdapter<String> projectListAdapter;
	
	private Dialog busyDialog;

	private List<ProjectInfo> projects;
	private ProjectInfo selectedProject;
	
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
        		selectedProject = getProjectByName(selectedItem);
        		
        		ConfirmDialog dialog = DialogCreatorUtil.createConfirmDialog(FetchProjectsActivity.this, 
        				ConfirmDialog.DOWNLOAD_PROJECT, 
        				getString(R.string.download_project_title),
        				getString(R.string.download_project_message) + " " + selectedItem);
        		dialog.show();
        	}
        });
    }
    
    @Override
    protected void onStart() {
    	super.onStart();

        fetchProjectList();
    }
    
    /**
     * Fetch projects from the server to load into list
     */
    private void fetchProjectList() {
    	Log.d("debug", "fetchProjectList");
    	
    	if (ServerDiscovery.getInstance().isServerHostValid()) {
    		requestProjectList();
    	} else {
	    	runOnUiThread(new Runnable() {
	    		
	    		@Override
	    		public void run() {
	    			LocateServerDialog dialog = 
	    					DialogCreatorUtil.createLocateServerDialog(FetchProjectsActivity.this);
	    			dialog.show();
	    		}
	    	});
    	}
    }
    
    @Override
    public void handleDialogResponse(int resultCode, String type, Dialog dialog) {
    	Log.d("debug", "handleDialogResponse: " + String.valueOf(type) + " code : " + String.valueOf(resultCode));
    	dialog.dismiss();
    	
    	if (type == LocateServerDialog.TYPE){
    		
    		if (resultCode == LocateServerDialog.SUCCESS) {
    			requestProjectList();
    		} else {
    			showLocateServerFailureDialog();
    		}
    		
    	} else if (type == ConfirmDialog.SERVER_DISCOVERY_FAILURE) {
    		
    		if (resultCode == ConfirmDialog.YES) {
    			ServerDiscovery.getInstance().invalidateServerHost();
    			fetchProjectList();
    		} 
    	} else if (type == ConfirmDialog.SERVER_REQUEST_FAILURE) {
    		
    		if (resultCode == ConfirmDialog.YES) {
    			ServerDiscovery.getInstance().invalidateServerHost();
    			fetchProjectList();
    		}
    	} else if (type == ConfirmDialog.DOWNLOAD_PROJECT_FAILURE) {
    		
    		if (resultCode == ConfirmDialog.YES) {
    			ServerDiscovery.getInstance().invalidateServerHost();
    			downloadProjectArchive(selectedProject);
    		}
    	} else if (type == ConfirmDialog.DOWNLOAD_PROJECT) {
    		if (resultCode == ConfirmDialog.YES){
    			downloadProjectArchive(selectedProject);	
    		}
    		
    	}
    }
    
    private void requestProjectList() {
    	Log.d("debug", "requestProjectList");
    	
    	showBusyDialog();
    	
		faimsClient.fetchProjectList(new IFAIMSClient.FAIMClientListener<List<ProjectInfo>>() {
			
			@Override 
    		public void handleResponse(int resultCode, List<ProjectInfo> content) {
				
				if (FetchProjectsActivity.this.busyDialog != null) 
					FetchProjectsActivity.this.busyDialog.dismiss();
				
				FetchProjectsActivity.this.projects = content;
				
    			if (resultCode == FAIMSClient.SUCCESS) {	
    				runOnUiThread(new Runnable() {
    					
    					@Override
    					public void run() {
    						projectListAdapter.clear();
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
    }
    
    private void showBusyDialog() {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				busyDialog = 
						DialogCreatorUtil.createBusyDialog(FetchProjectsActivity.this,
								BusyDialog.TYPE,
								getString(R.string.fetch_projects_dialog_title), 
								getString(R.string.fetch_projects_dialog_message));
				busyDialog.show();
			}
		});
    }
    
    private void showServerRequestFailureDialog() {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				ConfirmDialog dialog = DialogCreatorUtil.createConfirmDialog(FetchProjectsActivity.this,
						ConfirmDialog.SERVER_REQUEST_FAILURE,
						getString(R.string.server_request_failure_title),
						getString(R.string.server_request_failure_message));
				dialog.show();
			}
		});
    }
    
    private void showLocateServerFailureDialog() {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				ConfirmDialog dialog = DialogCreatorUtil.createConfirmDialog(FetchProjectsActivity.this,
						ConfirmDialog.SERVER_REQUEST_FAILURE,
						getString(R.string.locate_server_failure_title),
						getString(R.string.locate_server_failure_message));
				dialog.show();
			}
		});
    }
    
    private void showDownloadProjectFailureDialog() {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				ConfirmDialog dialog = DialogCreatorUtil.createConfirmDialog(FetchProjectsActivity.this,
						ConfirmDialog.DOWNLOAD_PROJECT_FAILURE,
						getString(R.string.download_project_failure_title),
						getString(R.string.download_project_failure_message));
				dialog.show();
			}
		});
    }
    
    private void downloadProjectArchive(ProjectInfo project) {
    	Log.d("debug", "downloadProjectArchive");
    	
    	selectedProject = project;
    	
    	showBusyDialog();
    	
    	faimsClient.downloadProjectArchive(selectedProject, new IFAIMSClient.FAIMClientListener<ProjectInfo>() {
			
			@Override 
    		public void handleResponse(int resultCode, ProjectInfo content) {
				
				if (FetchProjectsActivity.this.busyDialog != null) 
					FetchProjectsActivity.this.busyDialog.dismiss();
				
    			if (resultCode == FAIMSClient.SUCCESS) {	
    				
    				Intent showProjectsIntent = new Intent(FetchProjectsActivity.this, ShowProjectActivity.class);
    				FetchProjectsActivity.this.startActivityForResult(showProjectsIntent, 1);
    				finish();
    				
    			} else if (resultCode == FAIMSClient.DOWNLOAD_TOO_BIG) {
    				
    			} else if (resultCode == FAIMSClient.DOWNLOAD_CORRUPTED) {
    				showDownloadProjectFailureDialog();
    			} else {
    				showDownloadProjectFailureDialog();
    			}
    		}
    		
    	});
    	
    }
    
    private ProjectInfo getProjectByName(String name) {
    	for (ProjectInfo project : projects) {
    		if (project.name == name) 
    			return project;
    	}
    	return null;
    }
    
}
