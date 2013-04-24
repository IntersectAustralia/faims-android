package au.org.intersect.faims.android.ui.activity;

import java.lang.ref.WeakReference;
import java.util.List;

import roboguice.activity.RoboActivity;
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
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.net.DownloadResult;
import au.org.intersect.faims.android.net.FAIMSClient;
import au.org.intersect.faims.android.net.FAIMSClientErrorCode;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.FetchResult;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.services.DownloadProjectService;
import au.org.intersect.faims.android.tasks.FetchProjectsListTask;
import au.org.intersect.faims.android.tasks.ITaskListener;
import au.org.intersect.faims.android.tasks.LocateServerTask;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.ChoiceDialog;
import au.org.intersect.faims.android.ui.dialog.ConfirmDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCode;
import au.org.intersect.faims.android.ui.dialog.IDialogListener;

import com.google.inject.Inject;

public class FetchProjectsActivity extends RoboActivity {
	
	public static class DownloadProjectHandler extends Handler {
		
		private WeakReference<FetchProjectsActivity> activityRef;

		public DownloadProjectHandler(FetchProjectsActivity activity) {
			this.activityRef = new WeakReference<FetchProjectsActivity>(activity);
		}
		
		public void handleMessage(Message message) {
			FetchProjectsActivity activity = activityRef.get();
			if (activity == null) {
				FLog.d("FetchProjectsHandler cannot get activity");
				return;
			}
			
			activity.busyDialog.dismiss();
			
			DownloadResult result = (DownloadResult) message.obj;
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				// start show project activity
				
				Intent showProjectsIntent = new Intent(activity, ShowProjectActivity.class);
				showProjectsIntent.putExtra("key", activity.selectedProject.key);
				activity.startActivityForResult(showProjectsIntent, 1);
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				if (result.errorCode == FAIMSClientErrorCode.STORAGE_LIMIT_ERROR) {
					activity.showDownloadProjectErrorDialog();
				} else {
					activity.showDownloadProjectFailureDialog();
				}
			} else {
				// ignore
			}
		}
		
	};
	
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
	
	protected final DownloadProjectHandler handler = new DownloadProjectHandler(FetchProjectsActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_fetch_projects);
        
        serverDiscovery.setApplication(getApplication());
        
        ListView projectList = (ListView) findViewById(R.id.project_list);
        
        projectListAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        projectList.setAdapter(projectListAdapter);
        
        projectList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	
        	@Override
        	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        		final String selectedItem = projectListAdapter.getItem(arg2).toString();
        		selectedProject = projects.get(arg2);
        		
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
    protected void onDestroy() {
    	super.onDestroy();
    	
    	if (busyDialog != null)
    		busyDialog.dismiss();
    	if (choiceDialog != null)
    		choiceDialog.dismiss();
    	if (confirmDialog != null)
    		confirmDialog.dismiss();
    	
    	// kill all services
		Intent intent = new Intent(FetchProjectsActivity.this, DownloadProjectService.class);
		stopService(intent);
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
    	
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyFetchingProjectsDialog();
    		
    		fetchTask = new FetchProjectsListTask(faimsClient, new ITaskListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void handleTaskCompleted(Object result) {
					FetchProjectsActivity.this.busyDialog.dismiss();
					
					FetchResult fetchResult = (FetchResult) result;
					
					if (fetchResult.resultCode == FAIMSClientResultCode.SUCCESS) {
						if (projectListAdapter != null) projectListAdapter.clear();
		    			FetchProjectsActivity.this.projects = (List<Project>) fetchResult.data;
		    			for (Project p : projects) {
		    				FetchProjectsActivity.this.projectListAdapter.add(p.name);
		    			}
					} else if (fetchResult.resultCode == FAIMSClientResultCode.FAILURE) {
						showFetchProjectsFailureDialog();
					} else {
						// ignore
					}
				}
    			
    		}).execute();
    	} else {
    		showBusyLocatingServerDialog();
    		
    		locateTask = new LocateServerTask(serverDiscovery, new ITaskListener() {

    			@Override
    			public void handleTaskCompleted(Object result) {
    				FetchProjectsActivity.this.busyDialog.dismiss();
    				
    				if ((Boolean) result) {
    					fetchProjectsList();
    				} else {
    					showLocateServerFetchProjectsFailureDialog();
    				}
    			}
        		
        	}).execute();
    	}
    	
    }
    
	protected void downloadProjectArchive() {
    	
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyDownloadingProjectsDialog();
    		
    		// start service
    		Intent intent = new Intent(FetchProjectsActivity.this, DownloadProjectService.class);
    		
		    Messenger messenger = new Messenger(handler);
		    intent.putExtra("MESSENGER", messenger);
		    intent.putExtra("project", selectedProject);
		    startService(intent);
    	} else {
    		showBusyLocatingServerDialog();
    		
    		locateTask = new LocateServerTask(serverDiscovery, new ITaskListener() {

    			@Override
    			public void handleTaskCompleted(Object result) {
    				FetchProjectsActivity.this.busyDialog.dismiss();
    				
    				if ((Boolean) result) {
    					downloadProjectArchive();
    				} else {
    					showLocateServerDownloadArchiveFailureDialog();
    				}
    			}
        		
        	}).execute();
    	}
    	
    }
    
    private void showLocateServerFetchProjectsFailureDialog() {
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
    
    private void showLocateServerDownloadArchiveFailureDialog() {
    	choiceDialog = new ChoiceDialog(FetchProjectsActivity.this,
				getString(R.string.locate_server_failure_title),
				getString(R.string.locate_server_failure_message),
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
    
}
