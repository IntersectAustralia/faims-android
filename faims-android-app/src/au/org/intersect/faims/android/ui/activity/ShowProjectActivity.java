package au.org.intersect.faims.android.ui.activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.javarosa.form.api.FormEntryController;

import roboguice.RoboGuice;
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
import au.org.intersect.faims.android.services.SyncDownloadDatabaseService;
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
	
	private static abstract class ShowProjectActivityHandler extends Handler {
		
		private WeakReference<ShowProjectActivity> activityRef;

		public ShowProjectActivityHandler(ShowProjectActivity activity) {
			this.activityRef = new WeakReference<ShowProjectActivity>(activity);
		}
		
		public void handleMessage(Message message) {
			ShowProjectActivity activity = activityRef.get();
			if (activity == null) {
				Log.d("FAIMS", "ShowProjectActivityHandler cannot get activity");
				return;
			}
			
			handleMessageSafe(activity, message);
		}
		
		public abstract void handleMessageSafe(ShowProjectActivity activity, Message message);
		
	};
	
	private static class DownloadDatabaseHandler extends ShowProjectActivityHandler {

		private String callback;

		public DownloadDatabaseHandler(ShowProjectActivity activity, String callback) {
			super(activity);
			this.callback = callback;
		}

		@Override
		public void handleMessageSafe(ShowProjectActivity activity,
				Message message) {
			activity.busyDialog.dismiss();
			
			FAIMSClientResultCode resultCode = (FAIMSClientResultCode) message.obj;
			if (resultCode == FAIMSClientResultCode.SUCCESS) {
				activity.linker.execute(callback);
			} else {
				activity.showDownloadDatabaseFailureDialog(callback);
			}
		}
		
	}
	
	private static class UploadDatabaseHandler extends ShowProjectActivityHandler {

		private String callback;

		public UploadDatabaseHandler(ShowProjectActivity activity, String callback) {
			super(activity);
			this.callback = callback;
		}

		@Override
		public void handleMessageSafe(ShowProjectActivity activity,
				Message message) {
			activity.busyDialog.dismiss();
			
			FAIMSClientResultCode resultCode = (FAIMSClientResultCode) message.obj;
			if (resultCode == FAIMSClientResultCode.SUCCESS) {
				activity.linker.execute(callback);
			} else {
				activity.showUploadDatabaseFailureDialog(callback);
			}
		}
		
	}
	
	private static class SyncUploadDatabaseHandler extends ShowProjectActivityHandler {

		public SyncUploadDatabaseHandler(ShowProjectActivity activity) {
			super(activity);
		}

		@Override
		public void handleMessageSafe(ShowProjectActivity activity,
				Message message) {
			FAIMSClientResultCode resultCode = (FAIMSClientResultCode) message.obj;
			if (resultCode == FAIMSClientResultCode.SUCCESS) {
				activity.startDownloadSync();
			} else if (resultCode != null) {
				activity.delaySyncInterval();
				activity.waitForNextSync();
				
				activity.callSyncFailure();
			}
			
			activity.isSyncUploading = false;
		}
		
	}
	
	private static class SyncDownloadDatabaseHandler extends ShowProjectActivityHandler {

		public SyncDownloadDatabaseHandler(ShowProjectActivity activity) {
			super(activity);
		}

		@Override
		public void handleMessageSafe(ShowProjectActivity activity,
				Message message) {
			FAIMSClientResultCode resultCode = (FAIMSClientResultCode) message.obj;
			if (resultCode == FAIMSClientResultCode.SUCCESS) {
				activity.resetSyncInterval();
				activity.waitForNextSync();
				
				activity.callSyncSuccess();
			} else if (resultCode != null) {
				activity.delaySyncInterval();
				activity.waitForNextSync();
				
				activity.callSyncFailure();
			}
		
			activity.isSyncDownloading = false;
		}
		
	}
	
	private static class WifiBroadcastReceiver extends BroadcastReceiver {
		
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
	
	private enum SyncState {
		WAIT_FOR_NEXT_SYNC,
		WAIT_FOR_SYNC_END,
		IDLE
	}
	
	// Note: This manager class handle starting the thread from a waiting state
	private class SyncManagerThread extends Thread {
		
		private SyncState state;
		private long lastTime;
		private long syncDelay;
		private boolean isDead;
		private boolean isSleeping;
		
		@Override 
		public void start() {
			wakeup();
			super.start();
		}
		
		@Override
		public void run() {
			try {
				
				while (!isDead) {
				
					while(isSyncing) {
						Log.d("FAIMS", "SyncManager synching");
						
						switch(this.state) {
						case WAIT_FOR_NEXT_SYNC:
							long delta = System.currentTimeMillis() - this.lastTime;
							if (delta > this.syncDelay) {
								ShowProjectActivity.this.startSync();
							}
							break;
						case WAIT_FOR_SYNC_END:
							if (!isSyncUploading && !isSyncDownloading) {
								ShowProjectActivity.this.startSync();
							}
							break;
						default:
							// do nothing
							break;
						}
						
						Thread.sleep(1000);
					}
					
					Log.d("FAIMS", "SyncManager shutdown");
					
					// go into sleep mode
					isSleeping = true;
					while(isSleeping) {
						Thread.yield();
					}
					
				}
				
			} catch (Exception e) {
				Log.e("FAIMS", "SyncManager error", e);
			}
		}
		
		public void waitForNextSync(long delay) {
			this.syncDelay = delay;
			this.lastTime = System.currentTimeMillis();
			this.state = SyncState.WAIT_FOR_NEXT_SYNC;
		}
		
		public void waitForSyncToEnd() {
			this.state = SyncState.WAIT_FOR_SYNC_END;
		}
		
		public void wakeup() {
			this.isSleeping = false;
			this.state = SyncState.IDLE;
		}
		
		public void kill() {
			this.isDead = true;
		}
		
	}
	
	public WifiBroadcastReceiver broadcastReceiver;

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
	
	private boolean isSyncUploading;
	private boolean isSyncDownloading;
	
	private float syncInterval;
	private float syncMinInterval;
	private float syncMaxInterval;
	private float syncDelay;
	
	private List<SyncListener> listeners;

	private SyncManagerThread syncManagerThread;

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
		
		broadcastReceiver = new WifiBroadcastReceiver(ShowProjectActivity.this);
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		registerReceiver(broadcastReceiver, intentFilter);
		
		syncMinInterval = getResources().getInteger(R.integer.sync_min_interval);
		syncMaxInterval = getResources().getInteger(R.integer.sync_max_interval);
		syncDelay = getResources().getInteger(R.integer.sync_failure_delay);
	}
	
	@Override
	protected void onDestroy() {
		if(this.linker != null){
			this.linker.destroyListener();
		}
		if(this.gpsDataManager != null){
			this.gpsDataManager.destroyListener();
		}
		if (this.locateTask != null) {
			this.locateTask.cancel(true);
		}
		if (this.syncManagerThread != null) {
			this.syncManagerThread.kill();
		}
		if (this.broadcastReceiver != null) {
			this.unregisterReceiver(broadcastReceiver);
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
	
	public void downloadDatabaseFromServer(final String callback) {
		FAIMSLog.log();
		
		if (serverDiscovery.isServerHostValid()) {
			showBusyDownloadDatabaseDialog();
			
			// start service
    		Intent intent = new Intent(ShowProjectActivity.this, DownloadDatabaseService.class);
			
    		Project project = ProjectUtil.getProject(projectKey);
    		
    		DownloadDatabaseHandler handler = new DownloadDatabaseHandler(ShowProjectActivity.this, callback);
    		
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
	
	public void uploadDatabaseToServer(final String callback) {
    	FAIMSLog.log();
    	
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyUploadDatabaseDialog();
		    
			// start service
    		Intent intent = new Intent(ShowProjectActivity.this, UploadDatabaseService.class);
			
    		Project project = ProjectUtil.getProject(projectKey);
    		
    		UploadDatabaseHandler handler = new UploadDatabaseHandler(ShowProjectActivity.this, callback);
    		
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
		resetSyncInterval();
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
		
		// locating server
		if (ShowProjectActivity.this.locateTask != null){
			ShowProjectActivity.this.locateTask.cancel(true);
			ShowProjectActivity.this.locateTask = null;
		}
		
		// stop upload sync
		Intent uploadIntent = new Intent(ShowProjectActivity.this, SyncUploadDatabaseService.class);
		ShowProjectActivity.this.stopService(uploadIntent);
		
		// stop download sync
		Intent downloadIntent = new Intent(ShowProjectActivity.this, SyncDownloadDatabaseService.class);
		ShowProjectActivity.this.stopService(downloadIntent);
		
	}
	
	public void startSync() {
		if (!syncEnabled) return;
		
		if (isSyncUploading || isSyncDownloading) {
			waitForSyncToEnd();
		} else {
			Log.d("FAIMS", "starting sync");
			isSyncing = true;
			if (syncManagerThread == null) {
				syncManagerThread = new SyncManagerThread();
				syncManagerThread.start();
			}
			syncManagerThread.wakeup();
			syncLocateServer();	
		}
	}
	
	private void waitForNextSync() {
		if (!syncEnabled) return;
		Log.d("FAIMS", "waiting for sync interval");
		syncManagerThread.waitForNextSync((long) syncInterval * 1000);
	}
	
	private void waitForSyncToEnd() {
		if (!syncEnabled) return;
		Log.d("FAIMS", "waiting for sync to end");
		syncManagerThread.waitForSyncToEnd();
	}
	
	private void syncLocateServer() {
		Log.d("FAIMS", "sync locating server");
		
		if (serverDiscovery.isServerHostValid()) {
			startUploadSync();
		} else {
		
			locateTask = new LocateServerTask(serverDiscovery, new IActionListener() {
				
		    	@Override
		    	public void handleActionResponse(ActionResultCode resultCode,
		    			Object data) {
		    		if (resultCode == ActionResultCode.SUCCESS) {
		    			waitForNextSync(); 
		    		} else {
		    			delaySyncInterval();
		    			waitForNextSync();
		    			
		    			callSyncFailure();
		    		}
		    		
		    	}
		      		
			}).execute();
		}
	}
	
	private void startUploadSync() {
		Log.d("FAIMS", "sync uploading");
		
		// handler must be created on ui thread
		runOnUiThread(new Runnable() {
			
			@Override 
			public void run() {
				// start sync upload service
				Intent intent = new Intent(ShowProjectActivity.this, SyncUploadDatabaseService.class);
						
				Project project = ProjectUtil.getProject(projectKey);
				
				SyncUploadDatabaseHandler handler = new SyncUploadDatabaseHandler(ShowProjectActivity.this);
				
				Messenger messenger = new Messenger(handler);
				intent.putExtra("MESSENGER", messenger);
				intent.putExtra("project", project);
				String userId = databaseManager.getUserId();
				if (userId == null) {
					userId = "0"; // TODO: what should happen if user sets no user?
				}
				intent.putExtra("userId", userId);
				ShowProjectActivity.this.startService(intent);
				
				isSyncUploading = true;
				
				callSyncStart();
			}
		});
	}
	
	private void startDownloadSync() {
		Log.d("FAIMS", "sync downloading");
		
		// handler must be created on ui thread
		runOnUiThread(new Runnable() {
			
			@Override 
			public void run() {
				// start sync download service
				Intent intent = new Intent(ShowProjectActivity.this, SyncDownloadDatabaseService.class);
						
				Project project = ProjectUtil.getProject(projectKey);
				
				SyncDownloadDatabaseHandler handler = new SyncDownloadDatabaseHandler(ShowProjectActivity.this);
				
				Messenger messenger = new Messenger(handler);
				intent.putExtra("MESSENGER", messenger);
				intent.putExtra("project", project);
				ShowProjectActivity.this.startService(intent);
				
				isSyncDownloading = true;
			}
		});
	}
	
	private void resetSyncInterval() {
		syncInterval = syncMinInterval;
	}
	
	private void delaySyncInterval() {
		syncInterval += syncDelay;
		if (syncInterval > syncMaxInterval) 
			syncInterval = syncMaxInterval;
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

	public void setSyncMinInterval(float value) {
		this.syncMinInterval = value;
	}
	
	public void setSyncMaxInterval(float value) {
		this.syncMaxInterval = value;
	}
	
	public void setSyncDelay(float value) {
		this.syncDelay = value;
	}

	public float getSyncMinInterval() {
		return this.syncMinInterval;
	}
	
	public float getSyncMaxInterval(float value) {
		return this.syncMaxInterval;
	}
	
	public float gettSyncDelay(float value) {
		return this.syncDelay;
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
