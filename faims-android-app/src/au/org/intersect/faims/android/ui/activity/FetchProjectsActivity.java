package au.org.intersect.faims.android.ui.activity;

import java.util.List;

import roboguice.activity.RoboActivity;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.IFAIMSClient;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.tasks.ActionResultCode;
import au.org.intersect.faims.android.tasks.ActionType;
import au.org.intersect.faims.android.tasks.DownloadProjectTask;
import au.org.intersect.faims.android.tasks.FetchProjectsListTask;
import au.org.intersect.faims.android.tasks.LocateServerTask;
import au.org.intersect.faims.android.tasks.TaskType;
import au.org.intersect.faims.android.ui.dialog.IFAIMSDialogListener;
import au.org.intersect.faims.android.util.DialogFactory;
import au.org.intersect.faims.android.util.FAIMSLog;

import com.google.inject.Inject;


public class FetchProjectsActivity extends RoboActivity implements IFAIMSDialogListener {
	
	@Inject
	IFAIMSClient faimsClient;
	private ArrayAdapter<String> projectListAdapter;
	
	private LocateServerTask locateTask;
	private FetchProjectsListTask fetchTask;
	private DownloadProjectTask downloadTask;
	
	private TaskType lastTask;
	
	private Dialog choiceDialog;

	private List<Project> projects;
	private Project selectedProject;
	
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
        		
        		choiceDialog = DialogFactory.createChoiceDialog(FetchProjectsActivity.this, 
        				ActionType.CONFIRM_DOWNLOAD_PROJECT, 
        				getString(R.string.confirm_download_project_title),
        				getString(R.string.confirm_download_project_message) + " " + selectedItem + "?");
        		choiceDialog.show();
        	}
        });
        
        fetchProjectsList();
    }
    
    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_fetch_projects, menu);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.fetch_project:
				fetchProjectsFromServer();
				return (true);
			default:
				return (super.onOptionsItemSelected(item));
		}
	}
    */
    
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
    
    private void locateServer(TaskType taskType) {
    	FAIMSLog.log();
    	
    	locateTask = new LocateServerTask(FetchProjectsActivity.this, taskType);
    	locateTask.execute();
    	
    	lastTask = taskType;
    }
    
    /**
     * Fetch projects from the server to load into list
     */
    private void fetchProjectsList() {
    	FAIMSLog.log();
    	
    	if (ServerDiscovery.getInstance().isServerHostValid()) {
    		fetchTask = new FetchProjectsListTask(FetchProjectsActivity.this, faimsClient);
    		fetchTask.execute();
    	} else {
    		locateServer(TaskType.FETCH_PROJECTS_LIST);
    	}
    }
    

    private void downloadProjectArchive() {
    	FAIMSLog.log();
    	
    	if (ServerDiscovery.getInstance().isServerHostValid()) {
    		downloadTask = new DownloadProjectTask(FetchProjectsActivity.this, faimsClient);
    		downloadTask.execute(selectedProject);
    	} else {
    		locateServer(TaskType.DOWNLOAD_PROJECT);
    	}
    	
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public void handleDialogResponse(ActionResultCode resultCode, Object data, ActionType type, Dialog dialog) {
    	FAIMSLog.log("dialog is " + type + " and resultCode is " + resultCode + " and data is " + data);
    	
    	dialog.dismiss();
    	
    	if (type == ActionType.LOCATE_SERVER) {
    		// TODO clear this listener only
    		ServerDiscovery.getInstance().clearListeners();
    		
    		if (resultCode == ActionResultCode.SUCCESS) {
    			
    			TaskType taskType = (TaskType) data;
    			if (taskType == TaskType.FETCH_PROJECTS_LIST)
    				fetchProjectsList();
    			else if (taskType == TaskType.DOWNLOAD_PROJECT)
    				downloadProjectArchive();
    		} else if (resultCode == ActionResultCode.CANCEL){
    			locateTask.cancel(true);
    		} else {
    			showLocateServerFailureDialog();
    		}
    		
    	} else if (type == ActionType.FETCH_PROJECT_LIST) {
    		
    		if (resultCode == ActionResultCode.SUCCESS) {
    			
    			if (projectListAdapter != null) projectListAdapter.clear();
    			this.projects = (List<Project>) data;
    			for (Project p : projects) {
    				this.projectListAdapter.add(p.name);
    			}
    			
    		} else {
    			ServerDiscovery.getInstance().invalidateServerHost();
    			
    			showFetchProjectsFailureDialog();
    		}
    	} else if (type == ActionType.DOWNLOAD_PROJECT) {
    		
    		if (resultCode == ActionResultCode.SUCCESS) {
    			// show project
    			Intent showProjectsIntent = new Intent(FetchProjectsActivity.this, ShowProjectActivity.class);
				showProjectsIntent.putExtra("name", selectedProject.name);
				showProjectsIntent.putExtra("directory", "/faims/projects/" + selectedProject.name.replaceAll("\\s", "_"));
				FetchProjectsActivity.this.startActivityForResult(showProjectsIntent, 1);
				finish();
    		} else if (resultCode == ActionResultCode.CANCEL) {
    			downloadTask.cancel(true);
    		} else {
    			
    			FAIMSClientResultCode errorCode = (FAIMSClientResultCode) data;
    			if (errorCode == FAIMSClientResultCode.STORAGE_LIMIT_ERROR)
    				
    				showDownloadProjectErrorDialog();
    			else {
    				ServerDiscovery.getInstance().invalidateServerHost();
    				
    				showDownloadProjectFailureDialog();
    			}
    		}
    	} else if (type == ActionType.LOCATE_SERVER_FAILURE) {
    		
    		if (resultCode == ActionResultCode.SELECT_YES) {
    			
    			locateServer(lastTask);
    		}
    	} else if (type == ActionType.FETCH_PROJECT_LIST_FAILURE) {
    		
    		if (resultCode == ActionResultCode.SELECT_YES) {
    			fetchProjectsList();
    		}
    	} else if (type == ActionType.DOWNLOAD_PROJECT_FAILURE) {
    		
    		if (resultCode == ActionResultCode.SELECT_YES) {
    			downloadProjectArchive();
    		}
    	} else if (type == ActionType.DOWNLOAD_PROJECT_ERROR) {
    		
    		// does nothing
    	} else if (type == ActionType.CONFIRM_DOWNLOAD_PROJECT) {
    		
    		if (resultCode == ActionResultCode.SELECT_YES) {
    			downloadProjectArchive();
    		}
    	}
    }
    
    private void showLocateServerFailureDialog() {
    	choiceDialog = DialogFactory.createChoiceDialog(FetchProjectsActivity.this,
				ActionType.LOCATE_SERVER_FAILURE,
				getString(R.string.locate_server_failure_title),
				getString(R.string.locate_server_failure_message));
		choiceDialog.show();
    }
    
    private void showFetchProjectsFailureDialog() {
    	choiceDialog = DialogFactory.createChoiceDialog(FetchProjectsActivity.this,
    			ActionType.FETCH_PROJECT_LIST_FAILURE,
				getString(R.string.fetch_projects_failure_title),
				getString(R.string.fetch_projects_failure_message));
		choiceDialog.show();
    }
    
    private void showDownloadProjectFailureDialog() {
    	choiceDialog = DialogFactory.createChoiceDialog(FetchProjectsActivity.this,
    			ActionType.DOWNLOAD_PROJECT_FAILURE,
				getString(R.string.download_project_failure_title),
				getString(R.string.download_project_failure_message));
		choiceDialog.show();
    }
    
    private void showDownloadProjectErrorDialog() {
    	choiceDialog = DialogFactory.createConfirmDialog(FetchProjectsActivity.this,
    			ActionType.DOWNLOAD_PROJECT_ERROR,
				getString(R.string.download_project_error_title),
				getString(R.string.download_project_error_message));
		choiceDialog.show();
    }
    
    private Project getProjectByName(String name) {
    	for (Project project : projects) {
    		if (project.name == name) 
    			return project;
    	}
    	return null;
    }
    
}
