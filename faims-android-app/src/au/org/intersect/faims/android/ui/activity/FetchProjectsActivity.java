package au.org.intersect.faims.android.ui.activity;

import java.util.List;

import roboguice.activity.RoboActivity;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.net.FAIMSClientResultCodes;
import au.org.intersect.faims.android.net.IFAIMSClient;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.tasks.DownloadProjectTask;
import au.org.intersect.faims.android.tasks.FetchProjectsListTask;
import au.org.intersect.faims.android.tasks.LocateServerTask;
import au.org.intersect.faims.android.tasks.TaskTypes;
import au.org.intersect.faims.android.ui.dialog.DialogResultCodes;
import au.org.intersect.faims.android.ui.dialog.DialogTypes;
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
        				DialogTypes.CONFIRM_DOWNLOAD_PROJECT, 
        				getString(R.string.confirm_download_project_title),
        				getString(R.string.confirm_download_project_message) + " " + selectedItem + "?");
        		choiceDialog.show();
        	}
        });
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
     
    	FAIMSLog.log();
    	
    	if (fetchTask == null || fetchTask.getStatus() == AsyncTask.Status.FINISHED)
    		fetchProjectsList();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	FAIMSLog.log();
    	
    	if (choiceDialog != null) {
    		choiceDialog.dismiss();
    		choiceDialog = null;
    	}
    	
    }
    
    private void locateServer(TaskTypes taskType) {
    	FAIMSLog.log();
    	
    	locateTask = new LocateServerTask(FetchProjectsActivity.this, taskType);
    }
    
    /**
     * Fetch projects from the server to load into list
     */
    private void fetchProjectsList() {
    	FAIMSLog.log();
    	
    	if (ServerDiscovery.getInstance().isServerHostValid()) {
    		fetchTask = new FetchProjectsListTask(FetchProjectsActivity.this, faimsClient);
    	} else {
    		locateServer(TaskTypes.FETCH_PROJECTS_LIST);
    	}
    }
    

    private void downloadProjectArchive() {
    	FAIMSLog.log();
    	
    	if (ServerDiscovery.getInstance().isServerHostValid()) {
    		downloadTask = new DownloadProjectTask(FetchProjectsActivity.this, faimsClient);
    	} else {
    		locateServer(TaskTypes.DOWNLOAD_PROJECT);
    	}
    	
    }
    
    @Override
    public void handleDialogResponse(DialogResultCodes resultCode, Object data, DialogTypes type, Dialog dialog) {
    	FAIMSLog.log("dialog is " + type);
    	
    	dialog.dismiss();
    	
    	if (type == DialogTypes.LOCATE_SERVER) {
    		
    		if (resultCode == DialogResultCodes.SUCCESS) {
    			// TODO: only remove this activity as a listener
    			ServerDiscovery.getInstance().clearListeners();
    			
    			TaskTypes taskType = (TaskTypes) data;
    			if (taskType == TaskTypes.FETCH_PROJECTS_LIST)
    				fetchProjectsList();
    			else if (taskType == TaskTypes.DOWNLOAD_PROJECT)
    				downloadProjectArchive();
    		} else {
    			showLocateServerFailureDialog();
    		}
    		
    	} else if (type == DialogTypes.FETCH_PROJECT_LIST) {
    		
    		if (resultCode == DialogResultCodes.SUCCESS) {
    			ServerDiscovery.getInstance().invalidateServerHost();
    			
    			fetchProjectsList();
    		} else {
    			showFetchProjectsFailureDialog();
    		}
    	} else if (type == DialogTypes.DOWNLOAD_PROJECT) {
    		
    		if (resultCode == DialogResultCodes.SUCCESS) {
    			ServerDiscovery.getInstance().invalidateServerHost();
    			
    			// show project
    			Intent showProjectsIntent = new Intent(FetchProjectsActivity.this, ShowProjectActivity.class);
				showProjectsIntent.putExtra("name", selectedProject.name);
				showProjectsIntent.putExtra("directory", "/faims/projects/" + selectedProject.name.replaceAll("\\s", "_"));
				FetchProjectsActivity.this.startActivityForResult(showProjectsIntent, 1);
				finish();
    		} else {
    			
    			FAIMSClientResultCodes errorCode = (FAIMSClientResultCodes) data;
    			if (errorCode == FAIMSClientResultCodes.STORAGE_LIMIT_ERROR)
    				showDownloadProjectErrorDialog();
    			else
    				showDownloadProjectFailureDialog();
    			
    		}
    	} else if (type == DialogTypes.LOCATE_SERVER_FAILURE) {
    		
    		if (resultCode == DialogResultCodes.SELECT_YES) {
    			
    			locateServer((TaskTypes) data);
    		}
    	} else if (type == DialogTypes.FETCH_PROJECT_LIST_FAILURE) {
    		
    		if (resultCode == DialogResultCodes.SELECT_YES) {
    			fetchProjectsList();
    		}
    	} else if (type == DialogTypes.DOWNLOAD_PROJECT_FAILURE) {
    		
    		if (resultCode == DialogResultCodes.SELECT_YES) {
    			downloadProjectArchive();
    		}
    	} else if (type == DialogTypes.DOWNLOAD_PROJECT_ERROR) {
    		
    		// does nothing
    	} else if (type == DialogTypes.CONFIRM_DOWNLOAD_PROJECT) {
    		
    		if (resultCode == DialogResultCodes.SELECT_YES) {
    			downloadProjectArchive();
    		}
    	}
    }
    
    private void showLocateServerFailureDialog() {
    	choiceDialog = DialogFactory.createChoiceDialog(FetchProjectsActivity.this,
				DialogTypes.LOCATE_SERVER_FAILURE,
				getString(R.string.locate_server_failure_title),
				getString(R.string.locate_server_failure_message));
		choiceDialog.show();
    }
    
    private void showFetchProjectsFailureDialog() {
    	choiceDialog = DialogFactory.createChoiceDialog(FetchProjectsActivity.this,
    			DialogTypes.FETCH_PROJECT_LIST_FAILURE,
				getString(R.string.fetch_projects_failure_title),
				getString(R.string.fetch_projects_failure_message));
		choiceDialog.show();
    }
    
    private void showDownloadProjectFailureDialog() {
    	choiceDialog = DialogFactory.createChoiceDialog(FetchProjectsActivity.this,
    			DialogTypes.DOWNLOAD_PROJECT_FAILURE,
				getString(R.string.download_project_failure_title),
				getString(R.string.download_project_failure_message));
		choiceDialog.show();
    }
    
    private void showDownloadProjectErrorDialog() {
    	choiceDialog = DialogFactory.createConfirmDialog(FetchProjectsActivity.this,
    			DialogTypes.DOWNLOAD_PROJECT_ERROR,
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
