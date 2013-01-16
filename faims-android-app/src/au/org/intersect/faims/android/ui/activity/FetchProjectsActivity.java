package au.org.intersect.faims.android.ui.activity;

import java.util.List;

import roboguice.activity.RoboActivity;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
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
import au.org.intersect.faims.android.tasks.ActionResultCode;
import au.org.intersect.faims.android.tasks.ActionType;
import au.org.intersect.faims.android.tasks.DownloadProjectTask;
import au.org.intersect.faims.android.tasks.FetchProjectsListTask;
import au.org.intersect.faims.android.tasks.IActionListener;
import au.org.intersect.faims.android.tasks.LocateServerTask;
import au.org.intersect.faims.android.tasks.TaskType;
import au.org.intersect.faims.android.ui.dialog.ChoiceDialog;
import au.org.intersect.faims.android.ui.dialog.ConfirmDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCode;
import au.org.intersect.faims.android.ui.dialog.DialogType;
import au.org.intersect.faims.android.ui.dialog.IDialogListener;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;

import com.google.inject.Inject;

public class FetchProjectsActivity extends RoboActivity implements IActionListener, IDialogListener {
	
	@Inject
	FAIMSClient faimsClient;
	@Inject
	ServerDiscovery serverDiscovery;
	private BluetoothDevice gpsDevice;
	
	private ArrayAdapter<String> projectListAdapter;
	
	private LocateServerTask locateTask;
	private FetchProjectsListTask fetchTask;
	private DownloadProjectTask downloadTask;
	
	protected ChoiceDialog choiceDialog;
	protected ConfirmDialog confirmDialog;
	
	protected List<Project> projects;
	protected Project selectedProject;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FAIMSLog.log();
        
        Intent intent = getIntent();
        this.gpsDevice = intent.getParcelableExtra("gpsDevice");
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
        		
        		choiceDialog = DialogFactory.createChoiceDialog(FetchProjectsActivity.this, 
        				DialogType.CONFIRM_DOWNLOAD_PROJECT, 
        				getString(R.string.confirm_download_project_title),
        				getString(R.string.confirm_download_project_message) + " " + selectedItem + "?");
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

    	// cleanup tasks and dialogs to avoid memory leaks 
    	// note: tasks and dialogs still hold references to activity
    	if (locateTask != null) locateTask.cancel(true);
    	if (fetchTask != null) fetchTask.cancel(true);
    	if (downloadTask != null) downloadTask.cancel(true);
    
    	if (choiceDialog != null) choiceDialog.cleanup();
    	if (confirmDialog != null) confirmDialog.cleanup();
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
    
    /*
    private boolean isLocatingServer() {
    	return locateTask != null && locateTask.getStatus() != AsyncTask.Status.FINISHED;
    }
    
    private boolean isFetchingProjectList() {
    	return fetchTask != null && fetchTask.getStatus() != AsyncTask.Status.FINISHED;
    }
    
    private boolean isDownloadingProject() {
    	return downloadTask != null && downloadTask.getStatus() != AsyncTask.Status.FINISHED;
    }
    */
    
    /**
     * Fetch projects from the server to load into list
     */
    protected void fetchProjectsList() {
    	FAIMSLog.log();
    	
    	locateTask = new LocateServerTask(FetchProjectsActivity.this, TaskType.FETCH_PROJECTS_LIST, serverDiscovery);
    	locateTask.execute();
    }
    
    protected void downloadProjectArchive() {
    	FAIMSLog.log();
    	
    	locateTask = new LocateServerTask(FetchProjectsActivity.this, TaskType.DOWNLOAD_PROJECT, serverDiscovery);
    	locateTask.execute();
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public void handleActionResponse(ActionResultCode resultCode, Object data, ActionType type) {
    	FAIMSLog.log("action is " + type + " and resultCode is " + resultCode + " and data is " + data);
    	
    	if (type == ActionType.LOCATE_SERVER) {
    		
    		if (resultCode == ActionResultCode.SUCCESS) {
    			
    			TaskType taskType = (TaskType) data;
    			if (taskType == TaskType.FETCH_PROJECTS_LIST) {
    				fetchTask = new FetchProjectsListTask(FetchProjectsActivity.this, faimsClient, serverDiscovery);
    				fetchTask.execute();
    			} else if (taskType == TaskType.DOWNLOAD_PROJECT) {
    				downloadTask = new DownloadProjectTask(FetchProjectsActivity.this, selectedProject, faimsClient, serverDiscovery);
    				downloadTask.execute();
    			}
    		} else if (resultCode == ActionResultCode.FAILURE) {
    			showLocateServerFailureDialog();
    		}
    		
    	} else if (type == ActionType.FETCH_PROJECT_LIST) {
    		
    		if (resultCode == ActionResultCode.SUCCESS) {
    			
    			if (projectListAdapter != null) projectListAdapter.clear();
    			this.projects = (List<Project>) data;
    			for (Project p : projects) {
    				this.projectListAdapter.add(p.name);
    			}
    			
    		} else if (resultCode == ActionResultCode.FAILURE) {
    			showFetchProjectsFailureDialog();
    		}
    	} else if (type == ActionType.DOWNLOAD_PROJECT) {
    		
    		if (resultCode == ActionResultCode.SUCCESS) {
    			// show project
    			Intent showProjectsIntent = new Intent(FetchProjectsActivity.this, ShowProjectActivity.class);
				showProjectsIntent.putExtra("name", selectedProject.name);
				showProjectsIntent.putExtra("gpsDevice", this.gpsDevice);
				showProjectsIntent.putExtra("directory", "/faims/projects/" + selectedProject.name.replaceAll("\\s", "_"));
				FetchProjectsActivity.this.startActivityForResult(showProjectsIntent, 1);
				finish();
    		} else if (resultCode == ActionResultCode.FAILURE) {
    			
    			FAIMSClientResultCode errorCode = (FAIMSClientResultCode) data;
    			if (errorCode == FAIMSClientResultCode.STORAGE_LIMIT_ERROR)
    				showDownloadProjectErrorDialog();
    			else {
    				showDownloadProjectFailureDialog();
    			}
    		}
    	}
    }
    
    private void showLocateServerFailureDialog() {
    	choiceDialog = DialogFactory.createChoiceDialog(FetchProjectsActivity.this,
				DialogType.LOCATE_SERVER_FAILURE,
				getString(R.string.locate_server_failure_title),
				getString(R.string.locate_server_failure_message));
		choiceDialog.show();
    }
    
    private void showFetchProjectsFailureDialog() {
    	choiceDialog = DialogFactory.createChoiceDialog(FetchProjectsActivity.this,
    			DialogType.FETCH_PROJECT_LIST_FAILURE,
				getString(R.string.fetch_projects_failure_title),
				getString(R.string.fetch_projects_failure_message));
		choiceDialog.show();
    }
    
    private void showDownloadProjectFailureDialog() {
    	choiceDialog = DialogFactory.createChoiceDialog(FetchProjectsActivity.this,
    			DialogType.DOWNLOAD_PROJECT_FAILURE,
				getString(R.string.download_project_failure_title),
				getString(R.string.download_project_failure_message));
		choiceDialog.show();
    }
    
    private void showDownloadProjectErrorDialog() {
    	confirmDialog = DialogFactory.createConfirmDialog(FetchProjectsActivity.this,
    			DialogType.DOWNLOAD_PROJECT_ERROR,
				getString(R.string.download_project_error_title),
				getString(R.string.download_project_error_message));
    	confirmDialog.show();
    }
    
    private Project getProjectByName(String name) {
    	for (Project project : projects) {
    		if (project.name == name) 
    			return project;
    	}
    	return null;
    }
    
	@Override
	public void handleDialogResponse(DialogResultCode resultCode, Object data,
			DialogType type, Dialog dialog) {
		FAIMSLog.log("dialog is " + type + " and resultCode is " + resultCode + " and data is " + data);
		
		if (type == DialogType.CONFIRM_DOWNLOAD_PROJECT) {
			if (resultCode == DialogResultCode.SELECT_YES) {
				downloadProjectArchive();
			}
		} else if (type == DialogType.BUSY_LOCATING_SERVER) {
			if (resultCode == DialogResultCode.CANCEL) {
				locateTask.cancel(true);
			}
		} else if (type == DialogType.BUSY_FETCHING_PROJECT_LIST) {
			if (resultCode == DialogResultCode.CANCEL) {
				fetchTask.cancel(true);
			}
		} else if (type == DialogType.BUSY_DOWNLOADING_PROJECT) {
			if (resultCode == DialogResultCode.CANCEL) {
				downloadTask.cancel(true);
			}
		} else if (type == DialogType.LOCATE_SERVER_FAILURE) {
			if (resultCode == DialogResultCode.SELECT_YES) {
				fetchProjectsList();
			}
		} else if (type == DialogType.DOWNLOAD_PROJECT_FAILURE) {
			if (resultCode == DialogResultCode.SELECT_YES) {
				downloadProjectArchive();
			}
		} else if (type == DialogType.DOWNLOAD_PROJECT_ERROR) {
			
		}
		
	}
    
}
