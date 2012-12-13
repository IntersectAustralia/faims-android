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
	private boolean busyDialogVisible;
	
	private Dialog downloadDialog;
	private boolean downloadDialogVisible;
	
	private Dialog choiceDialog;

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
        		
        		choiceDialog = DialogCreatorUtil.createChoiceDialog(FetchProjectsActivity.this, 
        				ChoiceDialog.DOWNLOAD_PROJECT, 
        				getString(R.string.download_project_title),
        				getString(R.string.download_project_message) + " " + selectedItem);
        		choiceDialog.show();
        	}
        });
    }
    
    @Override
    protected void onStart() {
    	super.onStart();

    	
        fetchProjectList();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	if (choiceDialog != null) {
    		choiceDialog.dismiss();
    		choiceDialog = null;
    	}
    	
    	if (busyDialog != null) {
    		busyDialog.dismiss();
    		busyDialogVisible = false;
    	}
    	
    	if (downloadDialog != null) {
    		downloadDialog.dismiss();
    		downloadDialogVisible = false;
    	}
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
    			ServerDiscovery.getInstance().clearListeners();
    			requestProjectList();
    		} else {
    			showLocateServerFailureDialog();
    		}
    		
    	} else if (type == ChoiceDialog.SERVER_DISCOVERY_FAILURE) {
    		
    		if (resultCode == ChoiceDialog.YES) {
    			ServerDiscovery.getInstance().invalidateServerHost();
    			fetchProjectList();
    		} 
    	} else if (type == ChoiceDialog.SERVER_REQUEST_FAILURE) {
    		
    		if (resultCode == ChoiceDialog.YES) {
    			ServerDiscovery.getInstance().invalidateServerHost();
    			fetchProjectList();
    		}
    	} else if (type == ChoiceDialog.DOWNLOAD_PROJECT_FAILURE) {
    		
    		if (resultCode == ChoiceDialog.YES) {
    			ServerDiscovery.getInstance().invalidateServerHost();
    			downloadProjectArchive(selectedProject);
    		}
    	} else if (type == ChoiceDialog.DOWNLOAD_PROJECT) {
    		if (resultCode == ChoiceDialog.YES){
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
				Log.d("debug", "addProjects");
				
				dismissBusyDialog();
				
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
    
    private void dismissBusyDialog() {
    	if (busyDialogVisible) {
	    	new Thread(new Runnable() {
	    		
	    		@Override
	    		public void run() {
	    			if (busyDialog != null) {
	    				busyDialog.dismiss();
	    				busyDialogVisible = false;
	    			}
	    		}
	    	}).start();
    	}
    }
    
    private void dismissDownloadDialog() {
    	if (downloadDialogVisible) {
	    	new Thread(new Runnable() {
	    		
	    		@Override
	    		public void run() {
	    			if (downloadDialog != null) {
	    				downloadDialog.dismiss();
	    				downloadDialogVisible = false;
	    			}
	    		}
	    	}).start();
    	}
    }
    
    private void showBusyDialog() {
    	busyDialogVisible = true;
    	
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
    
    private void showDownloadDialog() {
    	downloadDialogVisible = true;
    	
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				downloadDialog = 
						DialogCreatorUtil.createBusyDialog(FetchProjectsActivity.this,
								BusyDialog.TYPE,
								getString(R.string.download_dialog_title), 
								getString(R.string.download_dialog_message));
				downloadDialog.show();
				
			}
		});
    }
    
    private void showServerRequestFailureDialog() {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				choiceDialog = DialogCreatorUtil.createChoiceDialog(FetchProjectsActivity.this,
						ChoiceDialog.SERVER_REQUEST_FAILURE,
						getString(R.string.server_request_failure_title),
						getString(R.string.server_request_failure_message));
				choiceDialog.show();
			}
		});
    }
    
    private void showLocateServerFailureDialog() {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				choiceDialog = DialogCreatorUtil.createChoiceDialog(FetchProjectsActivity.this,
						ChoiceDialog.SERVER_REQUEST_FAILURE,
						getString(R.string.locate_server_failure_title),
						getString(R.string.locate_server_failure_message));
				choiceDialog.show();
			}
		});
    }
    
    private void showDownloadProjectFailureDialog() {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				choiceDialog = DialogCreatorUtil.createChoiceDialog(FetchProjectsActivity.this,
						ChoiceDialog.DOWNLOAD_PROJECT_FAILURE,
						getString(R.string.download_project_failure_title),
						getString(R.string.download_project_failure_message));
				choiceDialog.show();
			}
		});
    }
    
    private void showDownloadErrorDialog() {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				choiceDialog = DialogCreatorUtil.createConfirmDialog(FetchProjectsActivity.this,
						ConfirmDialog.DOWNLOAD_PROJECT_TO_BIG_ERROR,
						getString(R.string.download_project_error_title),
						getString(R.string.download_project_error_message));
				choiceDialog.show();
			}
		});
    }
    
    private void downloadProjectArchive(ProjectInfo project) {
    	Log.d("debug", "downloadProjectArchive");
    	
    	selectedProject = project;
    	
    	if (ServerDiscovery.getInstance().isServerHostValid()) {
    		requestProjectArchive();
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
    
    private void requestProjectArchive() {

    	showDownloadDialog();
    	
    	faimsClient.downloadProjectArchive(selectedProject, new IFAIMSClient.FAIMClientListener<ProjectInfo>() {
			
			@Override 
    		public void handleResponse(int resultCode, ProjectInfo content) {
				
				dismissDownloadDialog();
				
    			if (resultCode == FAIMSClient.SUCCESS) {	
    				
    				Intent showProjectsIntent = new Intent(FetchProjectsActivity.this, ShowProjectActivity.class);
    				showProjectsIntent.putExtra("name", selectedProject.name);
    				showProjectsIntent.putExtra("directory", "/faims/projects/" + selectedProject.name.replaceAll("\\s", "_"));
    				FetchProjectsActivity.this.startActivityForResult(showProjectsIntent, 1);
    				finish();
    				
    			} else if (resultCode == FAIMSClient.DOWNLOAD_TOO_BIG) {
    				showDownloadErrorDialog();
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
