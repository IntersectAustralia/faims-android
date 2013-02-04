package au.org.intersect.faims.android.ui.activity;

import java.util.List;

import roboguice.activity.RoboActivity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.services.DownloadProjectService;
import au.org.intersect.faims.android.tasks.ActionResultCode;
import au.org.intersect.faims.android.tasks.FetchProjectsListTask;
import au.org.intersect.faims.android.tasks.IActionListener;
import au.org.intersect.faims.android.tasks.LocateServerTask;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.ChoiceDialog;
import au.org.intersect.faims.android.ui.dialog.ConfirmDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCode;
import au.org.intersect.faims.android.ui.dialog.IDialogListener;
import au.org.intersect.faims.android.util.FAIMSLog;

import com.google.inject.Inject;

public class FetchProjectsActivity extends RoboActivity {
	
	@Inject
	FAIMSClient faimsClient;
	@Inject
	ServerDiscovery serverDiscovery;
	
	private ArrayAdapter<String> projectListAdapter;
	
	protected List<Project> projects;
	protected Project selectedProject;
	
	protected BusyDialog busyDialog;
	protected ChoiceDialog choiceDialog;
	protected ConfirmDialog confirmDialog;
	private AsyncTask<Void, Void, Void> locateTask;
	private AsyncTask<Void, Void, Void> fetchTask;
	protected Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FAIMSLog.log();
        
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
        		
        		choiceDialog = new ChoiceDialog(FetchProjectsActivity.this, 
        				getString(R.string.confirm_download_project_title),
        				getString(R.string.confirm_download_project_message) + " " + selectedItem + "?",
        				new IDialogListener() {

							@Override
							public void handleDialogResponse(
									DialogResultCode resultCode) {
								if (resultCode == DialogResultCode.SELECT_YES) {
									downloadProjectArchive();
								}
							}
        			
        		});
        		choiceDialog.show();
        	}
        });
        
        fetchProjectsList();
    }

    @Override
    protected void onStart() {
    	super.onStart();
    	FAIMSLog.log();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	FAIMSLog.log();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	FAIMSLog.log();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	FAIMSLog.log();
    	
    	if (busyDialog.isShowing())
    		busyDialog.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.acitvity_fetch_projects, menu);
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refresh_project_list:
				fetchProjectsList();
				return (true);
			default:
				return (super.onOptionsItemSelected(item));
		}
	}
    
    /**
     * Fetch projects from the server to load into list
     */
    protected void fetchProjectsList() {
    	FAIMSLog.log();
    	
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyFetchingProjectsDialog();
    		
    		fetchTask = new FetchProjectsListTask(faimsClient, new IActionListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void handleActionResponse(ActionResultCode resultCode,
						Object data) {
					FetchProjectsActivity.this.busyDialog.dismiss();
					
					if (resultCode == ActionResultCode.SUCCESS) {
						if (projectListAdapter != null) projectListAdapter.clear();
		    			FetchProjectsActivity.this.projects = (List<Project>) data;
		    			for (Project p : projects) {
		    				FetchProjectsActivity.this.projectListAdapter.add(p.name);
		    			}
					} else {
						showFetchProjectsFailureDialog();
					}
				}
    			
    		}).execute();
    	} else {
    		showBusyLocatingServerDialog();
    		
    		locateTask = new LocateServerTask(serverDiscovery, new IActionListener() {

    			@Override
    			public void handleActionResponse(ActionResultCode resultCode,
    					Object data) {
    				FetchProjectsActivity.this.busyDialog.dismiss();
    				
    				if (resultCode == ActionResultCode.FAILURE) {
    					showLocateServerFailureDialog();
    				} else {
    					fetchProjectsList();
    				}
    			}
        		
        	}).execute();
    	}
    	
    }
    
	@SuppressLint("HandlerLeak")
	protected void downloadProjectArchive() {
    	FAIMSLog.log();
    	
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyDownloadingProjectsDialog();
    		
    		// start service
    		Intent intent = new Intent(FetchProjectsActivity.this, DownloadProjectService.class);
		    // Create a new Messenger for the communication back
    		handler = new Handler() {
				
				public void handleMessage(Message message) {
					FetchProjectsActivity.this.busyDialog.dismiss();
					
					FAIMSClientResultCode resultCode = (FAIMSClientResultCode) message.obj;
					if (resultCode == FAIMSClientResultCode.SUCCESS) {
						// start show project activity
						
						Intent showProjectsIntent = new Intent(FetchProjectsActivity.this, ShowProjectActivity.class);
						showProjectsIntent.putExtra("directory", "/faims/projects/" + selectedProject.name.replaceAll("\\s", "_"));
						FetchProjectsActivity.this.startActivityForResult(showProjectsIntent, 1);
					} else {
						if (resultCode == FAIMSClientResultCode.STORAGE_LIMIT_ERROR) {
							showDownloadProjectErrorDialog();
						} else if (resultCode == FAIMSClientResultCode.SERVER_FAILURE || resultCode == FAIMSClientResultCode.DOWNLOAD_CORRUPTED){
							showDownloadProjectFailureDialog();
						}
					}
				}
				
			};
		    Messenger messenger = new Messenger(handler);
		    intent.putExtra("MESSENGER", messenger);
		    intent.putExtra("project", selectedProject.id);
		    startService(intent);
    	} else {
    		showBusyLocatingServerDialog();
    		
    		locateTask = new LocateServerTask(serverDiscovery, new IActionListener() {

    			@Override
    			public void handleActionResponse(ActionResultCode resultCode,
    					Object data) {
    				FetchProjectsActivity.this.busyDialog.dismiss();
    				
    				if (resultCode == ActionResultCode.FAILURE) {
    					showLocateServerFailureDialog();
    				} else {
    					downloadProjectArchive();
    				}
    			}
        		
        	}).execute();
    	}
    	
    }
    
    private void showLocateServerFailureDialog() {
    	choiceDialog = new ChoiceDialog(FetchProjectsActivity.this,
				getString(R.string.locate_server_failure_title),
				getString(R.string.locate_server_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							fetchProjectsList();
						}
					}
    		
    	});
    	choiceDialog.show();
    }
    
    private void showFetchProjectsFailureDialog() {
    	choiceDialog = new ChoiceDialog(FetchProjectsActivity.this,
				getString(R.string.fetch_projects_failure_title),
				getString(R.string.fetch_projects_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							fetchProjectsList();
						}
					}
    		
    	});
    	choiceDialog.show();
    }
    
    private void showDownloadProjectFailureDialog() {
    	choiceDialog = new ChoiceDialog(FetchProjectsActivity.this,
				getString(R.string.download_project_failure_title),
				getString(R.string.download_project_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							downloadProjectArchive();
						}
					}
    		
    	});
    	choiceDialog.show();
    }
    
    private void showDownloadProjectErrorDialog() {
    	confirmDialog = new ConfirmDialog(FetchProjectsActivity.this,
				getString(R.string.download_project_error_title),
				getString(R.string.download_project_error_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						// do nothing
					}
    		
    	});
    	confirmDialog.show();
    }
    
    private void showBusyLocatingServerDialog() {
    	busyDialog = new BusyDialog(FetchProjectsActivity.this, 
				getString(R.string.locate_server_title),
				getString(R.string.locate_server_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							FetchProjectsActivity.this.locateTask.cancel(true);
						}
					}
			
		});
		busyDialog.show();
    }
    
    private void showBusyFetchingProjectsDialog() {
    	busyDialog = new BusyDialog(FetchProjectsActivity.this, 
				getString(R.string.fetch_projects_title),
				getString(R.string.fetch_projects_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							FetchProjectsActivity.this.fetchTask.cancel(true);
						}
					}
			
		});
		busyDialog.show();
    }
    
    private void showBusyDownloadingProjectsDialog() {
    	busyDialog = new BusyDialog(FetchProjectsActivity.this, 
				getString(R.string.download_project_title),
				getString(R.string.download_project_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							// stop service
				    		Intent intent = new Intent(FetchProjectsActivity.this, DownloadProjectService.class);
				    		
				    		stopService(intent);
						}
					}
			
		});
	    busyDialog.show();
    }
    
    private Project getProjectByName(String name) {
    	for (Project project : projects) {
    		if (project.name == name) 
    			return project;
    	}
    	return null;
    }
    
}
