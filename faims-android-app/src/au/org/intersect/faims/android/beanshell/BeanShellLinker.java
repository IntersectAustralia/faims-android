package au.org.intersect.faims.android.beanshell;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.view.InputDevice;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.callbacks.ActionButtonCallback;
import au.org.intersect.faims.android.beanshell.callbacks.DeleteCallback;
import au.org.intersect.faims.android.beanshell.callbacks.FetchCallback;
import au.org.intersect.faims.android.beanshell.callbacks.SaveCallback;
import au.org.intersect.faims.android.beanshell.callbacks.ViewTask;
import au.org.intersect.faims.android.data.ArchEntity;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.EntityAttribute;
import au.org.intersect.faims.android.data.FileInfo;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.IFAIMSRestorable;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.data.Relationship;
import au.org.intersect.faims.android.data.RelationshipAttribute;
import au.org.intersect.faims.android.data.User;
import au.org.intersect.faims.android.data.VocabularyTerm;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.exceptions.MapException;
import au.org.intersect.faims.android.gps.GPSDataManager;
import au.org.intersect.faims.android.gps.GPSLocation;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.managers.BluetoothManager;
import au.org.intersect.faims.android.managers.FileManager;
import au.org.intersect.faims.android.net.ServerDiscovery;
import au.org.intersect.faims.android.nutiteq.GeometryData;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.nutiteq.GeometryTextStyle;
import au.org.intersect.faims.android.serializer.BeanShellSerializer;
import au.org.intersect.faims.android.tasks.CancelableTask;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity.SyncStatus;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.dialog.DateDialog;
import au.org.intersect.faims.android.ui.dialog.TextDialog;
import au.org.intersect.faims.android.ui.dialog.TimeDialog;
import au.org.intersect.faims.android.ui.drawer.NavigationDrawer;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.LegacyQueryBuilder;
import au.org.intersect.faims.android.ui.map.QueryBuilder;
import au.org.intersect.faims.android.ui.view.CameraPictureGallery;
import au.org.intersect.faims.android.ui.view.CustomButton;
import au.org.intersect.faims.android.ui.view.CustomCheckBoxGroup;
import au.org.intersect.faims.android.ui.view.CustomFileList;
import au.org.intersect.faims.android.ui.view.CustomListView;
import au.org.intersect.faims.android.ui.view.CustomRadioGroup;
import au.org.intersect.faims.android.ui.view.CustomSpinner;
import au.org.intersect.faims.android.ui.view.CustomWebView;
import au.org.intersect.faims.android.ui.view.FileListGroup;
import au.org.intersect.faims.android.ui.view.HierarchicalPictureGallery;
import au.org.intersect.faims.android.ui.view.HierarchicalSpinner;
import au.org.intersect.faims.android.ui.view.ICustomView;
import au.org.intersect.faims.android.ui.view.IView;
import au.org.intersect.faims.android.ui.view.Picture;
import au.org.intersect.faims.android.ui.view.PictureGallery;
import au.org.intersect.faims.android.ui.view.Tab;
import au.org.intersect.faims.android.ui.view.TabGroup;
import au.org.intersect.faims.android.ui.view.Table;
import au.org.intersect.faims.android.ui.view.UIRenderer;
import au.org.intersect.faims.android.ui.view.VideoGallery;
import au.org.intersect.faims.android.util.Arch16n;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.GeometryUtil;
import au.org.intersect.faims.android.util.ModuleUtil;
import bsh.EvalError;
import bsh.Interpreter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Point;

@Singleton
public class BeanShellLinker implements IFAIMSRestorable {
	
	@Inject
	DatabaseManager databaseManager;
	
	@Inject
	GPSDataManager gpsDataManager;
	
	@Inject
	BluetoothManager bluetoothManager;
	
	@Inject
	ServerDiscovery serverDiscovery;
	
	@Inject
	FileManager fileManager;
	
	@Inject
	UIRenderer uiRenderer;
	
	@Inject
	Arch16n arch16n;
	
	@Inject
	AutoSaveManager autoSaveManager;
	
	@Inject
	NavigationDrawer navigationDrawer;

	private Interpreter interpreter;

	private WeakReference<ShowModuleActivity> activityRef;
	
	private Module module;

	private HandlerThread trackingHandlerThread;
	private Handler trackingHandler;
	private Runnable trackingTask;
	private MediaRecorder recorder;
	
	private static final String TAG = "beanshell_linker:";
	
	private HashMap<String, Serializable> beanshellVariables;
	
	private String persistedObjectName;
	
	private Double prevLong;
	private Double prevLat;
	
	private String textAlertInput;
	private String dateAlertInput;
	private String timeAlertInput;

	private String lastFileBrowserCallback;

	private String cameraPicturepath;
	private String cameraCallBack;

	private String videoCallBack;
	private String cameraVideoPath;

	private String audioFileNamePath;
	private String audioCallBack;
	
	private String scanContents;
	private String scanCallBack;
	
	private String hardwareBufferContents;
	private String hardwareReadingCallBack;

	private Toast toast;

	public void init(ShowModuleActivity activity, Module module) {
		FAIMSApplication.getInstance().injectMembers(this);
		this.interpreter = new Interpreter();
		this.activityRef = new WeakReference<ShowModuleActivity>(activity);
		this.module = module;
		this.persistedObjectName = null;
		this.beanshellVariables = null; 
		this.lastFileBrowserCallback = null;
		this.trackingTask = null;
		this.prevLong = 0d;
		this.prevLat = 0d;
		this.textAlertInput = null;
		this.cameraPicturepath = null;
		this.cameraCallBack = null;
		this.videoCallBack = null;
		this.cameraVideoPath = null;
		this.audioFileNamePath = null;
		this.recorder = null;
		this.audioCallBack = null;
		this.scanContents = null;
		this.scanCallBack = null;
		
		try {
			interpreter.set("linker", this);
		} catch (Exception e) {
			FLog.e("error setting linker", e);
		}
		
		fileManager.addListener(
				ShowModuleActivity.FILE_BROWSER_REQUEST_CODE,
				new FileManager.FileManagerListener() {

					@Override
					public void onFileSelected(File file) {
						BeanShellLinker.this.setLastSelectedFile(file);
					}

				});
	}

	public void sourceFromAssets(String filename) {
		try {
			interpreter.eval(FileUtil.convertStreamToString(this.activityRef.get().getAssets().open(filename)));
		} catch (Exception e) {
			FLog.w("error sourcing script from assets", e);
			showWarning("Logic Error", "Error encountered in logic commands");
		}
	}
	
	public Interpreter getInterpreter() {
		return interpreter;
	}
	
	public DatabaseManager getDatabaseManager() {
		return databaseManager;
	}
	
	public UIRenderer getUIRenderer() {
		return uiRenderer;
	}
	
	public AutoSaveManager getAutoSaveManager() {
		return autoSaveManager;
	}
	
	public ShowModuleActivity getActivity() {
		return activityRef.get();
	}

	public void persistObject(String name) {
		setPersistedObjectName(name);
	}

	public String getPersistedObjectName() {
		return persistedObjectName;
	}

	public void setPersistedObjectName(String persistedObjectName) {
		this.persistedObjectName = persistedObjectName;
	}

	public void execute(String code) {
		executeOnUiThread(code);
	}
	
	public void set(String var, String value) {
		try {
			interpreter.set(var, value);
		} catch (Exception e) {
			FLog.i("error executing code", e);
			showWarning("Logic Error", "Error encountered in logic script");
		}
	}
	
	public void executeOnUiThread(final String code) {
		if (code == null) return;
		activityRef.get().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				try {
					interpreter.eval(code);
				} catch (EvalError e) {
					FLog.i("error executing code", e);
					showWarning("Logic Error", "Error encountered in logic script");
				} catch (Exception e) {
					FLog.e("error executing code", e);
				}
			}
			
		});
	}

	public void startTrackingGPS(final String type, final int value, final String callback) {
		FLog.d("gps tracking is started");
		
		gpsDataManager.setTrackingType(type);
		gpsDataManager.setTrackingValue(value);
		gpsDataManager.setTrackingExec(callback);

		if (trackingHandlerThread == null && trackingHandler == null) {
			if (!gpsDataManager.isExternalGPSStarted()
					&& !gpsDataManager
							.isInternalGPSStarted()) {
				showWarning("GPS", "No GPS is being used");
				return;
			}
			gpsDataManager.setTrackingStarted(true);
			activityRef.get().updateStatusBar();
			trackingHandlerThread = new HandlerThread("tracking");
			trackingHandlerThread.start();
			trackingHandler = new Handler(trackingHandlerThread.getLooper());
			if ("time".equals(type)) {
				trackingTask = new Runnable() {

					@Override
					public void run() {
						trackingHandler.postDelayed(this, value * 1000);
						activityRef.get().runOnUiThread(new Runnable() {						
							@Override
							public void run() {
								execute(callback);
							}
						});
					}
				};
				trackingHandler.postDelayed(trackingTask, value * 1000);
			} else if ("distance".equals(type)) {
				trackingTask = new Runnable() {

					@Override
					public void run() {
						trackingHandler.postDelayed(this, 1000);
						if (getGPSPosition() != null) {
							GPSLocation currentLocation = (GPSLocation) getGPSPosition();
							Double longitude = currentLocation.getLongitude();
							Double latitude = currentLocation.getLatitude();
							if (longitude != null && latitude != null) {
								if (prevLong != null && prevLat != null) {
									float[] results = new float[1];
									Location.distanceBetween(prevLat, prevLong,
											latitude, longitude, results);
									double distance = results[0];
									if (distance > value) {
										execute(callback);
									}
								}
								prevLong = longitude;
								prevLat = latitude;
							}
						} else {
							execute(callback);
						}
					}
				};
				trackingHandler.postDelayed(trackingTask, 1000);
			} else {
				FLog.e("wrong type format is used");
			}
		} else {
			showToast("GPS tracking has been started, please stop it before starting");
		}
	}
	
	public boolean isTrackingGPS() {
		try {
			return gpsDataManager.isTrackingStarted();
		} catch (Exception e) {
			FLog.e("error checking GPS tracking state", e);
			showWarning("Logic Error", "Error checking GPS tracking state");
		}
		return false;
	}

	public void stopTrackingGPS() {
		FLog.d("gps tracking is stopped");
		
		if (trackingHandlerThread != null) {
			trackingHandlerThread.quit();
			trackingHandlerThread = null;
		}
		
		if (trackingHandler != null) {
			trackingHandler.removeCallbacks(trackingTask);
			trackingHandler = null;
		}
		
		gpsDataManager.setTrackingStarted(false);
		activityRef.get().updateStatusBar();
	}

	public void bindViewToEvent(String ref, String type, final String code) {
		try {
			if ("click".equals(type.toLowerCase(Locale.ENGLISH))) {
				View view = uiRenderer.getViewByRef(ref);
				if (view instanceof CustomListView) {
					final CustomListView listView = (CustomListView) view;
					listView.setClickCallback(code);
				} else if (view instanceof IView) {
					IView iview = (IView) view;
					iview.setClickCallback(code);
				} else {
					FLog.w("cannot bind " + type +" event to view " + ref);
					showWarning("Logic Error", "Error bind " + type + " event to view " + ref);
					return;
				}
			} else if ("select".equals(type.toLowerCase(Locale.ENGLISH))) {
				View view = uiRenderer.getViewByRef(ref);
				if (view instanceof IView) {
					IView iview = (IView) view;
					iview.setSelectCallback(code);
				} else {
					FLog.w("cannot bind " + type +" event to view " + ref);
					showWarning("Logic Error", "Error bind " + type + " event to view " + ref);
					return;
				}
			} else if ("delayclick".equals(type.toLowerCase(Locale.ENGLISH))) {
				View view = uiRenderer.getViewByRef(ref);
				if (view instanceof CustomButton) {
					CustomButton button = (CustomButton) view;
					button.setDelayClickCallback(code);
				} else {
					FLog.w("cannot bind " + type +" event to view " + ref);
					showWarning("Logic Error", "Error bind " + type + " event to view " + ref);
					return;
				}
			} else if ("load".equals(type.toLowerCase(Locale.ENGLISH))) {
				TabGroup tg = uiRenderer.getTabGroupByLabel(ref);
				if (tg == null) {
					Tab tb = uiRenderer.getTabByLabel(ref);
					if (tb == null) {
						FLog.w("cannot bind " + type +" event to " + ref);
						showWarning("Logic Error", "Error bind " + type + " event to " + ref);
						return;
					} else {
						tb.addOnLoadCommand(code);
					}
				} else {
					tg.addOnLoadCommand(code);
				}
			} else if ("show".equals(type.toLowerCase(Locale.ENGLISH))) {
				TabGroup tg = uiRenderer.getTabGroupByLabel(ref);
				if (tg == null) {
					Tab tb = uiRenderer.getTabByLabel(ref);
					if (tb == null) {
						FLog.w("cannot bind " + type +" event to " + ref);
						showWarning("Logic Error", "Error bind " + type + " event to " + ref);
						return;
					} else {
						tb.addOnShowCommand(code);
					}
				} else {
					tg.addOnShowCommand(code);
				}
			} else {
				FLog.w("cannot find event type " + type);
				showWarning("Logic Error", "Error bind event type " + type);
			}
		} catch (Exception e) {
			FLog.e("exception binding event to view " + ref, e);
			showWarning("Logic Error", "Error binding event to view " + ref);
		}
	}

	public void bindFocusAndBlurEvent(String ref, final String focusCallback,
			final String blurCallback) {
		try {
			View view = uiRenderer.getViewByRef(ref);
			if (view instanceof IView) {
				IView iview = (IView) view;
				iview.setFocusBlurCallbacks(focusCallback, blurCallback);
			} else {
				FLog.w("cannot bind focus and blur to view " + ref);
				showWarning("Logic Error", "Error bind focus and blur to view " + ref);
				return;
			}
		} catch (Exception e) {
			FLog.e("exception binding focus and blur event to view " + ref, e);
			showWarning("Logic Error",
					"Error cannot bind focus and blur event to view " + ref);
		}
	}

	public void bindMapEvent(String ref, final String clickCallback,
			final String selectCallback) {
		try {
			View view = uiRenderer.getViewByRef(ref);
			if (view instanceof CustomMapView) {
				final CustomMapView mapView = (CustomMapView) view;
				mapView.setMapCallbacks(clickCallback, selectCallback);
			} else {
				FLog.w("cannot bind map event to view " + ref);
				showWarning("Logic Error",
						"Error cannot bind map event to view " + ref);
			}
		} catch (Exception e) {
			FLog.e("exception binding map event to view " + ref, e);
			showWarning("Logic Error", "Error cannot bind map event to view "
					+ ref);
		}
	}
	
	public void saveMapViewConfiguration(String ref, String file, String callback) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				JSONObject json = new JSONObject();
				((CustomMapView) obj).saveToJSON(json);
				File fileToSave = new File(file);
				if (!fileToSave.exists()) {
					fileToSave.getParentFile().mkdirs();
				}
				FileWriter writer = new FileWriter(file);
				writer.write(json.toString());
				writer.close();
				
				execute(callback);
			} else {
				FLog.w("cannot save configuration for map view " + obj);
				showWarning("Logic Error", "Cannot save configuration for map view " + obj);
			}
		} catch (Exception e) {
			FLog.e("error saving map view configuration", e);
			showWarning("Logic Error", "Error saving map view configuration");
		}
	}
	
	public void loadMapViewConfiguration(String ref, String configFile, final String callback) {
		try {
			final Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				String jsonContents = FileUtil.readFileIntoString(configFile);
				final JSONObject json = new JSONObject(jsonContents);
				
				activityRef.get().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						((CustomMapView) obj).loadFromJSON(json);
						execute(callback);
					}
					
				});
				
			} else {
				FLog.w("cannot load configuration for map view " + obj);
				showWarning("Logic Error", "Cannot load configuration for map view " + obj);
			}
		} catch (Exception e) {
			FLog.e("error loading map view configuration", e);
			showWarning("Logic Error", "Error loading map view configuration");
		}
	}
	
	protected TabGroup getTabGroup(String ref) throws Exception {
		TabGroup tabGroup = uiRenderer.getTabGroupByLabel(ref);
		if (tabGroup == null) {
			throw new Exception("Cannot find tab group " + ref);
		}
		return tabGroup;
	}
	
	protected Tab getTab(String ref) throws Exception {
		Tab tab = uiRenderer.getTabByLabel(ref);
		if (tab == null) {
			throw new Exception("Cannot find tab " + ref);
		}
		return tab;
	}
	
	protected TabGroup getTabGroupFromRef(String ref) throws Exception {
		if (ref == null) {
			throw new Exception("Cannot find tab group " + ref);
		}
		String[] ids = ref.split("/");
		if (ids.length < 1) {
			throw new Exception("Cannot find tab group " + ref);
		}
		String groupId = ids[0];
		TabGroup tabGroup = uiRenderer.getTabGroupByLabel(groupId);
		if (tabGroup == null) {
			throw new Exception("Cannot find tab group " + ref);
		}
		return tabGroup;
	}
	
	protected Tab getTabFromRef(String ref) throws Exception {
		if (ref == null) {
			throw new Exception("Cannot find tab " + ref);
		}
		String[] ids = ref.split("/");
		if (ids.length < 2) {
			throw new Exception("Cannot find tab " + ref);
		}
		String tabId = ids[0] + "/" + ids[1];
		Tab tab = uiRenderer.getTabByLabel(tabId);
		if (tab == null) {
			throw new Exception("Cannot find tab " + ref);
		}
		return tab;
	}

	public void newTabGroup(String label) {
		try {
			TabGroup tabGroup = showTabGroup(label);
			tabGroup.clearTabs();
			tabGroup.setArchEntity(null);
			tabGroup.setRelationship(null);
		} catch (Exception e) {
			FLog.e("error showing new tabgroup " + label, e);
			showWarning("Logic Error", "Error showing new tab group " + label);
		}
	}

	public void newTab(String label) {
		try {
			Tab tab = showTab(label);
			tab.clearViews();
		} catch (Exception e) {
			FLog.e("error showing new tab " + label, e);
			showWarning("Logic Error", "Error showing new tab " + label);
		}
	}

	public TabGroup showTabGroup(String label) {
		try {
			autoSaveManager.flush();
			
			final TabGroup tabGroup = uiRenderer.showTabGroup(label);
			if (tabGroup == null) {
				throw new Exception("cannot find tabgroup " + label);
			}
			return tabGroup;
		} catch (Exception e) {
			FLog.e("error showing tabgroup " + label, e);
			showWarning("Logic Error", "Error showing tab group " + label);
		}
		return null;
	}
	
	public TabGroup showTabGroup(final String label, final String uuid, final FetchCallback callback) {
		try {
			autoSaveManager.flush();
			
			final TabGroup tabGroup = uiRenderer.showTabGroup(label);
			if (tabGroup == null) {
				throw new Exception("cannot find tabgroup " + label);
			}
			tabGroup.setOnShowTask(new TabGroup.TabTask() {
				public void onShow() {
					try {
						autoSaveManager.pause();
						if (tabGroup.getArchEntType() != null) {
							TabGroupHelper.showArchEntityTabGroup(BeanShellLinker.this, uuid, tabGroup, callback);
						} else if (tabGroup.getRelType() != null) {
							TabGroupHelper.showRelationshipTabGroup(BeanShellLinker.this, uuid, tabGroup, callback);
						}
					} catch (Exception e) {
						FLog.e("error showing tabgroup " + label, e);
						showWarning("Logic Error", "Error showing tab group " + label);
					} finally {
						autoSaveManager.resume();
					}
				}
			});
			return tabGroup;
		} catch (Exception e) {
			FLog.e("error showing tabgroup " + label, e);
			showWarning("Logic Error", "Error showing tab group " + label);
		}
		return null;
	}

	public Tab showTab(String label) {
		try {
			Tab tab = uiRenderer.showTab(label);
			if (tab == null) {
				throw new Exception("cannot show tab " + label);
			}
			return tab;
		} catch (Exception e) {
			FLog.e("error showing tab " + label, e);
			showWarning("Logic Error", "Error showing tab " + label);
		}
		return null;
	}

	public Tab showTab(String label, String uuid, FetchCallback callback) {
		try {
			TabGroup tabGroup = getTabGroupFromRef(label);
			Tab tab = uiRenderer.showTab(label);
			if (tab == null) {
				throw new Exception("cannot show tab " + label);
			}
			if (tabGroup.getArchEntType() != null) {
				TabGroupHelper.showArchEntityTab(this, uuid, tab, callback);
			} else if (tabGroup.getRelType() != null) {
				TabGroupHelper.showRelationshipTab(this, uuid, tab, callback);
			}
			return tab;
		} catch (Exception e) {
			FLog.e("error showing tab " + label, e);
			showWarning("Logic Error", "Error showing tab " + label);
		}
		return null;
	}
	
	// This method is used internally by the AutoSaveManager and not directly called by any logic API call
	public void saveTabGroupWithBlockingOption(String tabGroupRef, String uuid, List<Geometry> geometry, 
			List<? extends Attribute> attributes, SaveCallback callback, 
			boolean newRecord, boolean blocking) {
		TabGroupHelper.saveTabGroupInBackground(this, tabGroupRef, uuid, geometry, attributes, callback, newRecord, blocking);
	}
	
	public void saveTabGroup(String ref, String uuid, List<Geometry> geometry, 
			List<? extends Attribute> attributes, SaveCallback callback) {
		TabGroupHelper.saveTabGroupInBackground(this, ref, uuid, geometry, attributes, callback, uuid == null, false);
	}
	
	public void saveTabGroup(String ref, String uuid, List<Geometry> geometry, 
			List<? extends Attribute> attributes, SaveCallback callback, 
			boolean enableAutoSave) {
		if (enableAutoSave) {
			boolean newRecord = uuid == null;
			if (newRecord) {
				uuid = databaseManager.sharedRecord().generateUUID();
			}
			autoSaveManager.enable(ref, uuid, geometry, attributes, callback, newRecord);
		} else {
			TabGroupHelper.saveTabGroupInBackground(this, ref, uuid, geometry, attributes, callback, uuid == null, false);
		}
	}
	
	public void duplicateTabGroup(String ref, List<Geometry> geometry, 
			List<? extends Attribute> attributes, List<String> excludeAttributes, SaveCallback callback) {
		TabGroupHelper.duplicateTabGroupInBackground(this, ref, geometry, attributes, excludeAttributes, callback);
	}
	
	public void disableAutoSave(String ref) {
		autoSaveManager.disable(ref);
	}

	public void saveTab(String ref, String uuid, List<Geometry> geometry, 
			List<? extends Attribute> attributes, SaveCallback callback) {
		TabGroupHelper.saveTabInBackground(this, ref, uuid, geometry, attributes, callback, uuid == null, false);
	}

	public void cancelTabGroup(String label, boolean warn) {
		try {
			TabGroup tabGroup = getTabGroup(label);
			if (warn) {
				boolean hasChanges = false;
				if (tabGroup.getArchEntType() != null
						|| tabGroup.getRelType() != null) {
					for (Tab tab : tabGroup.getTabs()) {
						if (tab.hasChanges()) {
							hasChanges = true;
							break;
						}
					}
				}
				if (hasChanges) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							this.activityRef.get());

					builder.setTitle("Warning");
					builder.setMessage("Are you sure you want to cancel the tab group? You have unsaved changes there.");
					builder.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// User clicked OK button
									goBack();
								}
							});
					builder.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// User cancelled the dialog
								}
							});

					builder.create().show();
				} else {
					goBack();
				}
			} else {
				goBack();
			}
		} catch (Exception e) {
			FLog.e("error cancelling tab group " + label, e);
			showWarning("Logic Error", "Error cancelling tab group " + label);
		}
	}

	public void cancelTab(String label, boolean warn) {
		try {
			final TabGroup tabGroup = getTabGroupFromRef(label);
			final Tab tab = getTab(label);
			if (warn) {
				if (tab.hasChanges() 
						&& (tabGroup.getArchEntType() != null || 
						tabGroup.getRelType() != null)) {
					AlertDialog.Builder builder = new AlertDialog.Builder(this.activityRef.get());
					builder.setTitle("Warning");
					builder.setMessage("Are you sure you want to cancel the tab? You have unsaved changes there.");
					builder.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// User clicked OK button
									if (tabGroup.getTabs().size() == 1) {
										goBack();
									} else {
										tabGroup.hideTab(tab.getName());
									}
								}
							});
					builder.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// User cancelled the dialog
									tabGroup.showTab(tab.getName());
								}
							});

					builder.create().show();
				} else {
					if (tabGroup.getTabs().size() == 1) {
						goBack();
					} else {
						tabGroup.hideTab(tab.getName());
					}
				}
			} else {
				if (tabGroup.getTabs().size() == 1) {
					goBack();
				} else {
					tabGroup.hideTab(tab.getName());
				}
			}
		} catch (Exception e) {
			FLog.e("error cancelling tab " + label, e);
			showWarning("Logic Error", "Error cancelling tab " + label);
		}
	}
	
	public void refreshTabgroupCSS(String ref) {
		try {
			TabGroup tabGroup = getTabGroup(ref);
			tabGroup.refreshCSS();
		} catch (Exception e) {
			FLog.e("Error trying to refresh tab group CSS for " + ref, e);
			showWarning("Logic Error", "Error trying to refresh tab group CSS for " + ref);
		}
	}
	
	public void keepTabGroupChanges(String ref) {
		try {
			TabGroup tabGroup = getTabGroup(ref);
			tabGroup.keepChanges();
		} catch (Exception e) {
			FLog.e("Error trying to reset tab group changes for " + ref, e);
			showWarning("Logic Error", "Error trying to reset tab group changes for " + ref);
		}
	}
	
	public boolean isAutosaveEnabled() {
		try {
			return autoSaveManager.isEnabled();
		} catch (Exception e) {
			FLog.e("error checking autosave state", e);
			showWarning("Logic Error", "Error checking autosave state");
		}
		return false;
	}

	public void goBack() {
		this.activityRef.get().onBackPressed();
	}
	
	public void showToast(final String message) {
		activityRef.get().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				try {
					int duration = Toast.LENGTH_SHORT;
					if (toast != null) {
						toast.cancel();
					}
					toast = Toast.makeText(activityRef.get().getBaseContext(),
							arch16n.substituteValue(message), duration);
					toast.show();
				} catch (Exception e) {
					FLog.e("error showing toast", e);
					showWarning("Logic Error", "Error showing toast");
				}
			}
			
		});
	}

	public Dialog showAlert(final String title, final String message,
			final String okCallback, final String cancelCallback) {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(this.activityRef.get());
	
			builder.setTitle(arch16n.substituteValue(title));
			builder.setMessage(arch16n.substituteValue(message));
			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// User clicked OK button
					execute(okCallback);
				}
			});
			builder.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							// User cancelled the dialog
							execute(cancelCallback);
						}
					});
	
			Dialog dialog = builder.create();
			dialog.show();
			return dialog;
		} catch (Exception e) {
			FLog.e("error showing alert", e);
			showWarning("Logic Error", "Error show alert dialog");
		}
		return null;
	}

	public Dialog showWarning(final String title, final String message) {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(this.activityRef.get());
	
			builder.setTitle(arch16n.substituteValue(title));
			builder.setMessage(arch16n.substituteValue(message));
			builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// User clicked OK button
				}
			});
			Dialog dialog = builder.create();
			dialog.show();
			return dialog;
		} catch (Exception e) {
			FLog.e("error showing warning", e);
		}
		return null;
	}
	
	public Dialog showBusy(final String title, final String message) {
		try {
			BusyDialog dialog = new BusyDialog(this.activityRef.get(), arch16n.substituteValue(title),
					arch16n.substituteValue(message), null);
			dialog.show();
			return dialog;
		} catch (Exception e) {
			FLog.e("error showing busy", e);
			showWarning("Logic Error", "Error show busy dialog");
		}
		return null;
	}
	
	public Dialog showTextAlert(final String title, final String message,
			final String okCallback, final String cancelCallback) {
		try {
			final TextDialog textDialog = new TextDialog(this.activityRef.get(), arch16n.substituteValue(title),
					arch16n.substituteValue(message), null, null);
			textDialog.setOkListener(new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					textAlertInput = textDialog.getInputText(); 
					execute(okCallback);
				}
			});
			textDialog.setCancelListener(new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					execute(cancelCallback);
				}
			});
			textDialog.show();
			return textDialog;
		} catch (Exception e) {
			FLog.e("error showing text alert", e);
			showWarning("Logic Error", "Error show text alert dialog");
		}
		return null;
	}
	
	public String getLastTextAlertInput() {
		return textAlertInput;
	}
	
	public Dialog showDateAlert(final String title, final String message,
			final String okCallback, final String cancelCallback) {
		try {
			final DateDialog dateDialog = new DateDialog(this.activityRef.get(), arch16n.substituteValue(title),
					arch16n.substituteValue(message), null, null);
			dateDialog.setOkListener(new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dateAlertInput = dateDialog.getDateText(); 
					execute(okCallback);
				}
			});
			dateDialog.setCancelListener(new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					execute(cancelCallback);
				}
			});
			dateDialog.show();
			return dateDialog;
		} catch (Exception e) {
			FLog.e("error showing date alert", e);
			showWarning("Logic Error", "Error show date alert dialog");
		}
		return null;
	}
	
	public String getLastDateAlertInput() {
		return dateAlertInput;
	}
	
	public Dialog showTimeAlert(final String title, final String message,
			final String okCallback, final String cancelCallback) {
		try {
			final TimeDialog timeDialog = new TimeDialog(this.activityRef.get(), arch16n.substituteValue(title),
					arch16n.substituteValue(message), null, null);
			timeDialog.setOkListener(new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					timeAlertInput = timeDialog.getTimeText(); 
					execute(okCallback);
				}
			});
			timeDialog.setCancelListener(new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					execute(cancelCallback);
				}
			});
			timeDialog.show();
			return timeDialog;
		} catch (Exception e) {
			FLog.e("error showing time alert", e);
			showWarning("Logic Error", "Error show time alert dialog");
		}
		return null;
	}
	
	public String getLastTimeAlertInput() {
		return timeAlertInput;
	}
	
	public void reportError(Exception e) {
		reportError(e.getMessage(), e);
	}
	
	public void reportError(final String message, Exception e) {
		FLog.e(message, e);
		ShowModuleActivity activity = activityRef.get();
		if (activity != null) {
			activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					showWarning("Logic Error", message);
				}
				
			});
		} else {
			FLog.d("cannot report error due to missing activity");
		}
	}

	public void setFieldValue(String ref, Object valueObj) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof ICustomView) {
				ICustomView customView = (ICustomView) obj;
				
				if (obj instanceof CustomCheckBoxGroup) {
					if (valueObj instanceof List<?>) {
						List<?> values = null;
						values = convertToNameValuePairs((List<?>) valueObj);
						customView.setValues(values);
					} else {
						String value = valueObj == null ? null : String.valueOf(valueObj);
						value = arch16n.substituteValue(value);
						customView.setValue((String) value);
					}
				} else if (obj instanceof PictureGallery) {
					if (valueObj instanceof List<?>) {
						List<?> values = null;
						values = convertToNameValuePairs((List<?>) valueObj);
						customView.setValues(values);
					} else {
						String value = valueObj == null ? null : String.valueOf(valueObj);
						value = arch16n.substituteValue(value);
						customView.setValue((String) value);
					}
				} else {
					String value = valueObj == null ? null : String.valueOf(valueObj);
					value = arch16n.substituteValue(value);
					customView.setValue((String) value);
				}
			} else {
				FLog.w("cannot set field value to view with ref " + ref);
				showWarning("Logic Error", "Cannot find view with ref " + ref);
			}
		} catch (Exception e) {
			FLog.e("error setting field value " + ref, e);
			showWarning("Logic Error", "Error setting field value " + ref);
		}
	}

	public void setFieldCertainty(String ref, Object valueObj) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof ICustomView) {
				ICustomView customView = (ICustomView) obj;

				float value = valueObj == null ? 0 : Float.valueOf(String.valueOf(valueObj));
				
				customView.setCertainty(value);
			} else {
				FLog.w("cannot set field certainty to view with ref " + ref);
				showWarning("Logic Error", "Cannot find view with ref " + ref);
			}
		} catch (Exception e) {
			FLog.e("error setting field certainty " + ref, e);
			showWarning("Logic Error", "Error setting field certainty " + ref);
		}
	}

	public void setFieldAnnotation(String ref, Object valueObj) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof ICustomView) {
				ICustomView customView = (ICustomView) obj;
				
				String value = valueObj == null ? null : String.valueOf(valueObj);
				
				customView.setAnnotation(value);	
			} else {
				FLog.w("cannot set field annotation to view with ref " + ref);
				showWarning("Logic Error", "Cannot find view with ref " + ref);
			}
		} catch (Exception e) {
			FLog.e("error setting field annotation " + ref, e);
			showWarning("Logic Error", "Error setting field annotation " + ref);
		}
	}
	
	public void setFieldDirty(String ref, boolean isDirty, String isDirtyReason) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof ICustomView) {
				ICustomView customView = (ICustomView) obj;
				
				ImageView dirtyButton = uiRenderer.getTabForView(ref).getDirtyButton(ref);
				if (dirtyButton != null) {
					dirtyButton.setVisibility(isDirty ? View.VISIBLE : View.GONE);
				}
				
				customView.setDirty(isDirty);
				customView.setDirtyReason(isDirtyReason);
			} else {
				FLog.w("cannot set field dirty to view with ref " + ref);
				showWarning("Logic Error", "Cannot find view with ref " + ref);
			}
		} catch (Exception e) {
			FLog.e("error setting field isDirty " + ref, e);
			showWarning("Logic Error", "Error setting field dirty " + ref);
		}
	}
	
	protected void appendFieldDirty(String ref, boolean isDirty, String dirtyReason) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof ICustomView) {
				ICustomView customView = (ICustomView) obj;
				
				boolean isViewDirty = isDirty || customView.isDirty();
				
				ImageView dirtyButton = uiRenderer.getTabForView(ref).getDirtyButton(ref);
				if (dirtyButton != null) {
					dirtyButton.setVisibility(isViewDirty ? View.VISIBLE : View.INVISIBLE);
				}
				
				customView.setDirty(isViewDirty);
				
				String reason = null;
				if (dirtyReason != null && !"".equals(dirtyReason)) {
					reason = customView.getDirtyReason();
					if (reason != null && !"".equals(reason)) {
						reason += ";" + dirtyReason;
					} else {
						reason = dirtyReason;
					}
				}
				
				customView.setDirtyReason(reason);
			} else {
				FLog.w("cannot set field dirty to view with ref " + ref);
				showWarning("Logic Error", "Cannot find view with ref " + ref);
			}
		} catch (Exception e) {
			FLog.e("error setting field isDirty " + ref, e);
			showWarning("Logic Error", "Error setting field dirty " + ref);
		}
	}

	public Object getFieldValue(String ref) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof ICustomView) {
				ICustomView customView = (ICustomView) obj;
				
				if (customView instanceof CustomCheckBoxGroup) {
					return customView.getValues();
				} else if (customView instanceof CustomFileList) {
					return customView.getValues();
				} else if (customView instanceof PictureGallery) {
					if (((PictureGallery) customView).isMulti()) {
						return customView.getValues();
					} else {
						return customView.getValue();
					}
				} else {
					return customView.getValue();
				}
			} else {
				FLog.w("cannot get field value from view with ref " + ref);
				showWarning("Logic Error", "Cannot find view with ref " + ref);
			}
		} catch (Exception e) {
			FLog.e("error getting field value " + ref, e);
			showWarning("Logic Error", "Error getting field value " + ref);
		}
		return null;
	}

	public Object getFieldCertainty(String ref) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof ICustomView) {
				ICustomView customView = (ICustomView) obj;
				
				return String.valueOf(customView.getCertainty());
			} else {
				FLog.w("cannot get field certainty from view with ref " + ref);
				showWarning("Logic Error", "Cannot find view with ref " + ref);
			}
		} catch (Exception e) {
			FLog.e("error getting field value " + ref, e);
			showWarning("Logic Error", "Error getting field certainty " + ref);
		}
		return null;
	}

	public Object getFieldAnnotation(String ref) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof ICustomView) {
				ICustomView customView = (ICustomView) obj;
				
				return customView.getAnnotation();
			} else {
				FLog.w("cannot get field annotation from view with ref " + ref);
				showWarning("Logic Error", "Cannot find view with ref " + ref);
			}
		} catch (Exception e) {
			FLog.e("error getting field value " + ref, e);
			showWarning("Logic Error", "Error getting field annotation " + ref);
		}
		return null;
	}
	
	public String getFieldDirty(String ref) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof ICustomView) {
				ICustomView customView = (ICustomView) obj;
				
				return customView.getDirtyReason();
			} else {
				FLog.w("cannot get field value dirty view with ref " + ref);
				showWarning("Logic Error", "Cannot find view with ref " + ref);
			}
		} catch (Exception e) {
			FLog.e("error getting field value " + ref, e);
			showWarning("Logic Error", "Error getting field dirty " + ref);
		}
		return null;
	}

	public void saveArchEnt(String entityId, String entityType,
			List<Geometry> geometry, List<EntityAttribute> attributes, SaveCallback callback) {
		DatabaseHelper.saveArchEnt(this, entityId, entityType, geometry, attributes, callback, entityId == null, false);
	}
	
	public void saveRel(String relationshipId, String relationshipType,
			List<Geometry> geometry, List<RelationshipAttribute> attributes, SaveCallback callback) {
		DatabaseHelper.saveRel(this, relationshipId, relationshipType, geometry, attributes, callback, relationshipId == null, false);
	}
	
	public void deleteArchEnt(String entityId, DeleteCallback callback) {
		DatabaseHelper.deleteArchEnt(this, entityId, callback);
	}

	public void deleteRel(String relationshipId, DeleteCallback callback){
		DatabaseHelper.deleteRel(this, relationshipId, callback);
	}
	
	public void addReln(String entityId, String relationshpId, String verb, SaveCallback callback) {
		DatabaseHelper.addReln(this, entityId, relationshpId, verb, callback);
	}

	@SuppressWarnings("rawtypes")
	public void populateDropDown(String ref, Collection valuesObj, boolean hasNull) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof CustomSpinner && valuesObj instanceof Collection<?>) {
				CustomSpinner spinner = (CustomSpinner) obj;

				List<NameValuePair> pairs = convertToNameValuePairs((Collection<?>) valuesObj);
				if (hasNull) {
					spinner.populateWithNull(pairs);
				} else {
					spinner.populate(pairs);
				}
				spinner.save();
			} else {
				FLog.w("cannot populate drop down "
						+ ref);
				showWarning("Logic Error", "Cannot populate drop down " + ref);
			}
		} catch (Exception e) {
			FLog.e("error populate drop down " + ref, e);
			showWarning("Logic Error", "Error populate drop down " + ref);
		}
	}
	
	public void populateHierarchicalDropDown(String ref, final String attributeName, final boolean hasNull) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof HierarchicalSpinner) {
				final HierarchicalSpinner spinner = (HierarchicalSpinner) obj;
				CancelableTask task = new CancelableTask() {

					private List<VocabularyTerm> terms;

					@Override
					protected Void doInBackground(
							Void... params) {
						try {
							terms = databaseManager.attributeRecord().getVocabularyTerms(attributeName);
							return null;
						} catch (Exception e) {
							FLog.e("Error trying to load vocabulary terms", e);
						}
						return null;
					}
					
					@Override
					protected void onPostExecute(Void result) {
						if (terms == null) {
							FLog.w("Error trying to load vocabulary terms for attribute " + attributeName);
							showWarning("Populate Error", "Error trying to load vocabulary terms");
						} else {
							VocabularyTerm.applyArch16n(terms, arch16n);
							if (hasNull) {
								spinner.setTermsWithNull(terms);
							} else {
								spinner.setTerms(terms);
							}
							spinner.save();
						}
					}
					
				};
				task.execute();				
			} else {
				FLog.w("cannot populate hierarchical drop down "
						+ ref);
				showWarning("Logic Error", "Cannot populate hierarchical drop down " + ref);
			}
		} catch (Exception e) {
			FLog.e("error populate hierarchical drop down " + ref, e);
			showWarning("Logic Error", "Error populate hierarchical drop down " + ref);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void populateList(String ref, Collection valuesObj) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof CustomCheckBoxGroup) {
				CustomCheckBoxGroup checkboxGroup = (CustomCheckBoxGroup) obj;
				checkboxGroup.populate(convertToNameValuePairs(valuesObj));
			} else if (obj instanceof CustomRadioGroup) {
				CustomRadioGroup radioGroup = (CustomRadioGroup) obj;
				radioGroup.populate(convertToNameValuePairs(valuesObj));
			} else if (obj instanceof CustomListView) {
				CustomListView list = (CustomListView) obj;
				list.populate(convertToNameValuePairs(valuesObj));
			} else {
				FLog.w("cannot populate list "
						+ ref);
				showWarning("Logic Error", "Cannot populate list " + ref);
			}
		} catch (Exception e) {
			FLog.e("error populate list " + ref, e);
			showWarning("Logic Error", "Error populate list " + ref);
		}
	}
	
	public void populateCursorList(String ref, String query, int limit) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof CustomListView) {
				CustomListView list = (CustomListView) obj;
				list.populateWithCursor(query, limit);
			} else {
				FLog.w("cannot populate cursor list " + ref);
				showWarning("Logic Error", "Cannot populate cursor list " + ref);
			}
		} catch (Exception e) {
			FLog.e("error populate cursor list " + ref, e);
			showWarning("Logic Error", "Error populate cursor list " + ref);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void populatePictureGallery(String ref, Collection valuesObj) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof PictureGallery) {
				List<Picture> pictures = new ArrayList<Picture>();
				if (valuesObj instanceof List<?>) {
					try {
						ArrayList<List<String>> arrayList = (ArrayList<List<String>>) valuesObj;
						for (List<String> pictureList : arrayList) {
							Picture picture = new Picture(pictureList.get(0),
									arch16n.substituteValue(pictureList.get(1)), module.getDirectoryPath(pictureList.get(2)).getPath());
							pictures.add(picture);
						}
					} catch (Exception e) {
						ArrayList<String> values = (ArrayList<String>) valuesObj;
						for (String value : values) {
							Picture picture = new Picture(value, null, value);
							pictures.add(picture);
						}
					}
				}
				
				PictureGallery gallery = (PictureGallery) obj;
				gallery.populate(pictures);
				gallery.save();
			} else {
				FLog.w("cannot populate picture gallery "
						+ ref);
				showWarning("Logic Error", "Cannot populate picture gallery "
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error populate picture gallery " + ref, e);
			showWarning("Logic Error", "Error populate picture gallery " + ref);
		}
	}
	
	public void populateHierarchicalPictureGallery(String ref, final String attributeName) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof PictureGallery) {
				final HierarchicalPictureGallery gallery = (HierarchicalPictureGallery) obj;
				CancelableTask task = new CancelableTask() {

					private List<VocabularyTerm> terms;

					@Override
					protected Void doInBackground(
							Void... params) {
						try {
							terms = databaseManager.attributeRecord().getVocabularyTerms(attributeName);
							return null;
						} catch (Exception e) {
							FLog.e("Error trying to load vocabulary terms", e);
						}
						return null;
					}
					
					@Override
					protected void onPostExecute(Void result) {
						if (terms == null) {
							FLog.w("Error trying to load vocabulary terms for attribute " + attributeName);
							showWarning("Populate Error", "Error trying to load vocabulary terms");
						} else {
							VocabularyTerm.applyArch16n(terms, arch16n);
							VocabularyTerm.applyProjectDir(terms, module.getDirectoryPath().getPath() + "/");
							gallery.setTerms(terms);
						}
						gallery.save();
					}
					
				};
				task.execute();
			} else {
				FLog.w("cannot populate hierarchical picture gallery "
						+ ref);
				showWarning("Logic Error", "Cannot populate hierarchical picture gallery "
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error populate hierarchical picture gallery " + ref, e);
			showWarning("Logic Error", "Error populate hierarchical picture gallery " + ref);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void populateFileList(String ref, Collection valuesObj) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof FileListGroup) {
				List<NameValuePair> files = new ArrayList<NameValuePair>();
				if (valuesObj instanceof List<?>) {
					ArrayList<String> values = (ArrayList<String>) valuesObj;
					for (String value : values) {
						files.add(new NameValuePair(value, value));
					}
				}
				
				final FileListGroup fileList = (FileListGroup) obj;
				fileList.populate(files);
				fileList.save();
			} else {
				FLog.w("Cannot populate file list " + ref);
				showWarning("Logic Error", "Cannot populate file list " + ref);
			}
		} catch (Exception e) {
			FLog.e("error populate file list " + ref, e);
			showWarning("Logic Error", "Error populate file list " + ref);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void populateCameraPictureGallery(String ref, Collection valuesObj) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof CameraPictureGallery) {
				List<Picture> pictures = new ArrayList<Picture>();
				if (valuesObj instanceof List<?>) {
					ArrayList<String> values = (ArrayList<String>) valuesObj;
					for (String value : values) {
						Picture picture = new Picture(value, null, value);
						pictures.add(picture);
					}
				}
				
				final CameraPictureGallery gallery = (CameraPictureGallery) obj;
				gallery.populateImages(pictures);
				gallery.save();
			} else {
				FLog.w("Cannot populate camera picture gallery "
						+ ref);
				showWarning("Logic Error", "Cannot populate camera picture gallery "
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error populate picture gallery " + ref, e);
			showWarning("Logic Error", "Error populate picture gallery " + ref);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void populateVideoGallery(String ref, Collection valuesObj) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof VideoGallery) {
				List<Picture> pictures = new ArrayList<Picture>();
				if (valuesObj instanceof List<?>) {
					ArrayList<String> values = (ArrayList<String>) valuesObj;
					for (String value : values) {
						Picture picture = new Picture(value, null, value);
						pictures.add(picture);
					}
				}
				
				final VideoGallery gallery = (VideoGallery) obj;
				gallery.populateImages(pictures);
				gallery.save();
			} else {
				FLog.w("Cannot populate video gallery "
						+ ref);
				showWarning("Logic Error", "Cannot populate video gallery "
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error populate video gallery " + ref, e);
			showWarning("Logic Error", "Error populate video gallery " + ref);
		}
	}
	
	public void populateWebView(String ref, String sourceFile) {
		try {
			String html = FileUtil.readFileIntoString(sourceFile);
			populateWebViewHtml(ref, html);
		} catch (Exception e) {
			FLog.e("error populate web view " + ref, e);
			showWarning("Logic Error", "Error populate web view " + ref);
		}
	}
	
	public void populateWebViewHtml(String ref, String html) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof CustomWebView) {
				CustomWebView webView = (CustomWebView) obj;
				
				String moduleFilesPath = "file://" + this.module.getDirectoryPath().toString() + "/";
				webView.setBaseUrl(moduleFilesPath);
				webView.loadDataWithBaseURL(html, "text/html", "utf-8", "");
			} else {
				FLog.w("Cannot populate web view " + ref);
				showWarning("Logic Error", "Cannot populate web view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error populate web view " + ref, e);
			showWarning("Logic Error", "Error populate web view " + ref);
		}
	}
	
	public void navigateWebViewBack(String ref) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof CustomWebView) {
				CustomWebView webView = (CustomWebView) obj;
				webView.goBack();
			} else {
				FLog.w("Cannot navigate web view " + ref);
				showWarning("Logic Error", "Cannot navigate web view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error navigate web view " + ref, e);
			showWarning("Logic Error", "Error navigate web view " + ref);
		}
	}

	public void populateTableRaw(String ref, String query, List<String> headers, String actionName, int actionIndex, String actionCallback) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof Table) {
				Table table = (Table) obj;
				table.populate(query, headers, actionName, actionIndex, actionCallback, false);
			} else {
				FLog.w("Cannot populate table"
						+ ref);
				showWarning("Logic Error", "Cannot populate table"
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error trying to populate table" + ref, e);
			showWarning("Logic Error", "Error trying to populate table" + ref);
		}
	}
	
	public void populateTablePivot(String ref, String query, List<String> headers, String actionName, int actionIndex, String actionCallback) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof Table) {
				Table table = (Table) obj;
				table.populate(query, headers, actionName, actionIndex, actionCallback, true);
			} else {
				FLog.w("Cannot populate table "
						+ ref);
				showWarning("Logic Error", "Cannot populate table "
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error trying to populate table " + ref, e);
			showWarning("Logic Error", "Error trying to populate table " + ref);
		}
	}
	
	public void refreshTable(String ref) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof Table) {
				Table table = (Table) obj;
				table.refresh();
			} else {
				FLog.w("Cannot refresh table "
						+ ref);
				showWarning("Logic Error", "Cannot refresh table "
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error trying to refresh table " + ref, e);
			showWarning("Logic Error", "Error trying to refresh table " + ref);
		}
	}
	
	public void styleTable(String ref, String cssFile) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof Table) {
				Table table = (Table) obj;
				table.style(cssFile);
			} else {
				FLog.w("Cannot style table "
						+ ref);
				showWarning("Logic Error", "Cannot style table "
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error trying to style table " + ref, e);
			showWarning("Logic Error", "Error trying to style table " + ref);
		}
	}
	
	public void scrollTableToTop(String ref) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof Table) {
				Table table = (Table) obj;
				table.scrollToTop();
			} else {
				FLog.w("Cannot scroll table "
						+ ref);
				showWarning("Logic Error", "Cannot scroll table "
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error trying to scroll table " + ref, e);
			showWarning("Logic Error", "Error trying to scroll table " + ref);
		}
	}
	
	public void scrollTableToBottom(String ref) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof Table) {
				Table table = (Table) obj;
				table.scrollToBottom();
			} else {
				FLog.w("Cannot scroll table "
						+ ref);
				showWarning("Logic Error", "Cannot scroll table "
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error trying to scroll table " + ref, e);
			showWarning("Logic Error", "Error trying to scroll table " + ref);
		}
	}
	
	public void scrollTableToRow(String ref, int row) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof Table) {
				Table table = (Table) obj;
				table.scrollToRow(row);
			} else {
				FLog.w("Cannot scroll table "
						+ ref);
				showWarning("Logic Error", "Cannot scroll table "
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error trying to scroll table " + ref, e);
			showWarning("Logic Error", "Error trying to scroll table " + ref);
		}
	}
	
	public ArrayList<NameValuePair> convertToNameValuePairs(Collection<?> valuesObj) throws Exception {
		ArrayList<NameValuePair> pairs = null;
		try {
			@SuppressWarnings("unchecked")
			List<NameValuePair> values = (List<NameValuePair>) valuesObj;
			pairs = new ArrayList<NameValuePair>();
			for (NameValuePair p : values) {
				pairs.add(new NameValuePair(arch16n
						.substituteValue(p.getName()), p.getValue()));
			}
		} catch (Exception e) {
			try {
				@SuppressWarnings("unchecked")
				List<List<String>> values = (List<List<String>>) valuesObj;
				pairs = new ArrayList<NameValuePair>();
				for (List<String> list : values) {
					pairs.add(new NameValuePair(arch16n
							.substituteValue(list.get(1)), list.get(0)));
				}
			} catch (Exception ee) {
				@SuppressWarnings("unchecked")
				List<String> values = (List<String>) valuesObj;
				pairs = new ArrayList<NameValuePair>();
				for (String value : values) {
					pairs.add(new NameValuePair(arch16n
							.substituteValue(value), arch16n
							.substituteValue(value)));
				}
			}
		}
		return pairs;
	}

	public void fetchArchEnt(String entityId, FetchCallback callback) {
		DatabaseHelper.fetchArchEnt(this, entityId, callback, false);
	}

	public void fetchRel(String relationshipId, FetchCallback callback) {
		DatabaseHelper.fetchRel(this, relationshipId, callback, false);
	}

	public void fetchOne(String query, FetchCallback callback) {
		DatabaseHelper.fetchOne(this, query, callback);
	}

	public void fetchAll(String query, FetchCallback callback) {
		DatabaseHelper.fetchAll(this, query, callback);
	}

	public void fetchEntityList(String entityType, FetchCallback callback) {
		DatabaseHelper.fetchEntityList(this, entityType, callback);
	}

	public void fetchRelationshipList(String relationshipType, FetchCallback callback) {
		DatabaseHelper.fetchRelationshipList(this, relationshipType, callback);
	}
	
	public int getGpsUpdateInterval() {
		try {
			return gpsDataManager.getGpsUpdateInterval();
		} catch (Exception e) {
			FLog.e("error trying to get gps interval", e);
			showWarning("Logic Error", "Error trying to get gps interval");
		}
		return -1;
	}

	public void setGpsUpdateInterval(int gpsUpdateInterval) {
		try {
			gpsDataManager.setGpsUpdateInterval(
					gpsUpdateInterval);
		} catch (Exception e) {
			FLog.e("error trying to set gps interval", e);
			showWarning("Logic Error", "Error trying to set gps interval");
		}
	}
	
	public void startExternalGPS() {
		try {
			bluetoothManager.resetConnection(); // make sure we get users to reconnect to the correct device
			gpsDataManager.startExternalGPSListener();	
		} catch (Exception e) {
			FLog.e("error trying to start external gps", e);
			showWarning("Logic Error", "Error trying to start external gps");
		}
	}

	public void startInternalGPS() {
		try {
			gpsDataManager.startInternalGPSListener();
		} catch (Exception e) {
			FLog.e("error trying to start internal gps", e);
			showWarning("Logic Error", "Error trying to start internal gps");
		}
	}
	
	public boolean isInternalGPSOn() {
		try {
			return gpsDataManager.isInternalGPSStarted();
		} catch (Exception e) {
			FLog.e("error checking internal GPS state", e);
			showWarning("Logic Error", "Error checking internal GPS state");
		}
		return false;
	}
	
	public void stopGPS() {
		try {
			gpsDataManager.destroyListener();
		} catch (Exception e) {
			FLog.e("error trying to stop gps", e);
			showWarning("Logic Error", "Error trying to stop gps");
		}
	}
	
	public boolean isExternalGPSOn() {
		try {
			return gpsDataManager.isExternalGPSStarted();
		} catch (Exception e) {
			FLog.e("error checking external GPS state", e);
			showWarning("Logic Error", "Error checking external GPS state");
		}
		return false;
	}

	public Object getGPSPosition() {
		try {
			return gpsDataManager.getGPSPosition();
		} catch (Exception e) {
			FLog.e("error trying to get gps position", e);
			showWarning("Logic Error", "Error trying to get gps position");
		}
		return null;
	}
	
	public Object getGPSPositionProjected() {
		try {
			GPSLocation l = (GPSLocation) gpsDataManager.getGPSPosition();
			if (l == null) return l;
			MapPos p = databaseManager.spatialRecord().convertFromProjToProj(GeometryUtil.EPSG4326, module.getSrid(), new MapPos(l.getLongitude(), l.getLatitude()));
			l.setLongitude(p.x);
			l.setLatitude(p.y);
			return l;
		} catch (Exception e) {
			FLog.e("error trying to get gps projected position", e);
			showWarning("Logic Error", "Error trying to get gps projected position");
		}
		return null;
	}

	public Object getGPSEstimatedAccuracy() {
		try {
			return gpsDataManager.getGPSEstimatedAccuracy();
		} catch (Exception e) {
			FLog.e("error trying to get gps accuracy", e);
			showWarning("Logic Error", "Error trying to get gps accuracy");
		}
		return null;
	}

	public Object getGPSHeading() {
		try {
			return gpsDataManager.getGPSHeading();
		} catch (Exception e) {
			FLog.e("error trying to get gps heading", e);
			showWarning("Logic Error", "Error trying to get gps heading");
		}
		return null;
	}

	public Object getGPSPosition(String gps) {
		try {
			return gpsDataManager.getGPSPosition(gps);
		} catch (Exception e) {
			FLog.e("error trying to get gps position", e);
			showWarning("Logic Error", "Error trying to get gps position");
		}
		return null;
	}

	public Object getGPSEstimatedAccuracy(String gps) {
		try {
			return gpsDataManager.getGPSEstimatedAccuracy(gps);
		} catch (Exception e) {
			FLog.e("error trying to get gps accuracy", e);
			showWarning("Logic Error", "Error trying to get gps accuracy");
		}
		return null;
	}

	public Object getGPSHeading(String gps) {
		try {
			return gpsDataManager.getGPSHeading(gps);
		} catch (Exception e) {
			FLog.e("error trying to get gps position", e);
			showWarning("Logic Error", "Error trying to get gps heading");
		}
		return null;
	}

	public void showBaseMap(final String ref, String layerName,
			String filename) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				final CustomMapView mapView = (CustomMapView) obj;

				String filepath = module.getDirectoryPath(filename).getPath();
				mapView.addBaseMap(layerName, filepath);

			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.e("error showing base map", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error rendering base map", e);
			showWarning("Logic Error", "Error cannot render base map " + ref);
		}
	}
	
	public void showRasterMap(final String ref, String layerName,
			String filename) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				final CustomMapView mapView = (CustomMapView) obj;

				String filepath = module.getDirectoryPath(filename).getPath();
				mapView.addRasterMap(layerName, filepath);

			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.e("error showing raster map", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error rendering raster map", e);
			showWarning("Logic Error", "Error cannot render raster map " + ref);
		}
	}

	public void setMapFocusPoint(String ref, float longitude, float latitude) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				MapPos p = databaseManager.spatialRecord().convertFromProjToProj(module.getSrid(), GeometryUtil.EPSG4326, new MapPos(longitude, latitude));
				mapView.setMapFocusPoint((float) p.x, (float) p.y);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.e("error setting map focus point", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error setting map focus point " + ref, e);
			showWarning("Logic Error", "Error setting map focus point " + ref);
		}
	}

	public void setMapRotation(String ref, float rotation) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				// rotation - 0 = north-up
				mapView.setRotation(rotation);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error setting map rotation " + ref, e);
			showWarning("Logic Error", "Error setting map rotation " + ref);
		}
	}

	public void setMapZoom(String ref, float zoom) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				// zoom - 0 = world, like on most web maps
				mapView.setZoom(zoom);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error setting map zoom " + ref, e);
			showWarning("Logic Error", "Error setting map zoom " + ref);
		}
	}

	public void setMapTilt(String ref, float tilt) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				// tilt means perspective view. Default is 90 degrees for
				// "normal" 2D map view, minimum allowed is 30 degrees.
				mapView.setTilt(tilt);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error setting map tilt " + ref, e);
			showWarning("Logic Error", "Error setting map tilt " + ref);
		}
	}

	public void centerOnCurrentPosition(String ref) {
		Object currentLocation = getGPSPositionProjected();
		if (currentLocation != null) {
			GPSLocation location = (GPSLocation) currentLocation;
			setMapFocusPoint(ref, (float) location.getLongitude(),
					(float) location.getLatitude());
		}
	}

	public int showShapeLayer(String ref, String layerName, String filename,
			GeometryStyle pointStyle, GeometryStyle lineStyle,
			GeometryStyle polygonStyle) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				String filepath = module.getDirectoryPath(filename).getPath();
				return mapView.addShapeLayer(layerName, filepath,
						pointStyle.toPointStyleSet(),
						lineStyle.toLineStyleSet(),
						polygonStyle.toPolygonStyleSet());
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.w("error showing shape layer", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error showing shape layer" + ref, e);
			showWarning("Logic Error", "Error showing shape layer " + ref);
		}
		return 0;
	}

	public int showSpatialLayer(String ref, String layerName, String filename,
			String tablename, String idColumn, String labelColumn,
			GeometryStyle pointStyle, GeometryStyle lineStyle,
			GeometryStyle polygonStyle, GeometryTextStyle textStyle) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				String filepath = module.getDirectoryPath(filename).getPath();
				return mapView.addSpatialLayer(layerName, filepath, tablename,
						idColumn, labelColumn, pointStyle,
						lineStyle,
						polygonStyle,
						textStyle);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.w("error showing spatial layer", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error showing spatial layer" + ref, e);
			showWarning("Logic Error", "Error showing spatial layer " + ref);
		}
		return 0;
	}

	public int showDatabaseLayer(String ref, String layerName,
			boolean isEntity, String queryName, String querySql,
			GeometryStyle pointStyle, GeometryStyle lineStyle,
			GeometryStyle polygonStyle, GeometryTextStyle textStyle) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				return mapView.addDatabaseLayer(layerName, isEntity, queryName,
						querySql, pointStyle,
						lineStyle,
						polygonStyle,
						textStyle);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.w("error showing database layer", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error showing database layer" + ref, e);
			showWarning("Logic Error", "Error showing database layer " + ref);
		}
		return 0;
	}
	
	public void setSelectedLayer(String ref, int layerId) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				mapView.setSelectedLayer(layerId);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.e("error setting selected layer", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error setting selected layer " + layerId, e);
			showWarning("Logic Error", "Error setting selected layer " + layerId);
		}
	}
	
	public void setSelectedLayer(String ref, String layerName) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				mapView.setSelectedLayer(layerName);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.e("error setting selected layer", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error setting selected layer " + layerName, e);
			showWarning("Logic Error", "Error setting selected layer " + layerName);
		}
	}

	public void removeLayer(String ref, int layerId) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				mapView.removeLayer(layerId);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.e("error removing layer", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error removing layer " + ref, e);
			showWarning("Logic Error", "Error removing layer " + ref);
		}
	}

	public int createCanvasLayer(String ref, String layerName) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				return mapView.addCanvasLayer(layerName);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.e("error creating canvas layer", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error creating canvas layer " + ref, e);
			showWarning("Logic Error", "Error creating canvas layer " + ref);
		}
		return 0;
	}

	public void setLayerVisible(String ref, int layerId, boolean visible) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				mapView.setLayerVisible(layerId, visible);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error setting vector layer visiblity " + ref, e);
			showWarning("Logic Error", "Error setting vector layer visibility "
					+ ref);
		}
	}
	
	public void setGdalLayerShowAlways(String ref, String layerName, boolean showAlways) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				mapView.setGdalLayerShowAlways(layerName, showAlways);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error setting gdal layer showalways option " + ref, e);
			showWarning("Logic Error", "Error setting gdal layer showalways option "
					+ ref);
		}
	}

	public int drawPoint(String ref, int layerId, MapPos point,
			GeometryStyle style) {

		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				GeometryData geomData = (GeometryData) mapView.drawPoint(layerId, databaseManager.spatialRecord().convertFromProjToProj(module.getSrid(), GeometryUtil.EPSG4326, point), style).userData;
				return geomData.geomId;
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.e("error drawing point", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error drawing point " + ref, e);
			showWarning("Logic Error", "Error drawing point " + ref);
		}
		return 0;
	}

	public int drawLine(String ref, int layerId, List<MapPos> points,
			GeometryStyle style) {

		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				GeometryData geomData = (GeometryData) mapView.drawLine(layerId, databaseManager.spatialRecord().convertFromProjToProj(module.getSrid(), GeometryUtil.EPSG4326, points), style).userData;
				return geomData.geomId;
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.e("error drawing line", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error drawing line " + ref, e);
			showWarning("Logic Error", "Error drawing line " + ref);
		}
		return 0;
	}

	public int drawPolygon(String ref, int layerId, List<MapPos> points,
			GeometryStyle style) {

		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				GeometryData geomData = (GeometryData) mapView.drawPolygon(layerId, databaseManager.spatialRecord().convertFromProjToProj(module.getSrid(), GeometryUtil.EPSG4326, points), style).userData;
				return geomData.geomId;
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.e("error drawing polygon", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error drawing polygon " + ref, e);
			showWarning("Logic Error", "Error drawing polygon " + ref);
		}
		return 0;
	}

	public void clearGeometry(String ref, int geomId) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				mapView.clearGeometry(geomId);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.e("error clearing geometry", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error clearing geometry " + ref, e);
			showWarning("Logic Error", "Error clearing geometry " + ref);
		}
	}

	public void clearGeometryList(String ref, List<Geometry> geomList) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				mapView.clearGeometryList(geomList);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error clearing geometry list " + ref, e);
			showWarning("Logic Error", "Error clearing geometry list " + ref);
		}
	}

	public List<Geometry> getGeometryList(String ref, int layerId) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				return databaseManager.spatialRecord().convertGeometryFromProjToProj(GeometryUtil.EPSG3857, module.getSrid(), mapView
						.getGeometryList(layerId));
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error getting geometry list " + ref, e);
			showWarning("Logic Error", "Error getting geometry list " + ref);
		}
		return null;
	}

	public Geometry getGeometry(String ref, int geomId) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				return databaseManager.spatialRecord().convertGeometryFromProjToProj(GeometryUtil.EPSG3857, module.getSrid(), mapView
						.getGeometry(geomId));
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error getting geomtry " + ref, e);
			showWarning("Logic Error", "Error getting geometry " + ref);
		}
		return null;
	}
	
	public String getGeometryLayerName(String ref, int geomId) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				return mapView.getGeometryLayerName(geomId);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error getting geomtry layer name " + ref, e);
			showWarning("Logic Error", "Error getting geometry layer name " + ref);
		}
		return null;
	}

	public void lockMapView(String ref, boolean lock) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.setViewLocked(lock);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error locking map view " + ref, e);
			showWarning("Logic Error", "Error locking map view " + ref);
		}
	}

	public void addGeometryHighlight(String ref, int geomId) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.addHighlight(geomId);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.e("error adding highlight", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error adding highlight " + ref, e);
			showWarning("Logic Error", "Error adding highlight " + ref);
		}
	}

	public void removeGeometryHighlight(String ref, int geomId) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.removeHighlight(geomId);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (MapException e) {
			FLog.e("error removing highlight", e);
			showWarning("Logic Error", e.getMessage());
		} catch (Exception e) {
			FLog.e("error removing highlight " + ref, e);
			showWarning("Logic Error", "Error removing highlight " + ref);
		}
	}

	public void clearGeometryHighlights(String ref) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.clearHighlights();
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error clearing higlights " + ref, e);
			showWarning("Logic Error", "Error clearing higlights " + ref);
		}
	}

	public List<Geometry> getGeometryHighlights(String ref) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				return databaseManager.spatialRecord().convertGeometryFromProjToProj(GeometryUtil.EPSG3857, module.getSrid(), mapView
						.getHighlights());
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error getting highlights " + ref, e);
			showWarning("Logic Error", "Error getting highlights " + ref);
		}
		return null;
	}

	public void prepareHighlightTransform(String ref) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.prepareHighlightTransform();
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error preparing highlight transform " + ref, e);
			showWarning("Logic Error", "Error preparing highlight transform "
					+ ref);
		}
	}

	public void doHighlightTransform(String ref) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.doHighlightTransform();
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error do highlight transform " + ref, e);
			showWarning("Logic Error", "Error do highlight transform " + ref);
		}
	}

	public void pushDatabaseToServer(final String callback) {
		this.activityRef.get().uploadDatabaseToServer(callback);
	}

	public void pullDatabaseFromServer(final String callback) {
		this.activityRef.get().downloadDatabaseFromServer(callback);
	}

	public void setSyncEnabled(boolean value) {
		if (value) {
			this.activityRef.get().enableSync();
		} else {
			this.activityRef.get().disableSync();
		}
	}
	
	public boolean isSyncEnabled() {
		try {
			return this.activityRef.get().isSyncEnabled();
		} catch (Exception e) {
			FLog.e("error checking sync state", e);
			showWarning("Logic Error", "Error checking sync state");
		}
		return false;
	}

	public void addSyncListener(final String startCallback,
			final String successCallback, final String failureCallback) {
		this.activityRef.get().addSyncListener(new ShowModuleActivity.SyncListener() {

			@Override
			public void handleStart() {
				execute(startCallback);
			}

			@Override
			public void handleSuccess() {
				execute(successCallback);
			}

			@Override
			public void handleFailure() {
				execute(failureCallback);
			}
		});
	}

	public void openCamera(String callback) {
		cameraCallBack = callback;
		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		cameraPicturepath = Environment.getExternalStorageDirectory() + "/"
				+ Environment.DIRECTORY_DCIM + "/image-"
				+ System.currentTimeMillis() + ".jpg";
		File file = new File(cameraPicturepath);
		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
		this.activityRef.get().startActivityForResult(cameraIntent,
				ShowModuleActivity.CAMERA_REQUEST_CODE);
	}

	public void executeCameraCallBack() {
		try {
			this.interpreter.eval(cameraCallBack);
		} catch (Exception e) {
			FLog.e("error when executing the callback for the camera", e);
		}
	}

	public void openVideo(String callback) {
		videoCallBack = callback;
		Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		cameraVideoPath = Environment.getExternalStorageDirectory() + "/"
				+ Environment.DIRECTORY_DCIM + "/video-"
				+ System.currentTimeMillis() + ".mp4";
		File file = new File(cameraVideoPath);
		videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
		activityRef.get().startActivityForResult(videoIntent,
				ShowModuleActivity.VIDEO_REQUEST_CODE);
	}

	public void executeVideoCallBack() {
		try {
			this.interpreter.eval(videoCallBack);
		} catch (Exception e) {
			FLog.e("error when executing the callback for the video", e);
		}
	}

	public void recordAudio(String callback) {
		audioCallBack = callback;
		audioFileNamePath = Environment.getExternalStorageDirectory()
				+ "/audio-" + System.currentTimeMillis() + ".mp4";
		AlertDialog.Builder builder = new AlertDialog.Builder(activityRef.get());

		builder.setTitle("FAIMS recording");

		LinearLayout layout = new LinearLayout(activityRef.get());
		layout.setOrientation(LinearLayout.VERTICAL);

		builder.setView(layout);
		ToggleButton button = new ToggleButton(activityRef.get());
		button.setTextOn("Stop Recording");
		button.setTextOff("Start Recording");
		button.setChecked(false);
		layout.addView(button);
		builder.setNeutralButton("Done", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				if (recorder != null) {
					stopRecording();
					executeAudioCallBack();
				}
			}

		});
		final AlertDialog dialog = builder.create();
		button.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					startRecording();
				} else {
					stopRecording();
					executeAudioCallBack();
					dialog.dismiss();
				}
			}
		});
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				if (recorder != null) {
					stopRecording();
					executeAudioCallBack();
				}
			}
		});
		dialog.show();
	}

	private void startRecording() {
		recorder = new MediaRecorder();
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setOutputFile(audioFileNamePath);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			recorder.prepare();
		} catch (IOException e) {
			FLog.e("prepare() failed", e);
		}

		recorder.start();
	}

	private void stopRecording() {
		recorder.stop();
		recorder.release();
		recorder = null;
	}

	public void executeAudioCallBack() {
		try {
			this.interpreter.eval(audioCallBack);
		} catch (Exception e) {
			FLog.e("error when executing the callback for the audio", e);
		}
	}

	public String getLastAudioFilePath() {
		return audioFileNamePath;
	}

	public String getLastVideoFilePath() {
		return cameraVideoPath;
	}

	public String getLastPictureFilePath() {
		return cameraPicturepath;
	}
	
	public String getLastScanContents() {
		return scanContents;
	}
	
	public void setLastScanContents(String contents) {
		this.scanContents = contents;
	}
	
	public String getHardwareBufferContents() {
		return hardwareBufferContents;
	}
	
	public void setHardwareBufferContents(String contents) {
		this.hardwareBufferContents = contents;
	}
	
	public String getConnectedServer() {
		if (serverDiscovery.isServerHostValid()) {
			return serverDiscovery.getPlainServerHost();
		} else {
			return null;
		}
	}
	
	public Module getModule() {
		return this.module;
	}

	public String getModuleName() {
		return this.module.getName();
	}
	
	public String getModuleVersion() {
		return this.module.getVersion();
	}
	
	public String getModuleSrid() {
		return this.module.getSrid();
	}

	public String getModuleId() {
		return this.module.getKey();
	}

	public String getModuleSeason() {
		return this.module.getSeason();
	}

	public String getProjectDescription() {
		return this.module.getDescription();
	}

	public String getPermitNo() {
		return this.module.getPermitNo();
	}

	public String getPermitHolder() {
		return this.module.getPermitHolder();
	}

	public String getContactAndAddress() {
		return this.module.getContactAndAddress();
	}

	public String getParticipants() {
		return this.module.getParticipants();
	}
	
	public String getPermitIssuedBy() {
		return this.module.getPermitIssuedBy();
	}

	public String getPermitType() {
		return this.module.getPermitType();
	}

	public String getCopyrightHolder() {
		return this.module.getCopyrightHolder();
	}

	public String getClientSponsor() {
		return this.module.getClientSponsor();
	}

	public String getLandOwner() {
		return this.module.getLandOwner();
	}

	public String hasSensitiveData() {
		return this.module.hasSensitiveData();
	}

	public String getCurrentTime() {
		return DateUtil.getCurrentTimestampGMT();
	}

	public void setSyncMinInterval(float value) {
		if (value < 0) {
			showWarning("Logic Error", "Invalid sync min interval " + value);
			return;
		}

		this.activityRef.get().setSyncMinInterval(value);
	}

	public void setSyncMaxInterval(float value) {
		if (value < 0 || value < this.activityRef.get().getSyncMinInterval()) {
			showWarning("Logic Error", "Invalid sync max interval " + value);
			return;
		}

		this.activityRef.get().setSyncMaxInterval(value);
	}

	public void setSyncDelay(float value) {
		if (value < 0) {
			showWarning("Logic Error", "Invalid sync delay " + value);
			return;
		}
		this.activityRef.get().setSyncDelay(value);
	}

	public void setUser(User user) {
		databaseManager.setUserId(user.getUserId());
	}

	public void showFileBrowser(String callback) {
		this.lastFileBrowserCallback = callback;
		this.activityRef.get().showFileBrowser(ShowModuleActivity.FILE_BROWSER_REQUEST_CODE);
	}

	public void setLastSelectedFile(File file) {
		try {
			interpreter.set("_last_selected_filename", file.getName());
			interpreter.set("_last_selected_filepath", file.getAbsolutePath());
			this.execute(lastFileBrowserCallback);
		} catch (Exception e) {
			FLog.e("error setting last selected file", e);
		}
	}

	public GeometryStyle createPointStyle(int minZoom, int color, float size,
			float pickingSize) {
		GeometryStyle style = new GeometryStyle(minZoom);
		style.pointColor = color;
		style.size = size;
		style.pickingSize = pickingSize;
		return style;
	}

	public GeometryStyle createLineStyle(int minZoom, int color, float width,
			float pickingWidth, GeometryStyle pointStyle) {
		GeometryStyle style = new GeometryStyle(minZoom);
		style.lineColor = color;
		style.width = width;
		style.pickingWidth = pickingWidth;
		if (pointStyle != null) {
			style.showPoints = true;
			style.pointColor = pointStyle.pointColor;
			style.size = pointStyle.size;
			style.pickingSize = pointStyle.pickingSize;
		}
		return style;
	}

	public GeometryStyle createPolygonStyle(int minZoom, int color,
			GeometryStyle lineStyle) {
		GeometryStyle style = new GeometryStyle(minZoom);
		style.polygonColor = color;
		if (lineStyle != null) {
			style.showStroke = true;
			style.lineColor = lineStyle.lineColor;
			style.width = lineStyle.width;
			style.pickingWidth = lineStyle.pickingWidth;
		}
		return style;
	}

	public GeometryTextStyle createTextStyle(int minZoom, int color, int size,
			Typeface font) {
		GeometryTextStyle style = new GeometryTextStyle(minZoom);
		style.color = color;
		style.size = size;
		style.font = font;
		return style;
	}

	public void setFileSyncEnabled(boolean enabled) {
		if (enabled) {
			activityRef.get().enableFileSync();
		} else {
			activityRef.get().disableFileSync();
		}
	}
	
	public boolean isFileSyncEnabled() {
		try {
			return activityRef.get().isFileSyncEnabled();
		} catch (Exception e) {
			FLog.e("error checking file sync state", e);
			showWarning("Logic Error", "Error checking file sync state");
		}
		return false;
	}

	public String attachFile(String filePath, boolean sync, String dir, final String callback) {
		return attachFile(filePath, sync, dir, callback, null);
	}
	
	public String attachFile(String filePath, boolean sync, String dir, final String callback, String attributeName) {
		try {
			File file = new File(filePath);
			if (!file.exists()) {
				showWarning("Logic Error", "Attach file cannot find file " + filePath);
				return null;
			}

			String attachFile = "";

			if (sync) {
				attachFile += activityRef.get().getResources().getString(
						R.string.app_dir);
			} else {
				attachFile += activityRef.get().getResources().getString(
						R.string.server_dir);
			}

			if (dir != null && !"".equals(dir)) {
				attachFile += "/" + dir;
			}

			// create directories
			FileUtil.makeDirs(module.getDirectoryPath(attachFile));
			String name = (attributeName != null && 
					databaseManager.fetchRecord().hasThumbnail(attributeName)) ? FileUtil.addOriginalExtToFile(file) : file.getName();
			 
			// create random file path
			attachFile += "/" + UUID.randomUUID() + "_" + name;
			
			final String atttachFileFinal = attachFile;
			final boolean syncFinal = sync;
			final File fileFinal = file;

			activityRef.get().copyFile(filePath, module.getDirectoryPath(attachFile).getPath(), new ShowModuleActivity.AttachFileListener() {

						@Override
						public void handleComplete() {
							try {
								// add file to database
								databaseManager.fileRecord().insertFile(atttachFileFinal, syncFinal, fileFinal);
								
								if (callback != null) {
									execute(callback);
								}
							} catch (Exception e) {
								reportError(e);
							}
						}
				
			});
			
			if(!activityRef.get().getSyncStatus().equals(SyncStatus.INACTIVE)){
				activityRef.get().setSyncStatus(ShowModuleActivity.SyncStatus.ACTIVE_HAS_CHANGES);
			}
			
			return attachFile;
		} catch (Exception e) {
			reportError("Error trying to attach file " + filePath, e);
			return null;
		}
	}

	public void viewArchEntAttachedFiles(String entityId) {
		if (entityId == null) {
			showWarning("Attached Files",
					"Please load/save a record to see attached files");
		} else {
			fetchArchEnt(entityId, new FetchCallback() {

				@Override
				public void onError(String message) {
					showWarning("Attached Files", "Cannot load arch entity");
				}

				@Override
				public void onFetch(Object result) {
					ArchEntity entity = (ArchEntity) result;
					List<String> attachedFiles = new ArrayList<String>();
					for (EntityAttribute attribute : entity.getAttributes()) {
						if ("file".equalsIgnoreCase(attribute.getType())) {
							if (!attribute.isDeleted()) {
								attachedFiles.add(attribute.getMeasure());
							}
						}
					}
					viewAttachedFiles(attachedFiles);
				}
				
			});
			
		}
	}

	public void viewRelAttachedFiles(String relationshipId) {
		if (relationshipId == null) {
			showWarning("Attached Files",
					"Please load/save a record to see attached files");
		} else {
			fetchRel(relationshipId, new FetchCallback() {

				@Override
				public void onError(String message) {
					showWarning("Attached Files", "Cannot load relationship");
				}

				@Override
				public void onFetch(Object result) {
					Relationship relationship = (Relationship) result;
					List<String> attachedFiles = new ArrayList<String>();
					for (RelationshipAttribute attribute : relationship
							.getAttributes()) {
						if ("file".equalsIgnoreCase(attribute.getType())) {
							if (!attribute.isDeleted()) {
								attachedFiles.add(attribute.getText());
							}
						}
					}
					viewAttachedFiles(attachedFiles);
				}
				
			});			
		}
	}

	private void viewAttachedFiles(List<String> files) {
		if (files.isEmpty()) {
			showWarning("Attached Files",
					"There is no attached file for the record");
		} else {
			final ListView listView = new ListView(activityRef.get());
			List<NameValuePair> attachedFiles = new ArrayList<NameValuePair>();
			Map<String, Integer> count = new HashMap<String, Integer>();
			for (String attachedFile : files) {
				String filename = module.getDirectoryPath(attachedFile).getName();
				filename = filename.substring(filename.indexOf("_") + 1);
				if (count.get(filename) != null) {
					int fileCount = count.get(filename);
					count.put(filename, fileCount + 1);
					int index = filename.indexOf(".");
					filename = filename.substring(0, index) + "(" + fileCount
							+ ")" + filename.substring(index);
				} else {
					count.put(filename, 1);
				}
				NameValuePair file = new NameValuePair(filename,
						module.getDirectoryPath(attachedFile).getPath());
				attachedFiles.add(file);
			}
			ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
					activityRef.get(), android.R.layout.simple_list_item_1,
					attachedFiles);
			listView.setAdapter(arrayAdapter);
			listView.setOnItemClickListener(new ListView.OnItemClickListener() {

				@SuppressLint("DefaultLocale")
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int index, long arg3) {
					NameValuePair pair = (NameValuePair) listView
							.getItemAtPosition(index);
					File file = new File(pair.getValue());
					if (file.exists()) {
						MimeTypeMap map = MimeTypeMap.getSingleton();
						String ext = MimeTypeMap.getFileExtensionFromUrl(file
								.getName());
						String type = map.getMimeTypeFromExtension(ext
								.toLowerCase());

						if (type == null)
							type = "*/*";

						try {
							Intent intent = new Intent(Intent.ACTION_VIEW);
							Uri data = Uri.fromFile(file);

							intent.setDataAndType(data, type);

							activityRef.get().startActivity(intent);
						} catch (Exception e) {
							FLog.e("Can not open file with the extension", e);
							Intent intent = new Intent(Intent.ACTION_VIEW);
							Uri data = Uri.fromFile(file);

							intent.setDataAndType(data, "*/*");

							activityRef.get().startActivity(intent);
						}
					} else {
						showPreviewWarning("Attached File", file);
					}
				}

			});
			AlertDialog.Builder builder = new AlertDialog.Builder(this.activityRef.get());

			builder.setTitle("Attached Files");
			builder.setView(listView);
			builder.setNeutralButton("Done",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
			builder.create().show();
		}
	}
	
	public String getPreviewText(String title, File file) {
		if (file.getPath().contains("files/server")) {
			if (CameraPictureGallery.IMAGE_PREVIEW.equals(title)) {
				return "This image can only be viewed on the server.";
			} else if (VideoGallery.VIDEO_PREVIEW.equals(title)) {
				return "This video can only be viewed on the server.";
			} else {
				return "This file can only be viewed on the server.";
			}
		} else {
			if (file.getPath().contains(FileUtil.ORIGINAL_EXT)) {
				if (CameraPictureGallery.IMAGE_PREVIEW.equals(title)) {
					return "Large image can be only viewed on the server.";
				} else if (VideoGallery.VIDEO_PREVIEW.equals(title)) {
					return "Entire video can only be viewed on the server.";
				} else {
					return "Full file can only be viewed on the server.";
				}
			} else {
				if (CameraPictureGallery.IMAGE_PREVIEW.equals(title)) {
					return "The image has not completed syncing to your device.";
				} else if (VideoGallery.VIDEO_PREVIEW.equals(title)) {
					return "The video has not completed syncing to your device.";
				} else {
					return "The file has not completed syncing to your device.";
				}
			}
		}
	}
	
	public void showPreviewWarning(String title, File file) {
		showWarning(title, getPreviewText(title, file));
	}

	public Geometry createGeometryPoint(MapPos point) {
		return new Point(point, null, createPointStyle(0, 0, 0, 0).toPointStyleSet(), null);
	}

	public void setToolsEnabled(String ref, boolean enabled) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.setToolsEnabled(enabled);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error setting tools enabled value " + ref, e);
			showWarning("Logic Error", "Error setting tools enabled value "
					+ ref);
		}
	}

	public void addDatabaseLayerQuery(String ref, String name, String sql) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.addDatabaseLayerQuery(name, sql);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error adding database layer query " + ref, e);
			showWarning("Logic Error", "Error adding database layer query "
					+ ref);
		}
	}

	public void addSelectQueryBuilder(String ref, String name,
			QueryBuilder builder) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.addSelectQueryBuilder(name, builder);
				mapView.setDatabaseToolVisible(true);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error adding select query builder " + ref, e);
			showWarning("Logic Error", "Error adding select query builder "
					+ ref);
		}
	}

	public void addLegacySelectQueryBuilder(String ref, String name,
			String dbPath, String tableName, LegacyQueryBuilder builder) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				String filepath = module.getDirectoryPath(dbPath).getPath();
				mapView.setLegacyToolVisible(true);
				mapView.addLegacySelectQueryBuilder(name, filepath, tableName,
						builder);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error adding legacy select query builder " + ref, e);
			showWarning("Logic Error",
					"Error adding legacy select query builder " + ref);
		}
	}

	public void addTrackLogLayerQuery(String ref, String name, String sql) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.addTrackLogLayerQuery(name, sql);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error adding track log query " + ref, e);
			showWarning("Logic Error", "Error adding track log query " + ref);
		}
	}
	
	public MapPos convertFromProjToProj(String fromSrid, String toSrid, MapPos p) {
		try {
			return databaseManager.spatialRecord().convertFromProjToProj(fromSrid, toSrid, p);
		} catch (Exception e) {
			FLog.e("error converting module from " + fromSrid + " to " + toSrid, e);
			showWarning("Logic Error", "Error converting projection from " + fromSrid + " to " + toSrid);
		}
		return null;
	}
	
	public void bindToolEvent(String ref, String type, final String callback) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				if ("create".equals(type)) {
					mapView.setCreateCallback(callback);
				} else if ("load".equals(type)) {
					mapView.setLoadCallback(callback);	
				} else {
					FLog.w("Error cannot bind to tool event " + type);
					showWarning("Logic Error", "Error cannot bind to tool event " + type);
				}
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error binding tool event " + ref, e);
			showWarning("Logic Error",
					"Error binding tool event " + ref);
		}
	}
	
	public void refreshMap(String ref) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.refreshMap();
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		} catch (Exception e) {
			FLog.e("error refreshing map " + ref, e);
			showWarning("Logic Error", "Error refreshing map " + ref);
		}
	}
	
	public boolean isAttachingFiles() {
		try {
			return activityRef.get().getCopyFileCount() > 0;
		} catch (Exception e) {
			FLog.e("error checking for attached files", e);
			showWarning("Logic Error", "Error checking for attached files");
		}
		return false;
	}
	
	public String getAttachedFilePath(String file) {
		try {
			return module.getDirectoryPath(file).getPath();
		} catch (Exception e) {
			FLog.e("error getting attached file path", e);
			showWarning("Logic Error", "Error getting attached file path");
		}
		return null;
	}
	
	public String stripAttachedFilePath(String file) {
		try {
			return file.replace(module.getDirectoryPath().getPath() + "/", "");
		} catch (Exception e) {
			FLog.e("error stripping attached file path", e);
			showWarning("Logic Error", "Error stripping attached file path");
		}
		return null;
	}
	
	public String addFile(String ref, String file) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof FileListGroup) {
				FileListGroup filesList = (FileListGroup) obj;
				filesList.addFile(file);
			} else {
				FLog.w("cannot add file to view " + obj);
				showWarning("Logic Error", "Cannot add file to view " + obj);
			}
		} catch (Exception e) {
			FLog.e("error adding file to list", e);
			showWarning("Logic Error", "Error adding file to list");
		}
		return null;
	}
	
	public String addPicture(String ref, String file) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof CameraPictureGallery) {
				CameraPictureGallery gallery = (CameraPictureGallery) obj;
				gallery.addFile(file);
			}else {
				FLog.w("cannot add picture to view " + obj);
				showWarning("Logic Error", "Cannot add picture to view " + obj);
			}
		} catch (Exception e) {
			FLog.e("error adding picture to gallery", e);
			showWarning("Logic Error", "Error adding picture to gallery");
		}
		return null;
	}
	
	public String addVideo(String ref, String file) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			if (obj instanceof VideoGallery) {
				VideoGallery gallery = (VideoGallery) obj;
				gallery.addFile(file);
			} else {
				FLog.w("cannot add video to view " + obj);
				showWarning("Logic Error", "Cannot add video to view " + obj);
			}
		} catch (Exception e) {
			FLog.e("error adding video to gallery", e);
			showWarning("Logic Error", "Error adding video to gallery");
		}
		return null;
	}
	
	public void scanCode(String callback) {
		scanCallBack = callback;
		
		Intent scanIntent = new Intent("com.google.zxing.client.android.SCAN");
		//Scan for both barcodes and QR codes, and remove result display pause
		scanIntent.putExtra("SCAN_MODE", "SCAN_MODE");
		scanIntent.putExtra("RESULT_DISPLAY_DURATION_MS", 0L);
		this.activityRef.get().startActivityForResult(scanIntent, ShowModuleActivity.SCAN_CODE_CODE);
	}
	
	public void executeScanCallBack() {
		execute(scanCallBack);
	}
	
	public void createBluetoothConnection(final String callback, int interval) {
		bluetoothManager.resetConnection(); // make sure we get users to reconnect to the correct device
		bluetoothManager.createConnection(new BluetoothManager.BluetoothListener() {
			
			@Override
			public void onInput(String input) {
				set("_bluetooth_message", input);
				execute(callback);
			}
			
			@Override
			public void onConnect() {	
			}
			
			@Override
			public void onDisconnect() {	
			}
		}, interval * 1000);
	}
	
	public void destroyBluetoothConnection() {
		bluetoothManager.destroyConnection();
	}
	
	public boolean isBluetoothConnected() {
		try {
			return bluetoothManager.isBluetoothConnected();
		} catch (Exception e) {
			FLog.e("error checking bluetooth state", e);
			showWarning("Logic Error", "Error checking bluetooth state");
		}
		return false;
	}
	
	public void readBluetoothMessage() {
		bluetoothManager.readMessage();
	}
	
	public void writeBluetoothMessage(String message) {
		bluetoothManager.writeMessage(message);
	}
	
	public void clearBluetoothMessages() {
		bluetoothManager.clearMessages();
	}
	
	@Override
	public void saveTo(Bundle savedInstanceState) throws Exception {
		String persistedObjectName = getPersistedObjectName();
		if (persistedObjectName != null) {
			try {
				Object persistedObject = interpreter.get(persistedObjectName);
				savedInstanceState.putSerializable(persistedObjectName,
						(Serializable) persistedObject);
			} catch (Exception e) {
				FLog.e("error storing bean shell data", e);
			}
		}
		
		BeanShellSerializer serializer = new BeanShellSerializer(interpreter);
		savedInstanceState.putSerializable(TAG + "beanshellVariables", serializer.getVariables());
		
		// Beanshell linker variables
		savedInstanceState.putString(TAG + "persistedObjectName", persistedObjectName);
		savedInstanceState.putString(TAG + "lastFileBrowserCallback", lastFileBrowserCallback);
		savedInstanceState.putDouble(TAG + "prevLong", prevLong);
		savedInstanceState.putDouble(TAG + "prevLat", prevLat);
		savedInstanceState.putString(TAG + "textAlertInput", textAlertInput);
		savedInstanceState.putString(TAG + "datetAlertInput", dateAlertInput);
		savedInstanceState.putString(TAG + "timeAlertInput", timeAlertInput);
		savedInstanceState.putString(TAG + "cameraPicturepath", cameraPicturepath);
		savedInstanceState.putString(TAG + "cameraCallBack", cameraCallBack);
		savedInstanceState.putString(TAG + "videoCallBack", videoCallBack);
		savedInstanceState.putString(TAG + "cameraVideoPath", cameraVideoPath);
		savedInstanceState.putString(TAG + "audioFileNamePath", audioFileNamePath);
		savedInstanceState.putString(TAG + "audioCallBack", audioCallBack);
		savedInstanceState.putString(TAG + "scanContents", scanContents);
		savedInstanceState.putString(TAG + "scanCallBack", scanCallBack);
		savedInstanceState.putString(TAG + "hardwareBufferContents", hardwareBufferContents);
		savedInstanceState.putString(TAG + "hardwareReadingCallBack", hardwareReadingCallBack);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void restoreFrom(Bundle savedInstanceState) {
		// Beanshell variables
		beanshellVariables = (HashMap<String, Serializable>) savedInstanceState.getSerializable(TAG + "beanshellVariables");
		
		// Beanshell linker variables
		persistedObjectName = savedInstanceState.getString(TAG + "persistedObjectName");
		lastFileBrowserCallback = savedInstanceState.getString(TAG + "lastFileBrowserCallback");
		prevLong = savedInstanceState.getDouble(TAG + "prevLong");
		prevLat = savedInstanceState.getDouble(TAG + "prevLat");
		textAlertInput = savedInstanceState.getString(TAG + "textAlertInput");
		cameraPicturepath = savedInstanceState.getString(TAG + "cameraPicturepath");
		cameraCallBack = savedInstanceState.getString(TAG + "cameraCallBack");
		videoCallBack = savedInstanceState.getString(TAG + "videoCallBack");
		cameraVideoPath = savedInstanceState.getString(TAG + "cameraVideoPath");
		audioFileNamePath = savedInstanceState.getString(TAG + "audioFileNamePath");
		audioCallBack = savedInstanceState.getString(TAG + "audioCallBack");
		scanContents = savedInstanceState.getString(TAG + "scanContents");
		scanCallBack = savedInstanceState.getString(TAG + "scanCallBack");
		hardwareBufferContents = savedInstanceState.getString(TAG + "hardwareBufferContents");
		hardwareReadingCallBack = savedInstanceState.getString(TAG + "hardwareReadingCallBack");
		
		if (persistedObjectName != null) {
			Object object = savedInstanceState
					.getSerializable(persistedObjectName);
			try {
				interpreter.set(persistedObjectName, object);
			} catch (Exception e) {
				FLog.e("error restoring bean shell data", e);
			}
		}
	}
	
	public void restoreTempBundle() throws Exception {
		if (beanshellVariables != null) {
			BeanShellSerializer serializer = new BeanShellSerializer(interpreter);
			serializer.setVariables(beanshellVariables);;
		}
	}
	
	@Override
	public void pause() {
	}
	
	@Override
	public void resume() {
	}
	
	@Override
	public void destroy() {
		stopTrackingGPS();
	}

	public void debugHardwareDevices(boolean enabled) {
		this.activityRef.get().setHardwareDebugMode(enabled);
	}
	
	public ArrayList<String> getHardwareDevices() {
		ArrayList<String> devices  = new ArrayList<String>();
		for(int deviceId : InputDevice.getDeviceIds()) {
			devices.add(InputDevice.getDevice(deviceId).getName());
		}
		
		return devices;
	}
	
	public void captureHardware(String deviceName, String delimiter, String callback) {
		this.activityRef.get().setHardwareToCapture(deviceName);
		clearHardwareDeviceBuffer();
		this.activityRef.get().setHardwareDelimiter(delimiter.charAt(0));
		hardwareReadingCallBack = callback;
	}
	
	public void executeHardwareCaptureCallBack() {
		execute(hardwareReadingCallBack);
	}
	
	public void clearHardwareDeviceBuffer() {
		this.activityRef.get().clearDeviceBuffer();
	}
	
	public void executeViewTask(final ViewTask task) {
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				try {
					task.doTask();
				} catch (Exception e) {
					reportError("Error found when executing execute view task dotask callback", e);
				}
			}
			
		});
	}
	
	public boolean hasViewOrContainer(String ref) {
		try {
			Tab tab = getTabFromRef(ref);
			return tab.hasViewOrContainer(ref);
		} catch (Exception e) {
			reportError("Error trying check if view or container exists" + ref, e);
			return false;
		}
	}
	
	public void createView(String ref, FormInputDef inputDef, String containerRef) {
		try {
			TabGroup tabGroup = getTabGroupFromRef(ref);
			Tab tab = getTabFromRef(ref);
			tab.addCustomView(ref, inputDef, tabGroup.isArchEnt(), tabGroup.isRelationship(), containerRef);
		} catch (Exception e) {
			reportError("Error trying to create view " + ref, e);
		}
	}
	
	public void removeView(String ref) {
		try {
			Tab tab = getTabFromRef(ref);
			tab.removeCustomView(ref);
		} catch (Exception e) {
			reportError("Error trying to remove view " + ref, e);
		}
	}
	
	public void createContainer(String ref, String style, String containerRef) {
		try {
			Tab tab = getTabFromRef(ref);
			tab.addCustomContainer(ref, style, containerRef);
		} catch (Exception e) {
			reportError("Error trying to create container " + ref, e);
		}
	}
	
	public void removeContainer(String ref) {
		try {
			final Tab tab = getTabFromRef(ref);
			tab.removeCustomContainer(ref);
		} catch (Exception e) {
			reportError("Error trying to remove container " + ref, e);
		}
	}
	
	public void removeAllViewsAndContainers(String tabGroupRef) {
		try {
			final TabGroup tabGroup = getTabGroup(tabGroupRef);
			tabGroup.removeCustomViews();
			tabGroup.removeCustomContainers();
		} catch (Exception e) {
			reportError("Error trying to remove all views and containers " + tabGroupRef, e);
		}
	}
	
	public void addActionBarItem(String name, ActionButtonCallback callback) {
		activityRef.get().addActionBarItem(name, callback);
	}
	
	public void removeActionBarItem(String name) {
		activityRef.get().removeActionBarItem(name);
	}
	
	public void refreshActionBarMenu() {
		activityRef.get().updateStatusBar();
	}
	
	public void addNavigationButton(String name, ActionButtonCallback callback, String type) {
		try {
			navigationDrawer.addNavigationAction(name, callback, type);
		} catch (Exception e) {
			reportError("Error trying to add navigation button action", e);
		}
	}
	
	public void removeNavigationButton(String name) {
		try {
			navigationDrawer.removeNavigationAction(name);
		} catch (Exception e) {
			reportError("Error trying to remove navigation button action", e);
		}
	}
	
	public Dialog cleanSyncedFiles() {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(this.activityRef.get());
			
			final ArrayList<FileInfo> filesWithThumbnails = databaseManager.fileRecord().getSyncedFiles(true);
			final ArrayList<FileInfo> allFiles = databaseManager.fileRecord().getSyncedFiles(false);
			
			builder.setTitle("Clean Synced Files");
			builder.setMessage("Deleting all synced files (" + FileUtil.calculateExistingFileCount(module, filesWithThumbnails) + " file/s) which have thumbnails will clean approximately: " +
					FileUtil.calculateTotalFileSpace(module, filesWithThumbnails) + "MB" + ".\nDeleting all synced files (" +
					FileUtil.calculateExistingFileCount(module, allFiles) + " file/s) will clean approximately: " + FileUtil.calculateTotalFileSpace(module, allFiles) + "MB");
			builder.setNeutralButton("Delete all synced files which have thumbnails", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					cleanFiles(filesWithThumbnails);
				}
			});
			builder.setPositiveButton("Delete all synced files", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					cleanFiles(allFiles);
				}
			});
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// do nothing
				}
			});
	
			Dialog dialog = builder.create();
			dialog.show();
			return dialog;
		} catch (Exception e) {
			reportError("Error trying to show cleaning synced files dialog", e);
		}
		return null;
	}
	
	private void cleanFiles(final ArrayList<FileInfo> files) {
		final Dialog loading = showBusy("Deleting Files", "Deleting files");
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					ModuleUtil.cleanSyncedFiles(module, files);
				} catch (Exception e) {
					reportError("Error trying to clean out synced files", e);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (loading != null) {
					loading.dismiss();
				}
			}
		}.execute();
	}
}