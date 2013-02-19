package au.org.intersect.faims.android.ui.activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.javarosa.form.api.FormEntryController;

import roboguice.RoboGuice;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
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
import au.org.intersect.faims.android.services.SyncUploadDatabaseService;
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
	
	public interface SyncListener {
		
		public void handleStart();
		public void handleSuccess();
		public void handleFailure();
		
	}
	
	public static class WifiBroadcastReceiver extends BroadcastReceiver {
		
		private WeakReference<ShowProjectActivity> activityRef;

		public WifiBroadcastReceiver(ShowProjectActivity activity) {
			this.activityRef = new WeakReference<ShowProjectActivity>(activity);
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			ShowProjectActivity activity = this.activityRef.get();
			if (activity == null) {
				Log.d("FAIMS", "WifiBroadcastReciever cannot get activity");
				return;
			}
			
		    final String action = intent.getAction();
		    Log.d("FAIMS", "WifiBroadcastReceiver action " + action);
		    
		    if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
		        if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
		            if (!activity.isSyncing && activity.syncEnabled) {
		            	activity.startSync();
		            }
		        } else {
		        	if (activity.isSyncing) {
		        		activity.stopSync();
		            }
		        }
		    }
		}
	}
	
	public WifiBroadcastReceiver broadcastReceiver = new WifiBroadcastReceiver(ShowProjectActivity.this);

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

	private String projectKey;

	private Arch16n arch16n;

	private boolean syncEnabled;
	private boolean isSyncing;
	private int uploadSyncInterval;
	
	private List<SyncListener> listeners;

	private boolean isUploadSyncRunning;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FAIMSLog.log();
		
		// inject faimsClient and serverDiscovery
		RoboGuice.getBaseApplicationInjector(this.getApplication()).injectMembers(this);

		setContentView(R.layout.activity_show_project);
		Intent data = getIntent();
		
		Project project = ProjectUtil.getProject(data.getStringExtra("key"));
		setTitle(project.name);
		
		this.projectKey = project.key;
		
		String projectDir = Environment.getExternalStorageDirectory() + "/faims/projects/" + project.key;
		
		databaseManager.init(projectDir + "/db.sqlite3");
		gpsDataManager = new GPSDataManager((LocationManager) getSystemService(LOCATION_SERVICE));
		arch16n = new Arch16n(projectDir, project.name);
		
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
		
		listeners = new ArrayList<SyncListener>();
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		registerReceiver(broadcastReceiver, intentFilter);
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
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (syncEnabled) {
			startSync();
		}
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		stopSync();
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
		String projectDir = Environment.getExternalStorageDirectory() + "/faims/projects/" + projectKey;
		
		Log.d("FAIMS", "loading schema: " + projectDir + "/ui_schema.xml");
		
		// Read, validate and parse the xforms
		ShowProjectActivity.this.fem = FileUtil.readXmlContent(projectDir + "/ui_schema.xml");
		
		arch16n.generatePropertiesMap();

		// render the ui definition
		ShowProjectActivity.this.renderer = new UIRenderer(ShowProjectActivity.this.fem, ShowProjectActivity.this.arch16n, ShowProjectActivity.this);
		ShowProjectActivity.this.renderer.createUI("/faims/projects/" + projectKey);
		ShowProjectActivity.this.renderer.showTabGroup(ShowProjectActivity.this, 0);
		
		Project project = ProjectUtil.getProject(projectKey);
		
		// bind the logic to the ui
		Log.d("FAIMS","Binding logic to the UI");
		linker = new BeanShellLinker(ShowProjectActivity.this, ShowProjectActivity.this.arch16n, getAssets(), renderer, databaseManager, gpsDataManager, project);
		linker.setBaseDir(projectDir);
		linker.sourceFromAssets("ui_commands.bsh");
		linker.execute(FileUtil.readFileIntoString(projectDir + "/ui_logic.bsh"));
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
			
    		Project project = ProjectUtil.getProject(projectKey);
    		
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
			
    		Project project = ProjectUtil.getProject(projectKey);
    		
	    	// start upload service
	    	// note: the temp file is automatically deleted by the service after it has finished
	    	Messenger messenger = new Messenger(handler);
		    intent.putExtra("MESSENGER", messenger);
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
	
	public void enableSync() {
		if (syncEnabled) return;
		syncEnabled = true;
		startSync();
	}

	public void disableSync() {
		if (!syncEnabled) return;
		syncEnabled = false;
		stopSync();
	}
	
	public void stopSync() {
		Log.d("FAIMS", "stopping sync");
		isSyncing = false;
		stopUploadSync();
	}
	
	public void startSync() {
		if (!syncEnabled) return;
		
		// wait for service to stop before starting
		if (isUploadSyncRunning) {
			delayStartSync();
		} else {
			
			Log.d("FAIMS", "starting sync");
			isSyncing = true;
			resetUploadSyncInterval();
			startUploadSync();
		}
	}
	
	public void delayStartSync() {	
		Log.d("FAIMS", "delaying starting sync");
		if (!syncEnabled) return;
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					while(isUploadSyncRunning) {
						Thread.sleep(1000);
					}
					Thread.sleep(1000);
					ShowProjectActivity.this.startSync();
				} catch (Exception e) {
					Log.e("FAIMS", "Error trying to start sync", e);
				}
			}
			
		}).start();
	}
	
	private void stopUploadSync() {
		Log.d("FAIMS", "stopping upload sync");
		
		Intent intent = new Intent(ShowProjectActivity.this, SyncUploadDatabaseService.class);
		ShowProjectActivity.this.stopService(intent);
	}
	
	private void startUploadSync() {
		if (!syncEnabled) return;
		
		Log.d("FAIMS", "starting upload sync");
		
		// handler needs to be created on ui thread
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				
				startUploadSyncService();
				
			}
			
		});
		
	}
	
	private void delayStartUploadSync() {
		if (!syncEnabled) return;
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					long time = uploadSyncInterval * 1000;
					Log.d("FAIMS", "Waiting for next upload sync in " + time);
					Thread.sleep(time);
					ShowProjectActivity.this.startUploadSync();
				} catch (Exception e) {
					Log.e("FAIMS", "Error waiting for next sync upload", e);
				}
			}
			
		}).start();
	}
	
	@SuppressLint("HandlerLeak")
	private void startUploadSyncService() {
		
		if (serverDiscovery.isServerHostValid()) {
			
			// start sync upload service
					
			// Create a new Messenger for the communication back
			final Handler handler = new Handler() {
						
				public void handleMessage(Message message) {
					FAIMSClientResultCode resultCode = (FAIMSClientResultCode) message.obj;
					
					if (resultCode != null) {
						if (resultCode == FAIMSClientResultCode.SUCCESS) {
							resetUploadSyncInterval();
							
							callSyncSuccess();
						} else {
							delayUploadSyncInterval();
							
							callSyncFailure();
						}
					}
					
					if (isSyncing) {
						delayStartUploadSync();	
					}
					
					isUploadSyncRunning = false;
				}
						
			};
					
			// start service
			Intent intent = new Intent(ShowProjectActivity.this, SyncUploadDatabaseService.class);
					
			Project project = ProjectUtil.getProject(projectKey);
			
			Messenger messenger = new Messenger(handler);
			intent.putExtra("MESSENGER", messenger);
			intent.putExtra("project", project);
			intent.putExtra("userId", "0");
			ShowProjectActivity.this.startService(intent);
			
			callSyncStart();
			
			isUploadSyncRunning = true;
		} else {
			Log.d("FAIMS", "upload sync locating server");
					
			locateTask = new LocateServerTask(serverDiscovery, new IActionListener() {

		    	@Override
		    	public void handleActionResponse(ActionResultCode resultCode,
		    			Object data) {
		    		if (resultCode == ActionResultCode.SUCCESS) {
		    			startUploadSync();
		    		} else {
		    			delayUploadSyncInterval();
		    			delayStartUploadSync();
		    			
		    			callSyncFailure();
		    		}
		    	}
		      		
			}).execute();
		}
	}
	
	private void resetUploadSyncInterval() {
		uploadSyncInterval = getResources().getInteger(R.integer.sync_min_interval);
	}
	
	private void delayUploadSyncInterval() {
		uploadSyncInterval += getResources().getInteger(R.integer.sync_failure_delay);
		int maxInterval = getResources().getInteger(R.integer.sync_max_interval);
		if (uploadSyncInterval > maxInterval) 
			uploadSyncInterval = maxInterval;
	}
	
	public void addSyncListener(SyncListener listener) {
		listeners.add(listener);
	}
	
	public void callSyncStart() {
		for (SyncListener listener : listeners) {
			listener.handleStart();
		}
	}
	
	public void callSyncSuccess() {
		for (SyncListener listener : listeners) {
			listener.handleSuccess();
		}
	}
	
	public void callSyncFailure() {
		for (SyncListener listener : listeners) {
			listener.handleFailure();
		}
	}

	public void restartSync() {
		stopSync();
		startSync();
	}
	
	/*
	@SuppressWarnings("rawtypes")
	private boolean isServiceRunning(Class c) {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	    	Log.d("FAIMS", service.service.getClassName());
	        if (c.getName().equals(service.service.getClass().getName())) {
	            return true;
	        }
	    }
	    return false;
	}
	*/
}
