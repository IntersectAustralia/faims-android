package au.org.intersect.faims.android.ui.activity;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;
import group.pals.android.lib.ui.filechooser.prefs.DisplayPrefs;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.javarosa.form.api.FormEntryController;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.IFAIMSRestorable;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.data.ShowModuleActivityData;
import au.org.intersect.faims.android.database.DatabaseChangeListener;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.gps.GPSDataManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.FileManager;
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
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.view.BeanShellLinker;
import au.org.intersect.faims.android.ui.view.TabGroup;
import au.org.intersect.faims.android.ui.view.UIRenderer;
import au.org.intersect.faims.android.util.Arch16n;
import au.org.intersect.faims.android.util.BitmapUtil;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.MeasurementUtil;
import au.org.intersect.faims.android.util.ModuleUtil;

import com.google.inject.Inject;
import com.nutiteq.utils.UnscaledBitmapLoader;

public class ShowModuleActivity extends FragmentActivity implements
		IFAIMSRestorable {

	public static final String FILES = "files";
	public static final String DATABASE = "database";

	public interface SyncListener {
		public void handleStart();
		public void handleSuccess();
		public void handleFailure();
	}

	public interface AttachFileListener {
		public void handleComplete();
	}

	private static abstract class ShowModuleActivityHandler extends Handler {

		private WeakReference<ShowModuleActivity> activityRef;

		public ShowModuleActivityHandler(ShowModuleActivity activity) {
			this.activityRef = new WeakReference<ShowModuleActivity>(activity);
		}

		public void handleMessage(Message message) {
			ShowModuleActivity activity = activityRef.get();
			if (activity == null) {
				FLog.d("ShowModuleActivityHandler cannot get activity");
				return;
			}

			handleMessageSafe(activity, message);
		}

		public abstract void handleMessageSafe(ShowModuleActivity activity,
				Message message);

	}

	private static class DownloadDatabaseHandler extends
			ShowModuleActivityHandler {

		private String callback;

		public DownloadDatabaseHandler(ShowModuleActivity activity,
				String callback) {
			super(activity);
			this.callback = callback;
		}

		@Override
		public void handleMessageSafe(ShowModuleActivity activity,
				Message message) {
			activity.busyDialog.dismiss();

			Result result = (Result) message.obj;
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

	private static class UploadDatabaseHandler extends
			ShowModuleActivityHandler {

		private String callback;

		public UploadDatabaseHandler(ShowModuleActivity activity,
				String callback) {
			super(activity);
			this.callback = callback;
		}

		@Override
		public void handleMessageSafe(ShowModuleActivity activity,
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

	private static class SyncDatabaseHandler extends ShowModuleActivityHandler {

		public SyncDatabaseHandler(ShowModuleActivity activity) {
			super(activity);
		}

		@Override
		public void handleMessageSafe(ShowModuleActivity activity,
				Message message) {
			Result result = (Result) message.obj;
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				if (activity.activityData.isFileSyncEnabled()) {
					activity.startSyncingFiles();
				} else {
					activity.resetSyncInterval();
					activity.waitForNextSync();

					activity.callSyncSuccess(DATABASE);

					activity.syncLock.release();
				}
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				if (result.errorCode == FAIMSClientErrorCode.BUSY_ERROR) {
					activity.resetSyncInterval();
					activity.waitForNextSync();

					activity.callSyncSuccess(DATABASE);

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

	private static class SyncFilesHandler extends ShowModuleActivityHandler {

		public SyncFilesHandler(ShowModuleActivity activity) {
			super(activity);
		}

		@Override
		public void handleMessageSafe(ShowModuleActivity activity,
				Message message) {
			Result result = (Result) message.obj;
			if (result.resultCode == FAIMSClientResultCode.SUCCESS) {
				activity.resetSyncInterval();
				activity.waitForNextSync();

				activity.callSyncSuccess(FILES);
			} else if (result.resultCode == FAIMSClientResultCode.FAILURE) {
				if (result.errorCode == FAIMSClientErrorCode.BUSY_ERROR) {
					activity.resetSyncInterval();
					activity.waitForNextSync();

					activity.callSyncSuccess(FILES);
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

		private WeakReference<ShowModuleActivity> activityRef;

		public WifiBroadcastReceiver(ShowModuleActivity activity) {
			this.activityRef = new WeakReference<ShowModuleActivity>(activity);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			ShowModuleActivity activity = this.activityRef.get();
			if (activity == null) {
				FLog.d("WifiBroadcastReceiver cannot get activity");
				return;
			}

			if (activity.serverDiscovery.isServerHostFixed()) {
				FLog.d("Ignoring WifiBroadcastReceiver as server host is fixed");
				return;
			}

			final String action = intent.getAction();
			FLog.d("WifiBroadcastReceiver action " + action);

			if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
				if (intent.getBooleanExtra(
						WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
					activity.wifiConnected = true;
					if (activity.activityData.isSyncEnabled()
							&& activity.isActivityShowing
							&& !activity.syncActive) {
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

	public enum SyncStatus {
		ACTIVE_NO_CHANGES, ACTIVE_SYNCING, INACTIVE, ERROR, ACTIVE_HAS_CHANGES;

		public static SyncStatus toSyncStatus(String syncStatusString) {
			return valueOf(syncStatusString);
		}
	}

	public static final int CAMERA_REQUEST_CODE = 1;

	public static final int FILE_BROWSER_REQUEST_CODE = 2;

	public static final int RASTER_FILE_BROWSER_REQUEST_CODE = 3;

	public static final int SPATIAL_FILE_BROWSER_REQUEST_CODE = 4;

	public static final int VIDEO_REQUEST_CODE = 5;
	
	public static final int SCAN_CODE_CODE = 6;

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

	private String moduleKey;

	private boolean wifiConnected;

	private boolean syncActive;

	private float syncInterval;

	private Semaphore syncLock = new Semaphore(1);

	private List<SyncListener> listeners;

	private SyncStatus syncStatus = SyncStatus.INACTIVE;

	private boolean isActivityShowing;

	private Timer syncTaskTimer;

	private ShowModuleActivityData activityData;
	private FileManager fm;

	private String moduleDir;

	private boolean pathIndicatorVisible;

	private float pathDistance;

	private boolean pathValid;

	private BitmapDrawable whiteArrow;

	private BitmapDrawable greyArrow;

	private Bitmap tempBitmap;

	private float pathBearing;

	private Float pathHeading;

	private int pathIndex;

	private int pathLength;

	private boolean delayStopSync;

	private boolean syncStarted = false;

	private Animation rotation;
	private ImageView syncAnimImage;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        FAIMSApplication.getInstance().setApplication(getApplication());

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_show_module);
		
		// inject faimsClient and serverDiscovery
		FAIMSApplication.getInstance().injectMembers(this);

		// Need to register license for the map view before create an instance
		// of map view
		CustomMapView.registerLicense(getApplicationContext());

		this.activityData = new ShowModuleActivityData();

		rotation = AnimationUtils.loadAnimation(this, R.anim.clockwise);
		rotation.setRepeatCount(Animation.INFINITE);

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		syncAnimImage = (ImageView) inflater.inflate(R.layout.rotate, null);

		setupSync();
		setupWifiBroadcast();
		setupModule();
		setProgressBarIndeterminateVisibility(false);

		// set file browser to reset last location when activity is created
		DisplayPrefs.setLastLocation(ShowModuleActivity.this, getModuleDir());

		busyDialog = new BusyDialog(this,
				getString(R.string.load_module_title),
				getString(R.string.load_module_message), null);
		busyDialog.show();

		new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPostExecute(Void result) {
				renderUI(savedInstanceState);
				busyDialog.dismiss();
			}

			@Override
			protected Void doInBackground(Void... params) {
				preRenderUI();
				return null;
			};

		}.execute();
	}

	public UIRenderer getUIRenderer() {
		return renderer;
	}

	public Arch16n getArch16n() {
		return arch16n;
	}

	public Module getModule() {
		return ModuleUtil.getModule(moduleKey);
	}

	public String getModuleDir() {
		return moduleDir;
	}

	public FileManager getFileManager() {
		return fm;
	}

	private void setupSync() {
		listeners = new ArrayList<SyncListener>();
		activityData.setSyncMinInterval(getResources().getInteger(
				R.integer.sync_min_interval));
		activityData.setSyncMaxInterval(getResources().getInteger(
				R.integer.sync_max_interval));
		activityData.setSyncDelay(getResources().getInteger(
				R.integer.sync_failure_delay));
	}

	private void setupWifiBroadcast() {
		broadcastReceiver = new WifiBroadcastReceiver(ShowModuleActivity.this);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		registerReceiver(broadcastReceiver, intentFilter);

		ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		// initialize wifi connection state
		if (mWifi != null && mWifi.isConnected()) {
			wifiConnected = true;
		}
	}

	private void setupModule() {
		Intent data = getIntent();

		Module module = ModuleUtil.getModule(data.getStringExtra("key"));
		setTitle(module.name);

		this.moduleKey = module.key;
		this.moduleDir = Environment.getExternalStorageDirectory()
				+ FaimsSettings.modulesDir + module.key;

		databaseManager.init(module.getDirectoryPath("db.sqlite"));
		databaseManager.addListener(new DatabaseChangeListener() {

			@Override
			public void onDatabaseChange() {
				if (!getSyncStatus().equals(SyncStatus.INACTIVE)) {
					setSyncStatus(SyncStatus.ACTIVE_HAS_CHANGES);
				}
			}

		});

		gpsDataManager.init(
				(LocationManager) getSystemService(LOCATION_SERVICE), this);
		arch16n = new Arch16n(moduleDir, module.name);

		fm = new FileManager();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		restoreFrom(savedInstanceState);
		super.onRestoreInstanceState(savedInstanceState);
		savedInstanceState.clear();
	}

	@Override
	protected void onDestroy() {
		FLog.c();
		if (this.linker != null) {
			this.linker.stopTrackingGPS();
		}
		if (this.gpsDataManager != null) {
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
		Intent uploadIntent = new Intent(ShowModuleActivity.this,
				UploadDatabaseService.class);
		stopService(uploadIntent);
		Intent downloadIntent = new Intent(ShowModuleActivity.this,
				DownloadDatabaseService.class);
		stopService(downloadIntent);
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		if (fragmentManager.getBackStackEntryCount() > 0) {
			TabGroup currentTabGroup = (TabGroup) fragmentManager
					.findFragmentByTag(fragmentManager.getBackStackEntryAt(
							fragmentManager.getBackStackEntryCount() - 1)
							.getName());
			if (currentTabGroup != null) {
				renderer.invalidateListViews(currentTabGroup);
				renderer.setCurrentTabGroup(currentTabGroup);
				getActionBar().setTitle(currentTabGroup.getLabel());
			}
			super.onBackPressed();
		} else {
			if (syncStarted) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Stop Syncing");
				builder.setMessage("Syncing is still in progress. Do you want to exit the activity and stop the sync?");
				builder.setPositiveButton("Yes", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						syncStarted = false;
						stopSync();
						ShowModuleActivity.super.onBackPressed();
					}
				});
				builder.setNegativeButton("No", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Do nothing
					}
				});
				builder.show();
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Exit Module");
				builder.setMessage("Do you want to exit module?");
				builder.setPositiveButton("Yes", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						ShowModuleActivity.super.onBackPressed();
					}
				});
				builder.setNegativeButton("No", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Do nothing
					}
				});
				builder.show();
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		serverDiscovery.initiateServerIPAndPort(preferences);

		isActivityShowing = true;

		if (activityData.isSyncEnabled()) {
			if (syncActive) {
				delayStopSync = false;
			} else {
				startSync();
			}
		}
		if (gpsDataManager.isExternalGPSStarted()) {
			gpsDataManager.startExternalGPSListener();
		}
		if (gpsDataManager.isInternalGPSStarted()) {
			gpsDataManager.startInternalGPSListener();
		}
		if (linker != null && gpsDataManager.isTrackingStarted()) {
			linker.startTrackingGPS(gpsDataManager.getTrackingType(),
					gpsDataManager.getTrackingValue(),
					gpsDataManager.getTrackingExec());
		}
		invalidateOptionsMenu();
	}

	@Override
	protected void onPause() {
		super.onPause();

		isActivityShowing = false;

		if (syncStarted) {
			stopSyncAfterCompletion();
		} else {
			stopSync();
		}

		if (this.linker != null) {
			this.linker.stopTrackingGPSForOnPause();
		}
		if (this.gpsDataManager != null) {
			this.gpsDataManager.destroyListener();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		try {
			if (resultCode == RESULT_CANCELED) {
				FLog.d("result cancelled");
				return;
			}

			switch (requestCode) {
			case FILE_BROWSER_REQUEST_CODE:
			case RASTER_FILE_BROWSER_REQUEST_CODE:
			case SPATIAL_FILE_BROWSER_REQUEST_CODE:
				if (data != null) {
					@SuppressWarnings("unchecked")
					List<LocalFile> files = (List<LocalFile>) data
							.getSerializableExtra(FileChooserActivity._Results);
					if (files != null && files.size() > 0) {
						fm.selectFile(requestCode, files.get(0));
					}
				}
				break;
			case CAMERA_REQUEST_CODE:
				if (resultCode == RESULT_OK) {
					this.linker.executeCameraCallBack();
				}
				break;
			case VIDEO_REQUEST_CODE:
				if (resultCode == RESULT_OK) {
					this.linker.executeVideoCallBack();
				}
				break;
			case SCAN_CODE_CODE:
				if (resultCode == RESULT_OK) {
					String contents = data.getStringExtra("SCAN_RESULT");
					this.linker.setLastScanContents(contents);
					
					this.linker.executeScanCallBack();
				}
			}
		} catch (Exception e) {
			FLog.e("error on activity result", e);
		}
	}

	public String getRealPathFromURI(Uri contentUri) {
		String res = null;
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = getContentResolver().query(contentUri, proj, null,
				null, null);
		if (cursor.moveToFirst()) {
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			res = cursor.getString(column_index);
		}
		cursor.close();
		return res;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_show_module, menu);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		// gps status
		menu.findItem(R.id.action_gps_inactive).setVisible(false);
		menu.findItem(R.id.action_gps_active_has_signal).setVisible(false);
		menu.findItem(R.id.action_gps_active_no_signal).setVisible(false);
		if (gpsDataManager.isExternalGPSStarted()
				|| gpsDataManager.isInternalGPSStarted()) {
			if (gpsDataManager.hasValidExternalGPSSignal()
					|| gpsDataManager.hasValidInternalGPSSignal()) {
				menu.findItem(R.id.action_gps_active_has_signal).setVisible(
						true);
			} else {
				menu.findItem(R.id.action_gps_active_no_signal)
						.setVisible(true);
			}
		} else {
			menu.findItem(R.id.action_gps_inactive).setVisible(true);
		}

		// tracker status
		menu.findItem(R.id.action_tracker_active_no_gps).setVisible(false);
		menu.findItem(R.id.action_tracker_active_has_gps).setVisible(false);
		menu.findItem(R.id.action_tracker_inactive).setVisible(false);
		if (gpsDataManager.isTrackingStarted()) {
			if (gpsDataManager.hasValidExternalGPSSignal()
					|| gpsDataManager.hasValidInternalGPSSignal()) {
				menu.findItem(R.id.action_tracker_active_has_gps).setVisible(
						true);
			} else {
				menu.findItem(R.id.action_tracker_active_no_gps).setVisible(
						true);
			}
		} else {
			menu.findItem(R.id.action_tracker_inactive).setVisible(true);
		}

		// sync status
		menu.findItem(R.id.action_sync).setVisible(false);
		menu.findItem(R.id.action_sync_active).setVisible(false);
		menu.findItem(R.id.action_sync_error).setVisible(false);
		menu.findItem(R.id.action_sync_has_changes).setVisible(false);
		menu.findItem(R.id.action_sync_inactive).setVisible(false);

		syncAnimImage.clearAnimation();

		switch (syncStatus) {
		case ACTIVE_SYNCING:
			MenuItem syncItem = menu.findItem(R.id.action_sync_active)
					.setVisible(true);

			syncAnimImage.startAnimation(rotation);

			syncItem.setActionView(syncAnimImage);

			break;
		case ERROR:
			menu.findItem(R.id.action_sync_error).setVisible(true);
			break;
		case ACTIVE_NO_CHANGES:
			menu.findItem(R.id.action_sync).setVisible(true);
			break;
		case ACTIVE_HAS_CHANGES:
			menu.findItem(R.id.action_sync_has_changes).setVisible(true);
			break;
		default:
			menu.findItem(R.id.action_sync_inactive).setVisible(true);
			break;
		}

		// follow status
		MenuItem distance_text = menu.findItem(R.id.distance_text);
		distance_text.setVisible(pathIndicatorVisible);
		String distanceInfo = pathIndex < 0 ? "" : " to point (" + pathIndex
				+ "/" + pathLength + ")";
		if (pathDistance > 1000) {
			distance_text.setTitle(MeasurementUtil.displayAsKiloMeters(
					pathDistance / 1000, "###,###,###,###.0") + distanceInfo);
		} else {
			distance_text.setTitle(MeasurementUtil.displayAsMeters(
					pathDistance, "###,###,###,###") + distanceInfo);
		}

		MenuItem direction_text = menu.findItem(R.id.direction_text);
		direction_text.setVisible(pathIndicatorVisible);
		direction_text.setTitle(MeasurementUtil.displayAsDegrees(pathBearing,
				"###"));

		MenuItem direction_indicator = menu.findItem(R.id.direction_indicator);
		direction_indicator.setVisible(pathIndicatorVisible);
		if (pathHeading != null) {
			if (tempBitmap != null) {
				tempBitmap.recycle();
			}
			if (whiteArrow == null) {
				whiteArrow = new BitmapDrawable(
						getResources(),
						UnscaledBitmapLoader
								.decodeResource(
										getResources(),
										au.org.intersect.faims.android.R.drawable.white_arrow));
			}
			if (greyArrow == null) {
				greyArrow = new BitmapDrawable(
						getResources(),
						UnscaledBitmapLoader
								.decodeResource(
										getResources(),
										au.org.intersect.faims.android.R.drawable.grey_arrow));
			}

			this.tempBitmap = BitmapUtil.rotateBitmap(
					pathValid ? whiteArrow.getBitmap() : greyArrow.getBitmap(),
					pathBearing - pathHeading);
			direction_indicator.setIcon(new BitmapDrawable(getResources(),
					tempBitmap));
		} else {
			direction_indicator.setVisible(false);
		}

		return true;
	}

	public void updateActionBar() {
		invalidateOptionsMenu();
	}

	public void setPathVisible(boolean value) {
		this.pathIndicatorVisible = value;
	}

	public void setPathDistance(float value) {
		this.pathDistance = value;
	}

	public void setPathIndex(int value, int length) {
		this.pathIndex = value;
		this.pathLength = length;
	}

	public void setPathBearing(float value) {
		this.pathBearing = value;
	}

	public void setPathHeading(Float heading) {
		this.pathHeading = heading;
	}

	public void setPathValid(boolean value) {
		this.pathValid = value;
	}

	protected void preRenderUI() {
		try {
			// Read, validate and parse the xforms
			ShowModuleActivity.this.fem = FileUtil.readXmlContent(moduleDir
					+ "/ui_schema.xml");

			arch16n.generatePropertiesMap();

			// bind the logic to the ui
			FLog.d("Binding logic to the UI");
			linker = new BeanShellLinker(ShowModuleActivity.this,
					ModuleUtil.getModule(moduleKey));
			linker.sourceFromAssets("ui_commands.bsh");
		} catch (Exception e) {
			FLog.e("error pre rendering ui", e);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			builder.setTitle(getString(R.string.render_ui_failure_title));
			builder.setMessage(getString(R.string.render_ui_failure_message));
			builder.setNeutralButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							ShowModuleActivity.this.finish();
						}
					});
			builder.create().show();
		}
	}

	protected void renderUI(Bundle savedInstanceState) {
		try {
			// render the ui definition
			ShowModuleActivity.this.renderer = new UIRenderer(
					ShowModuleActivity.this.fem,
					ShowModuleActivity.this.arch16n, ShowModuleActivity.this);
			ShowModuleActivity.this.renderer.createUI();
			if (savedInstanceState == null) {
				ShowModuleActivity.this.renderer.showTabGroup(
						ShowModuleActivity.this, 0);
			}
			linker.execute(FileUtil.readFileIntoString(moduleDir
					+ "/ui_logic.bsh"));
		} catch (Exception e) {
			FLog.e("error rendering ui", e);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			builder.setTitle(getString(R.string.render_ui_failure_title));
			builder.setMessage(getString(R.string.render_ui_failure_message));
			builder.setNeutralButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							ShowModuleActivity.this.finish();
						}
					});
			builder.create().show();
		}
	}

	public BeanShellLinker getBeanShellLinker() {
		return this.linker;
	}

	public void downloadDatabaseFromServer(final String callback) {

		if (serverDiscovery.isServerHostValid()) {
			showBusyDownloadDatabaseDialog();

			// start service
			Intent intent = new Intent(ShowModuleActivity.this,
					DownloadDatabaseService.class);

			Module module = ModuleUtil.getModule(moduleKey);

			DownloadDatabaseHandler handler = new DownloadDatabaseHandler(
					ShowModuleActivity.this, callback);

			Messenger messenger = new Messenger(handler);
			intent.putExtra("MESSENGER", messenger);
			intent.putExtra("module", module);
			ShowModuleActivity.this.startService(intent);
		} else {
			showBusyLocatingServerDialog();

			locateTask = new LocateServerTask(serverDiscovery,
					new ITaskListener() {

						@Override
						public void handleTaskCompleted(Object result) {
							ShowModuleActivity.this.busyDialog.dismiss();

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
			Intent intent = new Intent(ShowModuleActivity.this,
					UploadDatabaseService.class);

			Module module = ModuleUtil.getModule(moduleKey);

			UploadDatabaseHandler handler = new UploadDatabaseHandler(
					ShowModuleActivity.this, callback);

			// start upload service
			Messenger messenger = new Messenger(handler);
			intent.putExtra("MESSENGER", messenger);
			intent.putExtra("module", module);
			ShowModuleActivity.this.startService(intent);

		} else {
			showBusyLocatingServerDialog();

			locateTask = new LocateServerTask(serverDiscovery,
					new ITaskListener() {

						@Override
						public void handleTaskCompleted(Object result) {
							ShowModuleActivity.this.busyDialog.dismiss();

							if ((Boolean) result) {
								uploadDatabaseToServer(callback);
							} else {
								showLocateServerUploadDatabaseFailureDialog(callback);
							}
						}

					}).execute();
		}

	}

	private void showLocateServerUploadDatabaseFailureDialog(
			final String callback) {
		choiceDialog = new ChoiceDialog(ShowModuleActivity.this,
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

	private void showLocateServerDownloadDatabaseFailureDialog(
			final String callback) {
		choiceDialog = new ChoiceDialog(ShowModuleActivity.this,
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
		busyDialog = new BusyDialog(ShowModuleActivity.this,
				getString(R.string.locate_server_title),
				getString(R.string.locate_server_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							ShowModuleActivity.this.locateTask.cancel(true);
						}
					}

				});
		busyDialog.show();
	}

	private void showBusyUploadDatabaseDialog() {
		busyDialog = new BusyDialog(ShowModuleActivity.this,
				getString(R.string.upload_database_title),
				getString(R.string.upload_database_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							// stop service
							Intent intent = new Intent(ShowModuleActivity.this,
									UploadDatabaseService.class);

							stopService(intent);
						}
					}

				});
		busyDialog.show();
	}

	private void showBusyDownloadDatabaseDialog() {
		busyDialog = new BusyDialog(ShowModuleActivity.this,
				getString(R.string.download_database_title),
				getString(R.string.download_database_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						if (resultCode == DialogResultCode.CANCEL) {
							// stop service
							Intent intent = new Intent(ShowModuleActivity.this,
									DownloadDatabaseService.class);

							stopService(intent);
						}
					}

				});
		busyDialog.show();
	}

	private void showUploadDatabaseFailureDialog(final String callback) {
		choiceDialog = new ChoiceDialog(ShowModuleActivity.this,
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
		choiceDialog = new ChoiceDialog(ShowModuleActivity.this,
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
		confirmDialog = new ConfirmDialog(ShowModuleActivity.this,
				getString(R.string.download_busy_module_error_title),
				getString(R.string.download_busy_module_error_message),
				new IDialogListener() {

					@Override
					public void handleDialogResponse(DialogResultCode resultCode) {
						// do nothing
					}

				});
		confirmDialog.show();
	}

	private void showDownloadDatabaseErrorDialog(final String callback) {
		confirmDialog = new ConfirmDialog(ShowModuleActivity.this,
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
		if (activityData.isSyncEnabled())
			return;
		activityData.setSyncEnabled(true);
		resetSyncInterval();
		startSync();
	}

	public void disableSync() {
		if (!activityData.isSyncEnabled())
			return;
		if (syncStarted) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Stop Syncing");
			builder.setMessage("Syncing is still in progress. Do you want to stop the sync?");
			builder.setPositiveButton("Yes", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					activityData.setSyncEnabled(false);
					syncStarted = false;
					stopSync();
				}
			});
			builder.setNegativeButton("No", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			builder.show();
		} else {
			activityData.setSyncEnabled(false);
			stopSync();
		}
	}

	public void stopSyncAfterCompletion() {
		delayStopSync = true;
	}

	public void stopSync() {
		FLog.d("stopping sync");

		syncActive = false;

		// locating server
		if (ShowModuleActivity.this.locateTask != null) {
			ShowModuleActivity.this.locateTask.cancel(true);
			ShowModuleActivity.this.locateTask = null;

			syncLock.release();
		}

		// stop database sync
		Intent syncDatabaseIntent = new Intent(ShowModuleActivity.this,
				SyncDatabaseService.class);
		ShowModuleActivity.this.stopService(syncDatabaseIntent);

		// stop files sync
		Intent syncFilesIntent = new Intent(ShowModuleActivity.this,
				SyncFilesService.class);
		ShowModuleActivity.this.stopService(syncFilesIntent);

		if (syncTaskTimer != null) {
			syncTaskTimer.cancel();
			syncTaskTimer = null;
		}

		setSyncStatus(SyncStatus.INACTIVE);

	}

	public void startSync() {
		FLog.d("starting sync");

		if (serverDiscovery.isServerHostFixed() || wifiConnected) {
			syncActive = true;

			waitForNextSync();
			try {
				if (hasDatabaseChanges()) {
					setSyncStatus(SyncStatus.ACTIVE_HAS_CHANGES);
				} else {
					setSyncStatus(hasFileChanges() ? SyncStatus.ACTIVE_HAS_CHANGES
							: SyncStatus.ACTIVE_NO_CHANGES);
				}
			} catch (Exception e) {
				FLog.e("error when checking database changes", e);
				setSyncStatus(SyncStatus.ACTIVE_NO_CHANGES);
			}
		} else {
			setSyncStatus(SyncStatus.INACTIVE);
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
		if (!syncActive)
			return;

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

			locateTask = new LocateServerTask(serverDiscovery,
					new ITaskListener() {

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
				Intent intent = new Intent(ShowModuleActivity.this,
						SyncDatabaseService.class);

				Module module = ModuleUtil.getModule(moduleKey);

				SyncDatabaseHandler handler = new SyncDatabaseHandler(
						ShowModuleActivity.this);

				Messenger messenger = new Messenger(handler);
				intent.putExtra("MESSENGER", messenger);
				intent.putExtra("module", module);
				ShowModuleActivity.this.startService(intent);

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
		setSyncStatus(SyncStatus.ACTIVE_SYNCING);
		syncStarted = true;
	}

	public void callSyncSuccess(String type) {
		for (SyncListener listener : listeners) {
			listener.handleSuccess();
		}
		syncStarted = false;

		if (DATABASE.equals(type)) {
			try {
				if (hasDatabaseChanges()) {
					setSyncStatus(SyncStatus.ACTIVE_HAS_CHANGES);
				}
			} catch (Exception e) {
				FLog.e("error when checking database changes", e);
				setSyncStatus(SyncStatus.ACTIVE_NO_CHANGES);
			}
		} else if (FILES.equals(type)) {
			setSyncStatus(hasFileChanges() ? SyncStatus.ACTIVE_HAS_CHANGES
					: SyncStatus.ACTIVE_NO_CHANGES);
		} else {
			setSyncStatus(SyncStatus.ACTIVE_NO_CHANGES);
		}

		if (delayStopSync) {
			delayStopSync = false;
			stopSync();
		}
	}

	private boolean hasDatabaseChanges() throws Exception {
		Module module = ModuleUtil.getModule(moduleKey);
		return databaseManager.fetchRecord().hasRecordsFrom(module.timestamp);
	}

	private boolean hasFileChanges() {
		Module module = ModuleUtil.getModule(moduleKey);

		if (module.fileSyncTimeStamp != null) {
			File attachedFiles = new File(getModuleDir() + "/files");
			return hasFileChanges(attachedFiles, module.fileSyncTimeStamp);
		} else {
			return true;
		}
	}

	private boolean hasFileChanges(File attachedFiles, String fileSyncTimeStamp) {
		if (attachedFiles.isDirectory()) {
			for (File file : attachedFiles.listFiles()) {
				if (file.isDirectory()) {
					return hasFileChanges(file, fileSyncTimeStamp);
				} else {
					if (file.lastModified() > DateUtil.convertToDateGMT(
							fileSyncTimeStamp).getTime()) {
						return true;
					}
				}
			}
		}
		return false;

	}

	public void callSyncFailure() {
		for (SyncListener listener : listeners) {
			listener.handleFailure();
		}
		syncStarted = false;
		setSyncStatus(SyncStatus.ERROR);

		if (delayStopSync) {
			delayStopSync = false;
			stopSync();
		}
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
		Intent intent = new Intent(ShowModuleActivity.this,
				FileChooserActivity.class);
		startActivityForResult(intent, requestCode);
	}

	public int getCopyFileCount() {
		return activityData.getCopyFileCount();
	}

	/*
	 * 
	 * @SuppressWarnings("rawtypes") private boolean isServiceRunning(Class c) {
	 * ActivityManager manager = (ActivityManager)
	 * getSystemService(ACTIVITY_SERVICE); for (RunningServiceInfo service :
	 * manager.getRunningServices(Integer.MAX_VALUE)) {
	 * FLog.d(service.service.getClassName()); if
	 * (c.getName().equals(service.service.getClass().getName())) { return true;
	 * } } return false; }
	 */

	public void setSyncStatus(final SyncStatus status) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (!isSyncStarted()) {
					syncStatus = status;
					invalidateOptionsMenu();
				}
			}

		});
	}

	public SyncStatus getSyncStatus() {
		return syncStatus;
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
				Intent intent = new Intent(ShowModuleActivity.this,
						SyncFilesService.class);

				Module module = ModuleUtil.getModule(moduleKey);

				SyncFilesHandler handler = new SyncFilesHandler(
						ShowModuleActivity.this);

				Messenger messenger = new Messenger(handler);
				intent.putExtra("MESSENGER", messenger);
				intent.putExtra("module", module);
				ShowModuleActivity.this.startService(intent);

			}
		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.clear();
		saveTo(outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void saveTo(Bundle savedInstanceState) {
		try {
			linker.storeBeanShellData(savedInstanceState);
			renderer.storeBackStack(savedInstanceState,
					getSupportFragmentManager());
			renderer.storeTabs(savedInstanceState);
			renderer.storeViewValues(savedInstanceState);
			activityData.setUserId(databaseManager.getUserId());
			activityData.saveTo(savedInstanceState);
			gpsDataManager.saveTo(savedInstanceState);
		} catch (Exception e) {
			FLog.e("error saving bundle", e);
		}
	}

	@Override
	public void restoreFrom(Bundle savedInstanceState) {
		try {
			linker.restoreBeanShellData(savedInstanceState);
			renderer.restoreBackStack(savedInstanceState, this);
			renderer.restoreTabs(savedInstanceState);
			renderer.restoreViewValues(savedInstanceState);
			activityData.restoreFrom(savedInstanceState);
			gpsDataManager.restoreFrom(savedInstanceState);
			this.databaseManager.setUserId(activityData.getUserId());
		} catch (Exception e) {
			FLog.e("error restoring bundle", e);
		}
	}

	// TODO think about what happens if copy fails
	public void copyFile(final String fromFile, final String toFile,
			final AttachFileListener listener) {
		activityData.setCopyFileCount(activityData.getCopyFileCount() + 1);
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

					ShowModuleActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							new CopyFileTask(new File(fromFile), new File(
									toFile), new ITaskListener() {

								@Override
								public void handleTaskCompleted(Object result) {
									activityData.setCopyFileCount(activityData
											.getCopyFileCount() - 1);
									if (listener != null) {
										listener.handleComplete();
									}
								}

							}).execute();
						}

					});

				} catch (Exception e) {
					FLog.e("error copying file", e);
				}
			}

		}).start();
	}

	public boolean isSyncStarted() {
		return syncStarted;
	}
}
