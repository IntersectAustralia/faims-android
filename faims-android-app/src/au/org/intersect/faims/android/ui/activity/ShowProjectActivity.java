package au.org.intersect.faims.android.ui.activity;

import org.javarosa.form.api.FormEntryController;

import roboguice.RoboGuice;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.gps.GPSDataManager;
import au.org.intersect.faims.android.managers.DatabaseManager;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.services.DownloadDatabaseService;
import au.org.intersect.faims.android.services.UploadDatabaseService;
import au.org.intersect.faims.android.tasks.ActionResultCode;
import au.org.intersect.faims.android.tasks.IActionListener;
import au.org.intersect.faims.android.tasks.LocateServerTask;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.ChoiceDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCode;
import au.org.intersect.faims.android.ui.dialog.IDialogListener;
import au.org.intersect.faims.android.ui.form.Arch16n;
import au.org.intersect.faims.android.ui.form.BeanShellLinker;
import au.org.intersect.faims.android.ui.form.UIRenderer;
import au.org.intersect.faims.android.util.FAIMSLog;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ProjectUtil;

import com.google.inject.Inject;

public class ShowProjectActivity extends FragmentActivity {

	public static final int CAMERA_REQUEST_CODE = 1;
	
	@Inject
	ServerDiscovery serverDiscovery;
	
	@Inject
	DatabaseManager databaseManager;

	private FormEntryController fem;

	private UIRenderer renderer;

	private BeanShellLinker linker;
	
	private GPSDataManager gpsDataManager;

	protected BusyDialog busyDialog;
	protected ChoiceDialog choiceDialog;
	private AsyncTask<Void, Void, Void> locateTask;

	private Project project;

	private Arch16n arch16n;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FAIMSLog.log();

		setContentView(R.layout.activity_show_project);
		Intent data = getIntent();
		
		project = ProjectUtil.getProject(data.getStringExtra("name"));
		setTitle(project.name);
		
		databaseManager.init(Environment.getExternalStorageDirectory() + "/faims/projects/" + project.dir + "/db.sqlite3");
		gpsDataManager = new GPSDataManager((LocationManager) getSystemService(LOCATION_SERVICE));
		arch16n = new Arch16n(Environment.getExternalStorageDirectory() + "/faims/projects/" + project.dir, project.name);
		
		choiceDialog = new ChoiceDialog(ShowProjectActivity.this,
				getString(R.string.render_project_title),
				getString(R.string.render_project_message), new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							renderUI();
							gpsDataManager.startGPSListener();
						}
					}
			
		});
		choiceDialog.show();
		
		// inject faimsClient and serverDiscovery
		RoboGuice.getBaseApplicationInjector(this.getApplication()).injectMembers(this);
	}
	
	

	@Override
	protected void onDestroy() {
		if(this.linker != null){
			this.linker.destroyListener();
		}
		if(this.gpsDataManager != null){
			this.gpsDataManager.destroyListener();
		}
		super.onDestroy();
	}

	/*
	@Override
	protected void onResume() {
		super.onResume();
		FAIMSLog.log();
		this.manager.dispatchResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		FAIMSLog.log();
		this.manager.dispatchPause(isFinishing());
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		FAIMSLog.log();
		// after taking picture using camera
		if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
			Bitmap photo = (Bitmap) data.getExtras().get("data");
			this.renderer.getCurrentImageView().setImageBitmap(photo);
			this.renderer.clearCurrentImageView();
		}
	}
	*/
	
	protected void renderUI() {
		Log.d("FAIMS", "loading schema: " + Environment
				.getExternalStorageDirectory() + "/faims/projects/" + project.dir + "/ui_schema.xml");
		
		// Read, validate and parse the xforms
		ShowProjectActivity.this.fem = FileUtil.readXmlContent(Environment
				.getExternalStorageDirectory() + "/faims/projects/" + project.dir + "/ui_schema.xml");
		
		arch16n.generatePropertiesMap();

		// render the ui definition
		ShowProjectActivity.this.renderer = new UIRenderer(ShowProjectActivity.this.fem, ShowProjectActivity.this.arch16n, ShowProjectActivity.this);
		ShowProjectActivity.this.renderer.createUI("/faims/projects/" + project.dir);
		ShowProjectActivity.this.renderer.showTabGroup(ShowProjectActivity.this, 0);
		
		// bind the logic to the ui
		Log.d("FAIMS","Binding logic to the UI");
		linker = new BeanShellLinker(ShowProjectActivity.this, ShowProjectActivity.this.arch16n, getAssets(), renderer, databaseManager, gpsDataManager);
		linker.setBaseDir(Environment.getExternalStorageDirectory() + "/faims/projects/" + project.dir);
		linker.sourceFromAssets("ui_commands.bsh");
		linker.execute(FileUtil.readFileIntoString(Environment.getExternalStorageDirectory() + "/faims/projects/" + project.dir + "/ui_logic.bsh"));
	}
	
	public BeanShellLinker getBeanShellLinker(){
		return this.linker;
	}
	
	@SuppressLint("HandlerLeak")
	public void downloadDatabaseFromServer(final String callback) {
		FAIMSLog.log();
		
		if (serverDiscovery.isServerHostValid()) {
			showBusyDownloadDatabaseDialog();
		    
    		// Create a new Messenger for the communication back
    		final Handler handler = new Handler() {
				
				public void handleMessage(Message message) {
					ShowProjectActivity.this.busyDialog.dismiss();
					
					FAIMSClientResultCode resultCode = (FAIMSClientResultCode) message.obj;
					if (resultCode == FAIMSClientResultCode.SUCCESS) {
						linker.execute(callback);
					} else {
						showDownloadDatabaseFailureDialog(callback);
					}
				}
				
			};
			
			// start service
    		Intent intent = new Intent(ShowProjectActivity.this, DownloadDatabaseService.class);
			
	    	Messenger messenger = new Messenger(handler);
		    intent.putExtra("MESSENGER", messenger);
		    intent.putExtra("project", project);
		    ShowProjectActivity.this.startService(intent);
		} else {
			showBusyLocatingServerDialog();
			
			locateTask = new LocateServerTask(serverDiscovery, new IActionListener() {

    			@Override
    			public void handleActionResponse(ActionResultCode resultCode,
    					Object data) {
    				ShowProjectActivity.this.busyDialog.dismiss();
    				
    				if (resultCode == ActionResultCode.FAILURE) {
    					showLocateServerDownloadDatabaseFailureDialog(callback);
    				} else {
    					downloadDatabaseFromServer(callback);
    				}
    			}
        		
        	}).execute();
		}
	}
	
	@SuppressLint("HandlerLeak")
	public void uploadDatabaseToServer(final String callback) {
    	FAIMSLog.log();
    	
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyUploadDatabaseDialog();
		    
    		// Create a new Messenger for the communication back
    		final Handler handler = new Handler() {
				
				public void handleMessage(Message message) {
					ShowProjectActivity.this.busyDialog.dismiss();
					
					FAIMSClientResultCode resultCode = (FAIMSClientResultCode) message.obj;
					if (resultCode == FAIMSClientResultCode.SUCCESS) {
						linker.execute(callback);
					} else {
						showUploadDatabaseFailureDialog(callback);
					}
				}
				
			};
			
			// start service
    		Intent intent = new Intent(ShowProjectActivity.this, UploadDatabaseService.class);
			
	    	// start upload service
	    	// note: the temp file is automatically deleted by the service after it has finished
	    	Messenger messenger = new Messenger(handler);
		    intent.putExtra("MESSENGER", messenger);
		    intent.putExtra("database", Environment.getExternalStorageDirectory() + "/faims/projects/" + project.dir + "/db.sqlite3");
		    intent.putExtra("project", project);
		    intent.putExtra("userId", databaseManager.getUserId());
		    ShowProjectActivity.this.startService(intent);
		   
    	} else {
    		showBusyLocatingServerDialog();
    		
    		locateTask = new LocateServerTask(serverDiscovery, new IActionListener() {

    			@Override
    			public void handleActionResponse(ActionResultCode resultCode,
    					Object data) {
    				ShowProjectActivity.this.busyDialog.dismiss();
    				
    				if (resultCode == ActionResultCode.FAILURE) {
    					showLocateServerUploadDatabaseFailureDialog(callback);
    				} else {
    					uploadDatabaseToServer(callback);
    				}
    			}
        		
        	}).execute();
    	}
    	
    }
	
	private void showLocateServerUploadDatabaseFailureDialog(final String callback) {
    	choiceDialog = new ChoiceDialog(ShowProjectActivity.this,
				getString(R.string.locate_server_failure_title),
				getString(R.string.locate_server_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							uploadDatabaseToServer(callback);
						}
					}
    		
    	});
    	choiceDialog.show();
    }
	
	private void showLocateServerDownloadDatabaseFailureDialog(final String callback) {
    	choiceDialog = new ChoiceDialog(ShowProjectActivity.this,
				getString(R.string.locate_server_failure_title),
				getString(R.string.locate_server_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							downloadDatabaseFromServer(callback);
						}
					}
    		
    	});
    	choiceDialog.show();
    }
	
	private void showBusyLocatingServerDialog() {
    	busyDialog = new BusyDialog(ShowProjectActivity.this, 
				getString(R.string.locate_server_title),
				getString(R.string.locate_server_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							ShowProjectActivity.this.locateTask.cancel(true);
						}
					}
			
		});
		busyDialog.show();
    }
	
	private void showBusyUploadDatabaseDialog() {
    	busyDialog = new BusyDialog(ShowProjectActivity.this, 
				getString(R.string.upload_database_title),
				getString(R.string.upload_database_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							// stop service
				    		Intent intent = new Intent(ShowProjectActivity.this, UploadDatabaseService.class);
				    		
				    		stopService(intent);
						}
					}
			
		});
	    busyDialog.show();
    }
	
	private void showBusyDownloadDatabaseDialog() {
    	busyDialog = new BusyDialog(ShowProjectActivity.this, 
				getString(R.string.download_database_title),
				getString(R.string.download_database_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(
							DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							// stop service
				    		Intent intent = new Intent(ShowProjectActivity.this, DownloadDatabaseService.class);
				    		
				    		stopService(intent);
						}
					}
			
		});
	    busyDialog.show();
    }
	
	private void showUploadDatabaseFailureDialog(final String callback) {
    	choiceDialog = new ChoiceDialog(ShowProjectActivity.this,
				getString(R.string.upload_database_failure_title),
				getString(R.string.upload_database_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							uploadDatabaseToServer(callback);
						}
					}
    		
    	});
    	choiceDialog.show();
    }
	
	private void showDownloadDatabaseFailureDialog(final String callback) {
    	choiceDialog = new ChoiceDialog(ShowProjectActivity.this,
				getString(R.string.download_database_failure_title),
				getString(R.string.download_database_failure_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.SELECT_YES) {
							downloadDatabaseFromServer(callback);
						}
					}
    		
    	});
    	choiceDialog.show();
    }

}
