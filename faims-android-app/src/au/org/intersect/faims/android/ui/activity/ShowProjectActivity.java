package au.org.intersect.faims.android.ui.activity;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;
import group.pals.android.lib.ui.filechooser.prefs.DisplayPrefs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.javarosa.form.api.FormEntryController;

import roboguice.RoboGuice;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.IFAIMSRestorable;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.data.ShowProjectActivityData;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.gps.GPSDataManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.FileManager;
import au.org.intersect.faims.android.managers.LockManager;
import au.org.intersect.faims.android.net.DownloadResult;
import au.org.intersect.faims.android.net.FAIMSClientErrorCode;
import au.org.intersect.faims.android.net.FAIMSClientResultCode;
import au.org.intersect.faims.android.net.Result;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.services.DownloadDatabaseService;
import au.org.intersect.faims.android.services.SyncDatabaseService;
import au.org.intersect.faims.android.services.SyncFilesService;
import au.org.intersect.faims.android.services.UploadDatabaseService;
import au.org.intersect.faims.android.tasks.CopyFileTask;
import au.org.intersect.faims.android.tasks.ITaskListener;
import au.org.intersect.faims.android.tasks.LocateServerTask;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.ChoiceDialog;
import au.org.intersect.faims.android.ui.dialog.ConfirmDialog;
import au.org.intersect.faims.android.ui.dialog.DialogResultCode;
import au.org.intersect.faims.android.ui.dialog.IDialogListener;
import au.org.intersect.faims.android.ui.form.Arch16n;
import au.org.intersect.faims.android.ui.form.BeanShellLinker;
import au.org.intersect.faims.android.ui.form.TabGroup;
import au.org.intersect.faims.android.ui.form.UIRenderer;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ProjectUtil;
import au.org.intersect.faims.android.util.SpatialiteUtil;

import com.google.inject.Inject;

public class ShowProjectActivity extends FragmentActivity implements IFAIMSRestorable{
	
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
				FLog.d("ShowProjectActivityHandler cannot get activity");
				return;
			}
			
			handleMessageSafe(activity, message);
		}
		
		public abstract void handleMessageSafe(ShowProjectActivity activity, Message message);
		
	}
	
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
			
			DownloadResult result = (DownloadResult) message.obj;
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				activity.linker.execute(callback);
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				if (result.errorCode == FAIMSClientErrorCode.BUSY_ERROR) {
					activity.showBusyErrorDialog();
				} else if (result.errorCode == FAIMSClientErrorCode.STORAGE_LIMIT_ERROR) {
					activity.showDownloadDatabaseErrorDialog(callback);
				} else {
					activity.showDownloadDatabaseFailureDialog(callback);
				}
			} else {
				// ignore
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
			
			Result result = (Result) message.obj;
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				activity.linker.execute(callback);
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				activity.showUploadDatabaseFailureDialog(callback);
			} else {
				// ignore
			}
		}
		
	}
	
	private static class SyncDatabaseHandler extends ShowProjectActivityHandler {

		public SyncDatabaseHandler(ShowProjectActivity activity) {
			super(activity);
		}

		@Override
		public void handleMessageSafe(ShowProjectActivity activity,
				Message message) {
			Result result = (Result) message.obj;
			if (result.resultCode == FAIMSClientResultCode.SUCCESS){
				if(activity.activityData.isFileSyncEnabled()) {
					activity.startSyncingFiles();
				} else {
					activity.resetSyncInterval();
					activity.waitForNextSync();
					
					activity.callSyncSuccess();
					
					activity.syncLock.release();
				}
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				if (result.errorCode == FAIMSClientErrorCode.BUSY_ERROR) {
					activity.resetSyncInterval();
					activity.waitForNextSync();
					
					activity.callSyncSuccess();
					
					activity.syncLock.release();
				} else {
				
					// failure
					activity.delaySyncInterval();
					activity.waitForNextSync();
					
					activity.callSyncFailure();
					 
					activity.syncLock.release();
				}
			} else {
				// cancelled
				activity.syncLock.release();
			}
		}
	}
		
	private static class SyncFilesHandler extends ShowProjectActivityHandler {

		public SyncFilesHandler(ShowProjectActivity activity) {
			super(activity);
		}

		@Override
		public void handleMessageSafe(ShowProjectActivity activity,
				Message message) {
			Result result = (Result) message.obj;
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				activity.resetSyncInterval();
				activity.waitForNextSync();
				
				activity.callSyncSuccess();
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				if (result.errorCode == FAIMSClientErrorCode.BUSY_ERROR) {
					activity.resetSyncInterval();
					activity.waitForNextSync();
					
					activity.callSyncSuccess();
				} else {
					// failure
					activity.delaySyncInterval();
					activity.waitForNextSync();
					
					activity.callSyncFailure();
				}
			} else {
				// cancelled
			}
			
			activity.syncLock.release();
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
				FLog.d("WifiBroadcastReciever cannot get activity");
				return;
			}
			
		    final String action = intent.getAction();
		    FLog.d("WifiBroadcastReceiver action " + action);
		    
		    if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
		        if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
		        	activity.wifiConnected = true;
		            if (activity.activityData.isSyncEnabled() && activity.isActivityShowing && !activity.syncActive) {
		            	activity.startSync();
		            }
		        } else {
		        	activity.wifiConnected = false;
		        	if (activity.syncActive) {
		        		activity.stopSync();
		            }
		        }
		    }
		}
	}
	
	enum SyncIndicatorColor {
		GREEN,
		ORANGE,
		RED
	}

	public static final int CAMERA_REQUEST_CODE = 1;
	
	public static final int FILE_BROWSER_REQUEST_CODE = 2;
	
	public static final int RASTER_FILE_BROWSER_REQUEST_CODE = 3;
	
	public static final int SPATIAL_FILE_BROWSER_REQUEST_CODE = 4;
	
	@Inject
	ServerDiscovery serverDiscovery;
	
	@Inject
	DatabaseManager databaseManager;
	
	@Inject
	GPSDataManager gpsDataManager;

	private WifiBroadcastReceiver broadcastReceiver;
	
	private FormEntryController fem;

	private UIRenderer renderer;

	private BeanShellLinker linker;

	private BusyDialog busyDialog;
	private ChoiceDialog choiceDialog;
	private ConfirmDialog confirmDialog;
	
	private AsyncTask<Void, Void, Void> locateTask;

	private Arch16n arch16n;

	private String projectKey;
	
	private boolean wifiConnected;
	
	private boolean syncActive;
	
	private float syncInterval;

	private Semaphore syncLock = new Semaphore(1);
	
	private List<SyncListener> listeners;

	private boolean syncIndicatorVisible;

	private SyncIndicatorColor syncIndicatorColor = SyncIndicatorColor.GREEN;
	private SyncIndicatorColor lastSyncIndicatorColor = SyncIndicatorColor.GREEN;

	private boolean isActivityShowing;

	private Timer syncTaskTimer;

	private ShowProjectActivityData activityData;
	private FileManager fm;

	private String projectDir;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_project);
		
		// inject faimsClient and serverDiscovery
		RoboGuice.getBaseApplicationInjector(this.getApplication()).injectMembers(this);
		
		// set file browser to reset last location when activity is created
		DisplayPrefs.setLastLocation(ShowProjectActivity.this, Environment.getExternalStorageDirectory().getAbsolutePath());
		
		// initialize server discovery
		serverDiscovery.setApplication(getApplication());
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        serverDiscovery.initiateServerIPAndPort(preferences);
		// Need to register license for the map view before create an instance of map view
		CustomMapView.registerLicense(getApplicationContext());
		
		this.activityData = new ShowProjectActivityData();
		
		setupSync();
		setupWifiBroadcast();
		setupProject();
		
		renderUI(savedInstanceState);
	}
	
	public GPSDataManager getGPSDataManager() {
		return gpsDataManager;
	}
	
	public DatabaseManager getDatabaseManager() {
		return databaseManager;
	}
	
	public UIRenderer getUIRenderer() {
		return renderer;
	}
	
	public Arch16n getArch16n() {
		return arch16n;
	}
	
	public String getProjectDir() {
		return projectDir;
	}
	
	public FileManager getFileManager() {
		return fm;
	}
	
	private void setupSync() {
		listeners = new ArrayList<SyncListener>();
		activityData.setSyncMinInterval(getResources().getInteger(R.integer.sync_min_interval));
		activityData.setSyncMaxInterval(getResources().getInteger(R.integer.sync_max_interval));
		activityData.setSyncDelay(getResources().getInteger(R.integer.sync_failure_delay));
	}
	
	private void setupWifiBroadcast() {
		broadcastReceiver = new WifiBroadcastReceiver(ShowProjectActivity.this);
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		registerReceiver(broadcastReceiver, intentFilter);
		
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		
		// initialize wifi connection state
		if (mWifi != null && mWifi.isConnected()) {
			wifiConnected = true;
		}
	}
	
	private void setupProject() {
		Intent data = getIntent();
		
		Project project = ProjectUtil.getProject(data.getStringExtra("key"));
		setTitle(project.name);
		
		this.projectKey = project.key;
		this.projectDir = Environment.getExternalStorageDirectory() + FaimsSettings.projectsDir + project.key;
		
		databaseManager.init(projectDir + "/db.sqlite3");
		gpsDataManager.init((LocationManager) getSystemService(LOCATION_SERVICE));
		arch16n = new Arch16n(projectDir, project.name);
		
		SpatialiteUtil.setDatabaseName(projectDir + "/db.sqlite3");
		
		// clear any lock files that may exist
		String lock = projectDir + "/.lock";
		LockManager.clearLock(lock);
		
		fm = new FileManager();
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		restoreFrom(savedInstanceState);
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		FLog.c();
		if(this.linker != null){
			this.linker.stopTrackingGPS();
		}
		if(this.gpsDataManager != null){
			this.gpsDataManager.destroyListener();
		}
		if (this.locateTask != null) {
			this.locateTask.cancel(true);
		}
		if (this.broadcastReceiver != null) {
			this.unregisterReceiver(broadcastReceiver);
		}
		if (activityData.isSyncEnabled()) {
			stopSync();
		}
		if (busyDialog != null) {
			busyDialog.dismiss();
		}
		if (confirmDialog != null) {
			confirmDialog.dismiss();
		}
		if (choiceDialog != null) {
			confirmDialog.dismiss();
		}
		// kill all services
		Intent uploadIntent = new Intent(ShowProjectActivity.this, UploadDatabaseService.class);
		stopService(uploadIntent);
		Intent downloadIntent = new Intent(ShowProjectActivity.this, DownloadDatabaseService.class);
		stopService(downloadIntent);
		super.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		if(fragmentManager.getBackStackEntryCount() > 0){
			TabGroup currentTabGroup = (TabGroup) fragmentManager.findFragmentByTag(fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getName());
			renderer.invalidateListViews(currentTabGroup);
			renderer.setCurrentTabGroup(currentTabGroup);
		}
		super.onBackPressed();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		isActivityShowing = true;
		
		if (activityData.isSyncEnabled()) {
			startSync();
		}
		gpsDataManager.setGpsUpdateInterval(this.activityData.getGpsUpdateInterval());
		if(activityData.isExternalGPSStarted()){
			gpsDataManager.startExternalGPSListener();
		}
		if(activityData.isInternalGPSStarted()){
			gpsDataManager.startInternalGPSListener();
		}
		if(activityData.getTrackingType() != null){
			linker.startTrackingGPS(activityData.getTrackingType(), activityData.getTrackingValue());
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		isActivityShowing = false;
		
		if (syncActive) {
			stopSync();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		// after taking picture using camera
		/*if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
			Bitmap photo = (Bitmap) data.getExtras().get("data");
			this.renderer.getCurrentImageView().setImageBitmap(photo);
			this.renderer.clearCurrentImageView();
		}*/
		switch(requestCode) {
			case FILE_BROWSER_REQUEST_CODE:
			case RASTER_FILE_BROWSER_REQUEST_CODE:
			case SPATIAL_FILE_BROWSER_REQUEST_CODE:
				if (data != null) {
					@SuppressWarnings("unchecked")
					List<LocalFile> files = (List<LocalFile>)
			                data.getSerializableExtra(FileChooserActivity._Results);
					if (files != null && files.size() > 0) {
						fm.selectFile(requestCode, files.get(0));
					}
				}
				break;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    getMenuInflater().inflate(R.menu.activity_show_project, menu);
	    return true;
	}
	
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		MenuItem indicator = menu.findItem(R.id.sync_indicator);
		indicator.setVisible(syncIndicatorVisible);
		switch(syncIndicatorColor) {
		case GREEN:
			indicator.setIcon(getResources().getDrawable(R.drawable.ic_sync_indicator_green));
			break;
		case ORANGE:
			indicator.setIcon(getResources().getDrawable(R.drawable.ic_sync_indicator_orange));
			break;
		default:
			indicator.setIcon(getResources().getDrawable(R.drawable.ic_sync_indicator_red));
			break;
		}
	    return true;
	}
	
	protected void renderUI(Bundle savedInstanceState) {
		try {
			FLog.d("loading schema: " + projectDir + "/ui_schema.xml");
			
			// Read, validate and parse the xforms
			ShowProjectActivity.this.fem = FileUtil.readXmlContent(projectDir + "/ui_schema.xml");
			
			arch16n.generatePropertiesMap();
	
			// render the ui definition
			ShowProjectActivity.this.renderer = new UIRenderer(ShowProjectActivity.this.fem, ShowProjectActivity.this.arch16n, ShowProjectActivity.this);
			ShowProjectActivity.this.renderer.createUI(FaimsSettings.projectsDir + projectKey);
			if(savedInstanceState == null){
				ShowProjectActivity.this.renderer.showTabGroup(ShowProjectActivity.this, 0);
			}
			
			// bind the logic to the ui
			FLog.d("Binding logic to the UI");
			linker = new BeanShellLinker(ShowProjectActivity.this, ProjectUtil.getProject(projectKey));
			linker.sourceFromAssets("ui_commands.bsh");
			linker.execute(FileUtil.readFileIntoString(projectDir + "/ui_logic.bsh"));
		} catch (Exception e) {
			FLog.e("error rendering ui", e);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			
			builder.setTitle(getString(R.string.render_ui_failure_title));
			builder.setMessage(getString(R.string.render_ui_failure_message));
			builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   ShowProjectActivity.this.finish();
			           }
			       });
			builder.create().show();
		}
	}
	
	public BeanShellLinker getBeanShellLinker(){
		return this.linker;
	}
	
	public void downloadDatabaseFromServer(final String callback) {
		
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
			
			locateTask = new LocateServerTask(serverDiscovery, new ITaskListener() {

    			@Override
    			public void handleTaskCompleted(Object result) {
    				ShowProjectActivity.this.busyDialog.dismiss();
    				
    				if ((Boolean) result) {
    					downloadDatabaseFromServer(callback);			
    				} else {
    					showLocateServerDownloadDatabaseFailureDialog(callback);
    				}
    			}
        		
        	}).execute();
		}
	}
	
	public void uploadDatabaseToServer(final String callback) {
    	
    	if (serverDiscovery.isServerHostValid()) {
    		showBusyUploadDatabaseDialog();
		    
			// start service
    		Intent intent = new Intent(ShowProjectActivity.this, UploadDatabaseService.class);
			
    		Project project = ProjectUtil.getProject(projectKey);
    		
    		UploadDatabaseHandler handler = new UploadDatabaseHandler(ShowProjectActivity.this, callback);
    		
	    	// start upload service
	    	Messenger messenger = new Messenger(handler);
		    intent.putExtra("MESSENGER", messenger);
		    intent.putExtra("project", project);
		    intent.putExtra("userId", databaseManager.getUserId());
		    ShowProjectActivity.this.startService(intent);
		   
    	} else {
    		showBusyLocatingServerDialog();
    		
    		locateTask = new LocateServerTask(serverDiscovery, new ITaskListener() {

    			@Override
    			public void handleTaskCompleted(Object result) {
    				ShowProjectActivity.this.busyDialog.dismiss();
    				
    				if ((Boolean) result) {
    					uploadDatabaseToServer(callback);
    				} else {
    					showLocateServerUploadDatabaseFailureDialog(callback);
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
	
	private void showBusyErrorDialog() {
    	confirmDialog = new ConfirmDialog(ShowProjectActivity.this,
				getString(R.string.download_busy_project_error_title),
				getString(R.string.download_busy_project_error_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						// do nothing
					}
    		
    	});
    	confirmDialog.show();
    }
	
	private void showDownloadDatabaseErrorDialog(final String callback) {
    	confirmDialog = new ConfirmDialog(ShowProjectActivity.this,
				getString(R.string.download_database_error_title),
				getString(R.string.download_database_error_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						
					}
    		
    	});
    	confirmDialog.show();
    }

	public void enableSync() {
		if (activityData.isSyncEnabled()) return;
		activityData.setSyncEnabled(true);
		resetSyncInterval();
		startSync();
	}

	public void disableSync() {
		if (!activityData.isSyncEnabled()) return;
		activityData.setSyncEnabled(false);
		stopSync();
	}
	
	public void stopSync() {
		FLog.d("stopping sync");
		
		syncActive = false;
		
		// locating server
		if (ShowProjectActivity.this.locateTask != null){
			ShowProjectActivity.this.locateTask.cancel(true);
			ShowProjectActivity.this.locateTask = null;
			
			syncLock.release();
		}
		
		// stop database sync
		Intent syncDatabaseIntent = new Intent(ShowProjectActivity.this, SyncDatabaseService.class);
		ShowProjectActivity.this.stopService(syncDatabaseIntent);
		
		// stop files sync
		Intent syncFilesIntent = new Intent(ShowProjectActivity.this, SyncFilesService.class);
		ShowProjectActivity.this.stopService(syncFilesIntent);
		
		if (syncTaskTimer != null) {
			syncTaskTimer.cancel();
			syncTaskTimer = null;
		}
		
		if (syncIndicatorColor == SyncIndicatorColor.ORANGE) {
			revertSyncIndicatorColor();
		}
		
		setSyncIndicatorVisible(false);
	}
	
	public void startSync() {
		FLog.d("starting sync");
		
		if (wifiConnected) {
			syncActive = true;
			
			waitForNextSync();
			
			setSyncIndicatorVisible(true);
		} else {
			setSyncIndicatorVisible(false);
			FLog.d("cannot start sync wifi disabled");
		}
	}
	
	private void doSync() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					syncLock.acquire();
					
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							syncLocateServer();
						}
						
					});
				} catch (Exception e) {
					FLog.d("sync error", e);
				}
			}
			
		}).start();
	}
	
	private void waitForNextSync() {
		if (!syncActive) return;
		
		FLog.d("waiting for sync interval");
		
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				doSync();
			}
			
		};
		
		syncTaskTimer = new Timer();
		syncTaskTimer.schedule(task, (long) syncInterval * 1000);
	}
	
	private void syncLocateServer() {
		FLog.d("sync locating server");

		if (serverDiscovery.isServerHostValid()) {
			startSyncingDatabase();
		} else {
		
			locateTask = new LocateServerTask(serverDiscovery, new ITaskListener() {
				
		    	@Override
		    	public void handleTaskCompleted(Object result) {
		    		locateTask = null;
		    		
		    		if ((Boolean) result) {
		    			startSyncingDatabase();
		    		} else {
		    			delaySyncInterval();
		    			waitForNextSync();
		    			
		    			callSyncFailure();

			    		syncLock.release();
		    		}
		    	}
		      		
			}).execute();
		}
	}
	
	private void startSyncingDatabase() {
		FLog.d("start syncing database");
		
		// handler must be created on ui thread
		runOnUiThread(new Runnable() {
			
			@Override 
			public void run() {
				// start sync database service
				Intent intent = new Intent(ShowProjectActivity.this, SyncDatabaseService.class);
						
				Project project = ProjectUtil.getProject(projectKey);
				
				SyncDatabaseHandler handler = new SyncDatabaseHandler(ShowProjectActivity.this);
				
				Messenger messenger = new Messenger(handler);
				intent.putExtra("MESSENGER", messenger);
				intent.putExtra("project", project);
				String userId = databaseManager.getUserId();
				FLog.d("user id : " + userId);
				if (userId == null) {
					userId = "0"; // TODO: what should happen if user sets no user?
				}
				intent.putExtra("userId", userId);
				ShowProjectActivity.this.startService(intent);
				
				callSyncStart();
			}
		});
	}
	
	private void resetSyncInterval() {
		syncInterval = activityData.getSyncMinInterval();
	}
	
	private void delaySyncInterval() {
		syncInterval += activityData.getSyncDelay();
		if (syncInterval > activityData.getSyncMaxInterval()) 
			syncInterval = activityData.getSyncMaxInterval();
	}
	
	public void addSyncListener(SyncListener listener) {
		listeners.add(listener);
	}
	
	public void callSyncStart() {
		for (SyncListener listener : listeners) {
			listener.handleStart();
		}
		
		setSyncIndicatorColor(SyncIndicatorColor.ORANGE);
	}
	
	public void callSyncSuccess() {
		for (SyncListener listener : listeners) {
			listener.handleSuccess();
		}
		
		setSyncIndicatorColor(SyncIndicatorColor.GREEN);
	}
	
	public void callSyncFailure() {
		for (SyncListener listener : listeners) {
			listener.handleFailure();
		}
		
		setSyncIndicatorColor(SyncIndicatorColor.RED);
	}

	public void setSyncMinInterval(float value) {
		activityData.setSyncMinInterval(value);
	}
	
	public void setSyncMaxInterval(float value) {
		activityData.setSyncMaxInterval(value);
	}
	
	public void setSyncDelay(float value) {
		activityData.setSyncDelay(value);
	}

	public float getSyncMinInterval() {
		return activityData.getSyncMinInterval();
	}
	
	public float getSyncMaxInterval(float value) {
		return activityData.getSyncMaxInterval();
	}
	
	public float gettSyncDelay(float value) {
		return activityData.getSyncDelay();
	}
	
	public void showFileBrowser(int requestCode) {
		Intent intent = new Intent(ShowProjectActivity.this, FileChooserActivity.class);
		intent.putExtra(FileChooserActivity._Rootpath, (Parcelable) new LocalFile("/"));
		startActivityForResult(intent, requestCode);
	}
	
	/*
	
	@SuppressWarnings("rawtypes")
	private boolean isServiceRunning(Class c) {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	    	FLog.d(service.service.getClassName());
	        if (c.getName().equals(service.service.getClass().getName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	*/
	
	public void setSyncIndicatorVisible(boolean visible) {
		syncIndicatorVisible = visible;
		this.invalidateOptionsMenu();
	}
	
	private void revertSyncIndicatorColor() {
		syncIndicatorColor = lastSyncIndicatorColor;
		this.invalidateOptionsMenu();
	}
	
	private void setSyncIndicatorColor(SyncIndicatorColor color) {
		lastSyncIndicatorColor = syncIndicatorColor;
		syncIndicatorColor = color;
		this.invalidateOptionsMenu();
	}
	
	public void enableFileSync() {
		activityData.setFileSyncEnabled(true);
	}
	
	public void disableFileSync() {
		activityData.setFileSyncEnabled(false);
	}
	
	private void startSyncingFiles() {
		FLog.d("start syncing files");
		
		// handler must be created on ui thread
		runOnUiThread(new Runnable() {
			
			@Override 
			public void run() {
				// start upload server directory service
				Intent intent = new Intent(ShowProjectActivity.this, SyncFilesService.class);
						
				Project project = ProjectUtil.getProject(projectKey);
				
				SyncFilesHandler handler = new SyncFilesHandler(ShowProjectActivity.this);
				
				Messenger messenger = new Messenger(handler);
				intent.putExtra("MESSENGER", messenger);
				intent.putExtra("project", project);
				ShowProjectActivity.this.startService(intent);

			}
		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		saveTo(outState);
	}

	@Override
	public void saveTo(Bundle savedInstanceState) {
		linker.storeBeanShellData(savedInstanceState);
		linker.stopTrackingGPS();
		renderer.storeBackStack(savedInstanceState,getSupportFragmentManager());
		renderer.storeTabs(savedInstanceState);
		renderer.storeViewValues(savedInstanceState);
		activityData.setExternalGPSStarted(gpsDataManager.isExternalGPSStarted());
		activityData.setInternalGPSStarted(gpsDataManager.isInternalGPSStarted());
		activityData.setUserId(databaseManager.getUserId());
		activityData.saveTo(savedInstanceState);
		gpsDataManager.destroyListener();
	}

	@Override
	public void restoreFrom(Bundle savedInstanceState) {
		linker.restoreBeanShellData(savedInstanceState);
		renderer.restoreBackStack(savedInstanceState, this);
		renderer.restoreTabs(savedInstanceState);
		renderer.restoreViewValues(savedInstanceState);
		activityData.restoreFrom(savedInstanceState);
		gpsDataManager.setGpsUpdateInterval(activityData.getGpsUpdateInterval());
		if(activityData.isExternalGPSStarted()){
			gpsDataManager.startExternalGPSListener();
		}
		if(activityData.isInternalGPSStarted()){
			gpsDataManager.startInternalGPSListener();
		}
		if(activityData.getTrackingType() != null){
			linker.startTrackingGPS(activityData.getTrackingType(), activityData.getTrackingValue());
		}
		this.databaseManager.setUserId(activityData.getUserId());
	}

	// TODO think about what happens if copy fails
	public void copyFile(final String fromFile, final String toFile) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				final String lock = projectDir + "/.lock";
				
				try {
					
					LockManager.waitForLock(lock);
					
					new CopyFileTask(fromFile, toFile, new ITaskListener() {
			
						@Override
						public void handleTaskCompleted(Object result) {
							LockManager.clearLock(lock);
						}
						
					}).execute();
					
				} catch (Exception e) {
					FLog.e("error copying file", e);
				} finally {
					LockManager.clearLock(lock);
				}
			}
			
		}).start();
	}
}
