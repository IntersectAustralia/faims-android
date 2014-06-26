package au.org.intersect.faims.android.beanshell;

import java.io.File;
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
import android.provider.MediaStore;
import android.view.InputDevice;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ToggleButton;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.ArchEntity;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.EntityAttribute;
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
import au.org.intersect.faims.android.nutiteq.GeometryData;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.nutiteq.GeometryTextStyle;
import au.org.intersect.faims.android.nutiteq.WKTUtil;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity.SyncStatus;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.LegacyQueryBuilder;
import au.org.intersect.faims.android.ui.map.QueryBuilder;
import au.org.intersect.faims.android.ui.view.CameraPictureGallery;
import au.org.intersect.faims.android.ui.view.CustomButton;
import au.org.intersect.faims.android.ui.view.CustomCheckBoxGroup;
import au.org.intersect.faims.android.ui.view.CustomListView;
import au.org.intersect.faims.android.ui.view.CustomRadioGroup;
import au.org.intersect.faims.android.ui.view.CustomSpinner;
import au.org.intersect.faims.android.ui.view.FileListGroup;
import au.org.intersect.faims.android.ui.view.HierarchicalPictureGallery;
import au.org.intersect.faims.android.ui.view.HierarchicalSpinner;
import au.org.intersect.faims.android.ui.view.ICustomView;
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
import bsh.EvalError;
import bsh.Interpreter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;

@Singleton
public class BeanShellLinker implements IFAIMSRestorable {
	
	@Inject
	DatabaseManager databaseManager;
	
	@Inject
	GPSDataManager gpsDataManager;
	
	@Inject
	BluetoothManager bluetoothManager;
	
	@Inject
	FileManager fileManager;
	
	@Inject
	UIRenderer uiRenderer;
	
	@Inject
	Arch16n arch16n;
	
	@Inject
	AutoSaveManager autoSaveManager;

	private Interpreter interpreter;

	private WeakReference<ShowModuleActivity> activityRef;
	
	private Module module;

	private Handler trackingHandler;
	private Runnable trackingTask;
	private MediaRecorder recorder;

	protected Dialog saveDialog;
	
	private String persistedObjectName;
	
	private String lastFileBrowserCallback;
	
	private Double prevLong;
	private Double prevLat;

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
		this.lastFileBrowserCallback = null;
		this.trackingTask = null;
		this.prevLong = 0d;
		this.prevLat = 0d;
		this.cameraPicturepath = null;
		this.cameraCallBack = null;
		this.videoCallBack = null;
		this.cameraVideoPath = null;
		this.audioFileNamePath = null;
		this.recorder = null;
		this.audioCallBack = null;
		this.scanContents = null;
		this.scanCallBack = null;
		this.saveDialog = null;
		
		try {
			interpreter.set("linker", this);
		} catch (EvalError e) {
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
		} catch (EvalError e) {
			FLog.i("error executing code", e);
			showWarning("Logic Error", "Error encountered in logic script");
		}
	}
	
	public void executeOnUiThread(final String code) {
		activityRef.get().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				try {
					interpreter.eval(code);
				} catch (EvalError e) {
					FLog.i("error executing code", e);
					showWarning("Logic Error", "Error encountered in logic script");
				}
			}
			
		});
	}

	public void startTrackingGPS(final String type, final int value, final String callback) {
		FLog.d("gps tracking is started");
		
		gpsDataManager.setTrackingType(type);
		gpsDataManager.setTrackingValue(value);
		gpsDataManager.setTrackingExec(callback);

		if (trackingHandler == null) {
			if (!gpsDataManager.isExternalGPSStarted()
					&& !gpsDataManager
							.isInternalGPSStarted()) {
				showWarning("GPS", "No GPS is being used");
				return;
			}
			gpsDataManager.setTrackingStarted(true);
			this.activityRef.get().invalidateOptionsMenu();
			trackingHandler = new Handler(activityRef.get().getMainLooper());
			if ("time".equals(type)) {
				trackingTask = new Runnable() {

					@Override
					public void run() {
						trackingHandler.postDelayed(this, value * 1000);
						if (getGPSPosition() != null) {
							activityRef.get().runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
									execute(callback);
								}
							});
						} else {
							showToast("No GPS signal");
						}
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
							showToast("No GPS signal");
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

	public void stopTrackingGPS() {
		FLog.d("gps tracking is stopped");
		
		if (trackingHandler != null) {
			trackingHandler.removeCallbacks(trackingTask);
			trackingHandler = null;
		}
		
		gpsDataManager.setTrackingStarted(false);
		activityRef.get().invalidateOptionsMenu();
	}
	
	public void stopTrackingGPSForOnPause() {
		if (trackingHandler != null) {
			trackingHandler.removeCallbacks(trackingTask);
			trackingHandler = null;
		}
	}

	public void bindViewToEvent(String ref, String type, final String code) {
		try {

			if ("click".equals(type.toLowerCase(Locale.ENGLISH))) {
				View view = uiRenderer.getViewByRef(ref);
				if (view == null) {
					FLog.w("cannot find view " + ref);
					showWarning("Logic Error", "Error cannot find view " + ref);
					return;
				} else {
					if (view instanceof CustomListView) {
						final CustomListView listView = (CustomListView) view;
						listView.setOnItemClickListener(new ListView.OnItemClickListener() {

							@Override
							public void onItemClick(AdapterView<?> arg0,
									View arg1, int index, long arg3) {
								try {
									NameValuePair pair = (NameValuePair) listView
											.getItemAtPosition(index);
									interpreter.set("_list_item_value",
											pair.getValue());
									execute(code);
								} catch (Exception e) {
									FLog.e("error setting list item value", e);
								}
							}

						});
					} else if (view instanceof CustomSpinner) {
						final CustomSpinner spinner = (CustomSpinner) view;
						addSpinnerEventClickListener(spinner, code);
					} else if (view instanceof PictureGallery) {
						PictureGallery pictureGalleryView = (PictureGallery) view;
						addPictureGalleryEventClickListener(pictureGalleryView, code);
					} else if (view instanceof CustomRadioGroup) {
						final CustomRadioGroup radioGroup = (CustomRadioGroup) view;
						addRadioGroupEventClickListener(radioGroup, code);
					} else {
						view.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								execute(code);
							}
						});
					}
				}
			} else if ("select".equals(type.toLowerCase(Locale.ENGLISH))) {
				View view = uiRenderer.getViewByRef(ref);
				if (view == null) {
					FLog.w("cannot find view " + ref);
					showWarning("Logic Error", "Error cannot find view " + ref);
					return;
				} else {
					if (view instanceof CustomSpinner) {
						CustomSpinner spinner = (CustomSpinner) view;
						addSpinnerEventClickListener(spinner, code);
					} else if (view instanceof PictureGallery) {
						PictureGallery pictureGalleryView = (PictureGallery) view;
						addPictureGalleryEventClickListener(pictureGalleryView, code);
					} else if (view instanceof CustomRadioGroup) {
						final CustomRadioGroup radioGroup = (CustomRadioGroup) view;
						addRadioGroupEventClickListener(radioGroup, code);
					}
				}
			} else if ("delayclick".equals(type.toLowerCase(Locale.ENGLISH))) {
					View view = uiRenderer.getViewByRef(ref);
					if (view == null) {
						FLog.w("cannot find view " + ref);
						showWarning("Logic Error", "Error cannot find view " + ref);
						return;
					} else {
						if (view instanceof CustomButton) {
							view.setOnClickListener(new OnClickListener() {
	
								@Override
								public void onClick(View v) {
									CustomButton button = (CustomButton) v;
									if (button.canClick()) {
										execute(code);
										button.clicked();
									}
								}
							});
						}
					}
			} else if ("load".equals(type.toLowerCase(Locale.ENGLISH))) {
				TabGroup tg = uiRenderer.getTabGroupByLabel(ref);
				if (tg == null) {
					Tab tb = uiRenderer.getTabByLabel(ref);
					if (tb == null) {
						FLog.w("cannot find view " + ref);
						showWarning("Logic Error", "Error cannot find view " + ref);
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
						FLog.w("cannot find view " + ref);
						showWarning("Logic Error", "Error cannot find view " + ref);
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
	
	private void addSpinnerEventClickListener(final CustomSpinner spinner, final String code) {
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(
					AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				execute(code);
			}

			@Override
			public void onNothingSelected(
					AdapterView<?> arg0) {
				execute(code);
			}

		});
	}
	
	private void addPictureGalleryEventClickListener(final PictureGallery pictureGallery, final String code) {
		pictureGallery.setImageListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				execute(code);
			}

		});
	}
	
	private void addRadioGroupEventClickListener(final CustomRadioGroup radioGroup, final String code) {
		radioGroup.setOnCheckChangedListener(new RadioGroup.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				execute(code);
			}
		});
	}

	public void bindFocusAndBlurEvent(String ref, final String focusCallback,
			final String blurCallBack) {
		try {
			View view = uiRenderer.getViewByRef(ref);
			if (view == null) {
				FLog.w("cannot find view " + ref);
				showWarning("Logic Error", "Error cannot find view " + ref);
				return;
			} else {
				view.setOnFocusChangeListener(new OnFocusChangeListener() {

					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if (hasFocus) {
							if (focusCallback != null
									&& !focusCallback.isEmpty()) {
								execute(focusCallback);
							}
						} else {
							if (blurCallBack != null && !blurCallBack.isEmpty()) {
								execute(blurCallBack);
							}
						}
					}
				});
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
				mapView.setMapListener(new CustomMapView.CustomMapListener() {

					@Override
					public void onMapClicked(double x, double y, boolean arg2) {
						try {
							MapPos p = databaseManager.spatialRecord().convertFromProjToProj(GeometryUtil.EPSG3857, module.getSrid(), new MapPos(x, y));
							interpreter.set("_map_point_clicked", p);
							execute(clickCallback);
						} catch (Exception e) {
							FLog.e("error setting map point clicked", e);
						}
					}

					@Override
					public void onVectorElementClicked(VectorElement element,
							double arg1, double arg2, boolean arg3) {
						try {
							int geomId = mapView
									.getGeometryId((Geometry) element);
							interpreter.set("_map_geometry_selected", geomId);
							execute(selectCallback);
						} catch (Exception e) {
							FLog.e("error setting map geometry selected", e);
						}
					}

				});
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
	
	private TabGroup getTabGroup(String ref) throws Exception {
		TabGroup tabGroup = uiRenderer.showTabGroup(ref);
		if (tabGroup == null) {
			throw new Exception("Cannot find tabgroup " + ref);
		}
		return tabGroup;
	}
	
	private Tab getTab(String ref) throws Exception {
		Tab tab = uiRenderer.showTab(ref);
		if (tab == null) {
			throw new Exception("Cannot find tab " + ref);
		}
		return tab;
	}
	
	private TabGroup getTabGroupFromTabLabel(String ref) throws Exception {
		if (ref == null) {
			throw new Exception("Cannot find tabgroup " + ref);
		}
		String[] ids = ref.split("/");
		if (ids.length < 2) {
			throw new Exception("Cannot find tabgroup " + ref);
		}
		String groupId = ids[0];
		TabGroup tabGroup = uiRenderer.getTabGroupByLabel(groupId);
		if (tabGroup == null) {
			throw new Exception("Cannot find tabgroup " + ref);
		}
		return tabGroup;
	}

	public void newTabGroup(String label) {
		try {
			TabGroup tabGroup = showTabGroup(label);
			tabGroup.clearTabs();
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
			
			TabGroup tabGroup = getTabGroup(label);
			activityRef.get().getActionBar().setTitle(tabGroup.getLabel());
			return tabGroup;
		} catch (Exception e) {
			FLog.e("error showing tabgroup " + label, e);
			showWarning("Logic Error", "Error showing tab group " + label);
		}
		return null;
	}
	
	public TabGroup showTabGroup(final String label, final String uuid) {
		try {
			autoSaveManager.flush();
			
			final TabGroup tabGroup = getTabGroup(label);
			activityRef.get().getActionBar().setTitle(tabGroup.getLabel());
			tabGroup.setOnShowTask(new TabGroup.TabTask() {
				public void onShow() {
					try {
						autoSaveManager.pause();
						if (tabGroup.getArchEntType() != null) {
							TabGroupHelper.showArchEntityTabGroup(BeanShellLinker.this, uuid, tabGroup);
						} else if (tabGroup.getRelType() != null) {
							TabGroupHelper.showRelationshipTabGroup(BeanShellLinker.this, uuid, tabGroup);
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
			Tab tab = getTab(label);
			return tab;
		} catch (Exception e) {
			FLog.e("error showing tab " + label, e);
			showWarning("Logic Error", "Error showing tab " + label);
		}
		return null;
	}

	public Tab showTab(String label, String uuid) {
		try {
			TabGroup tabGroup = getTabGroupFromTabLabel(label);
			Tab tab = getTab(label);
			if (tabGroup.getArchEntType() != null) {
				TabGroupHelper.showArchEntityTab(this, uuid, tab);
			} else if (tabGroup.getRelType() != null) {
				TabGroupHelper.showRelationshipTab(this, uuid, tab);
			}
			return tab;
		} catch (Exception e) {
			FLog.e("error showing tab " + label, e);
			showWarning("Logic Error", "Error showing tab " + label);
		}
		return null;
	}
	
	public void saveTabGroup(final String ref, final String uuid, final List<Geometry> geometry, final List<? extends Attribute> attributes, final String callback) {
		saveTabGroupInBackground(ref, uuid, geometry, attributes, callback, uuid == null, false);
	}

	public void saveTab(final String ref, final String uuid, final List<Geometry> geometry, final List<? extends Attribute> attributes, final String callback) {
		saveTabInBackground(ref, uuid, geometry, attributes, callback, uuid == null, false);
	}
	
	public String enableAutoSaveOnTabGroup(String ref, String uuid) {
		boolean newRecord = uuid == null;
		if (newRecord) {
			uuid = databaseManager.sharedRecord().generateUUID();
		}
		autoSaveManager.enable(ref, uuid, newRecord);
		return uuid;
	}
	
	public void disableAutoSaveOnTabGroup(String ref) {
		autoSaveManager.flush();
	}
	
	public void saveTabGroupInBackground(final String ref, final String uuid, final List<Geometry> geometry, final List<? extends Attribute> attributes, 
			final String callback, final boolean newRecord, boolean blocking) {
		try {
			final TabGroup tabGroup = uiRenderer.getTabGroup(ref);
			if (tabGroup == null) {
				throw new Exception("Cannot find tabgroup " + ref);
			}
			if (blocking) {
				try {
					TabGroupHelper.saveTabGroup(BeanShellLinker.this, tabGroup, uuid, geometry, attributes, newRecord);
					if (callback != null) {
						execute(callback);
					}
				} catch (Exception e) {
					FLog.e("error saving tabgroup " + ref, e);
				}
			} else {
				AsyncTask<Void, Void, Void> autoSaveTask = new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						try {
							TabGroupHelper.saveTabGroup(BeanShellLinker.this, tabGroup, uuid, geometry, attributes, newRecord);
						} catch (Exception e) {
							FLog.e("error saving tabgroup " + ref, e);
						}
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						if (callback != null) {
							BeanShellLinker.this.execute(callback);
						}
					}
					
				};
				autoSaveTask.execute();
			}	
		} catch (Exception e) {
			FLog.e("error saving tabgroup " + ref, e);
		}
	}
	
	public void saveTabInBackground(final String ref, final String uuid, final List<Geometry> geometry, final List<? extends Attribute> attributes, 
			final String callback, final boolean newRecord, boolean blocking) {
		try {
			final TabGroup tabGroup = getTabGroupFromTabLabel(ref);
			final Tab tab = uiRenderer.getTabByLabel(ref);
			if (tab == null) {
				throw new Exception("cannot find tab " + ref);
			}
			if (blocking) {
				try {
					TabGroupHelper.saveTab(BeanShellLinker.this, tabGroup, tab, uuid, geometry, attributes, newRecord);
					if (callback != null) {
						execute(callback);
					}
				} catch (Exception e) {
					FLog.e("error saving tab " + ref, e);
				}
			} else {
				AsyncTask<Void, Void, Void> autoSaveTask = new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						try {
							TabGroupHelper.saveTab(BeanShellLinker.this, tabGroup, tab, uuid, geometry, attributes, newRecord);
						} catch (Exception e) {
							FLog.e("error saving tab " + ref, e);
						}
						return null;
					}
					
					@Override
					protected void onPostExecute(Void result) {
						if (callback != null) {
							BeanShellLinker.this.execute(callback);
						}
					}
					
				};
				autoSaveTask.execute();
			}	
		} catch (Exception e) {
			FLog.e("error saving tab " + ref, e);
		}
	}

	public void cancelTabGroup(String label, boolean warn) {
		try {
			TabGroup tabGroup = getTabGroup(label);
			if (warn) {
				boolean hasChanges = false;
				if (tabGroup.getArchEntType() != null
						|| tabGroup.getRelType() != null) {
					for (Tab tab : tabGroup.getTabs()) {
						if (hasChanges(tab)) {
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
			final TabGroup tabGroup = getTabGroupFromTabLabel(label);
			final Tab tab = getTab(label);
			if (warn) {
				if (hasChanges(tab)
						&& (tabGroup.getArchEntType() != null || tabGroup
								.getRelType() != null)) {
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

	private boolean hasChanges(Tab tab) {
		List<View> views = tab.getAttributeViews();
		for (View v : views) {
			
			if (v instanceof ICustomView) {
				ICustomView customView = (ICustomView) v;
				if (customView.hasChanges()) {
					return true;
				}
			}
			
		}
		return false;
	}

	public void goBack() {
		this.activityRef.get().onBackPressed();
	}
	
	public void showToast(String message) {
		try {
			int duration = Toast.LENGTH_SHORT;
			if (toast != null) {
				toast.cancel();
			}
			toast = Toast.makeText(activityRef.get().getBaseContext(),
					message, duration);
			toast.show();
		} catch (Exception e) {
			FLog.e("error showing toast", e);
			showWarning("Logic Error", "Error showing toast");
		}
	}

	public void showAlert(final String title, final String message,
			final String okCallback, final String cancelCallback) {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(this.activityRef.get());
	
			builder.setTitle(title);
			builder.setMessage(message);
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
	
			builder.create().show();
		} catch (Exception e) {
			FLog.e("error showing alert", e);
		}
	}

	public void showWarning(final String title, final String message) {
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(this.activityRef.get());
	
			builder.setTitle(title);
			builder.setMessage(message);
			builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// User clicked OK button
				}
			});
			builder.create().show();
		} catch (Exception e) {
			FLog.e("error showing warning", e);
		}
	}
	
	public Dialog showBusy(final String title, final String message) {
		try {
			BusyDialog d = new BusyDialog(this.activityRef.get(), title, message, null);
			d.show();
			return d;
		} catch (Exception e) {
			FLog.e("error showing busy", e);
		}
		return null;
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
				
				Button dirtyButton = uiRenderer.getTabForView(ref).getDirtyButton(ref);
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
	
	public void appendFieldDirty(String ref, boolean isDirty, String dirtyReason) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof ICustomView) {
				ICustomView customView = (ICustomView) obj;
				
				boolean isViewDirty = isDirty || customView.isDirty();
				
				Button dirtyButton = uiRenderer.getTabForView(ref).getDirtyButton(ref);
				if (dirtyButton != null) {
					dirtyButton.setVisibility(isViewDirty ? View.VISIBLE : View.GONE);
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

	public String getCurrentTime() {
		return DateUtil.getCurrentTimestampGMT();
	}
	
	public String saveArchEnt(String entityId, String entityType,
			List<Geometry> geometry, List<EntityAttribute> attributes, boolean newEntity) {
		try {
			List<Geometry> geomList = databaseManager.spatialRecord().convertGeometryFromProjToProj(this.module.getSrid(), GeometryUtil.EPSG4326, geometry);
			return databaseManager.entityRecord().saveArchEnt(entityId,
					entityType, WKTUtil.collectionToWKT(geomList), attributes, newEntity);
		} catch (Exception e) {
			FLog.e("error saving arch entity", e);
			showWarning("Logic Error", "Error saving arch entity");
		}
		return null;
	}

	public String saveArchEnt(String entityId, String entityType,
			List<Geometry> geometry, List<EntityAttribute> attributes) {
		return saveArchEnt(entityId, entityType, geometry, attributes, entityId == null);
	}

	public Boolean deleteArchEnt(String entityId){
		try {
			databaseManager.entityRecord().deleteArchEnt(entityId);
			for(Tab tab : uiRenderer.getTabList()){
				for(CustomMapView mapView : tab.getMapViewList()){
					mapView.removeFromAllSelections(entityId);
					mapView.updateSelections();
				}
			}
			return true;
		} catch (jsqlite.Exception e) {
			FLog.e("can not delete arch entity with the supplied id", e);
		}
		return false;
	}

	public String saveRel(String relationshipId, String relationshipType,
			List<Geometry> geometry, List<RelationshipAttribute> attributes, boolean newRelationship) {
		try {
			List<Geometry> geomList = databaseManager.spatialRecord().convertGeometryFromProjToProj(this.module.getSrid(), GeometryUtil.EPSG4326, geometry);
			return databaseManager.relationshipRecord().saveRel(relationshipId, relationshipType,
					WKTUtil.collectionToWKT(geomList), attributes, newRelationship);
		} catch (Exception e) {
			FLog.e("error saving relationship", e);
			showWarning("Logic Error", "Error saving relationship");
		}
		return null;
	}
	
	public String saveRel(String relationshipId, String relationshipType,
			List<Geometry> geometry, List<RelationshipAttribute> attributes) {
		return saveRel(relationshipId, relationshipType, geometry, attributes, relationshipId == null);
	}

	public Boolean deleteRel(String relationshpId){
		try {
			databaseManager.relationshipRecord().deleteRel(relationshpId);
			for(Tab tab : uiRenderer.getTabList()){
				for(CustomMapView mapView : tab.getMapViewList()){
					mapView.removeFromAllSelections(relationshpId);
					mapView.updateSelections();
				}
			}
			return true;
		} catch (jsqlite.Exception e) {
			FLog.e("can not delete relationship with the supplied id", e);
		}
		return false;
	}
	
	public boolean addReln(String entityId, String relationshpId, String verb) {
		try {
			return databaseManager.sharedRecord().addReln(entityId, relationshpId, verb);
		} catch (Exception e) {
			FLog.e("error saving arch entity relationship", e);
			showWarning("Logic Error", "Error saving arch entity relationship");
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	public void populateDropDown(String ref, Collection valuesObj) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof CustomSpinner && valuesObj instanceof Collection<?>) {
				CustomSpinner spinner = (CustomSpinner) obj;

				List<NameValuePair> pairs = convertToNameValuePairs((Collection<?>) valuesObj);

				ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
						this.activityRef.get(),
						android.R.layout.simple_spinner_dropdown_item, pairs);
				spinner.setAdapter(arrayAdapter);
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
	
	public void populateHierarchicalDropDown(String ref, String attributeName) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);

			if (obj instanceof HierarchicalSpinner) {
				List<VocabularyTerm> terms = databaseManager.attributeRecord().getVocabularyTerms(attributeName);
				if (terms == null) return;
				
				VocabularyTerm.applyArch16n(terms, arch16n);
				
				HierarchicalSpinner spinner = (HierarchicalSpinner) obj;
				spinner.setTerms(terms);
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
							Picture picture = new Picture(null, null, value);
							pictures.add(picture);
						}
					}
				}
				
				PictureGallery gallery = (PictureGallery) obj;
				gallery.populate(pictures);
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
	
	public void populateHierarchicalPictureGallery(String ref, String attributeName) {
		try {
			Object obj = uiRenderer.getViewByRef(ref);
			
			if (obj instanceof PictureGallery) {				
				List<VocabularyTerm> terms = databaseManager.attributeRecord().getVocabularyTerms(attributeName);
				if (terms == null) return;
				
				VocabularyTerm.applyArch16n(terms, arch16n);
				VocabularyTerm.applyProjectDir(terms, module.getDirectoryPath().getPath() + "/");
				
				HierarchicalPictureGallery gallery = (HierarchicalPictureGallery) obj;
				gallery.setTerms(terms);
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
				gallery.populate(pictures);
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
				gallery.populate(pictures);
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
	
	private ArrayList<NameValuePair> convertToNameValuePairs(Collection<?> valuesObj) throws Exception {
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

	public Object fetchArchEnt(String id) {
		try {
			ArchEntity e = databaseManager.entityRecord().fetchArchEnt(id);
			if (e != null) {
				List<Geometry> geomList = e.getGeometryList();
				if (geomList != null) {
					e.setGeometryList(databaseManager.spatialRecord().convertGeometryFromProjToProj(GeometryUtil.EPSG4326, module.getSrid(), geomList));
				}
			}
			return e;
		} catch (Exception e) {
			FLog.e("error fetching arch entity", e);
			showWarning("Logic Error", "Error fetching arch entity");
		}
		return null;
	}

	public Object fetchRel(String id) {
		try {
			Relationship r = databaseManager.relationshipRecord().fetchRel(id);
			if (r != null) {
				List<Geometry> geomList = r.getGeometryList();
				if (geomList != null) {
					r.setGeometryList(databaseManager.spatialRecord().convertGeometryFromProjToProj(GeometryUtil.EPSG4326, module.getSrid(), geomList));
				}
			}
			return r;
		} catch (Exception e) {
			FLog.e("error fetching relationship", e);
			showWarning("Logic Error", "Error fetching relationship");
		}
		return null;
	}

	public Object fetchOne(String query) {
		try {
			return databaseManager.fetchRecord().fetchOne(query);
		} catch (Exception e) {
			FLog.e("error fetching one", e);
			showWarning("Logic Error", "Error fetching one");
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public Collection fetchAll(String query) {
		try {
			return databaseManager.fetchRecord().fetchAll(query);
		} catch (Exception e) {
			FLog.e("error fetching all", e);
			showWarning("Logic Error", "Error fetching all");
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public Collection fetchEntityList(String type) {
		try {
			return databaseManager.fetchRecord().fetchEntityList(type);
		} catch (Exception e) {
			FLog.e("error fetching entity list", e);
			showWarning("Logic Error", "Error fetching entity list");
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public Collection fetchRelationshipList(String type) {
		try {
			return databaseManager.fetchRecord().fetchRelationshipList(type);
		} catch (Exception e) {
			FLog.e("error fetching relationship list", e);
			showWarning("Logic Error", "Error fetching relationship list");
		}
		return null;
	}
	
	public int getGpsUpdateInterval() {
		return gpsDataManager.getGpsUpdateInterval();
	}

	public void setGpsUpdateInterval(int gpsUpdateInterval) {
		gpsDataManager.setGpsUpdateInterval(
				gpsUpdateInterval);
	}
	
	public void startExternalGPS() {
		bluetoothManager.resetConnection(); // make sure we get users to reconnect to the correct device
		gpsDataManager.startExternalGPSListener();	
	}

	public void startInternalGPS() {
		gpsDataManager.startInternalGPSListener();
	}
	
	public void stopGPS() {
		gpsDataManager.destroyListener();
	}

	public Object getGPSPosition() {
		return gpsDataManager.getGPSPosition();
	}
	
	public Object getGPSPositionProjected() {
		GPSLocation l = (GPSLocation) gpsDataManager.getGPSPosition();
		if (l == null) return l;
		MapPos p = databaseManager.spatialRecord().convertFromProjToProj(GeometryUtil.EPSG4326, module.getSrid(), new MapPos(l.getLongitude(), l.getLatitude()));
		l.setLongitude(p.x);
		l.setLatitude(p.y);
		return l;
	}

	public Object getGPSEstimatedAccuracy() {
		return gpsDataManager.getGPSEstimatedAccuracy();
	}

	public Object getGPSHeading() {
		return gpsDataManager.getGPSHeading();
	}

	public Object getGPSPosition(String gps) {
		return gpsDataManager.getGPSPosition(gps);
	}

	public Object getGPSEstimatedAccuracy(String gps) {
		return gpsDataManager.getGPSEstimatedAccuracy(gps);
	}

	public Object getGPSHeading(String gps) {
		return gpsDataManager.getGPSHeading(gps);
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
						textStyle.toStyleSet());
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
						textStyle.toStyleSet());
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
		} catch (EvalError e) {
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
		} catch (EvalError e) {
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
		} catch (EvalError e) {
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
	
	public Module getModule() {
		return this.module;
	}

	public String getModuleName() {
		return this.module.getName();
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

	public String attachFile(String filePath, boolean sync, String dir, final String callback) {
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
			String name= file.getName();
			
			// create random file path
			attachFile += "/" + UUID.randomUUID() + "_" + name;

			activityRef.get().copyFile(filePath, module.getDirectoryPath(attachFile).getPath(), new ShowModuleActivity.AttachFileListener() {

						@Override
						public void handleComplete() {
							if (callback != null) {
								execute(callback);
							}
						}
				
			});
			if(!activityRef.get().getSyncStatus().equals(SyncStatus.INACTIVE)){
				activityRef.get().setSyncStatus(ShowModuleActivity.SyncStatus.ACTIVE_HAS_CHANGES);
			}
			return attachFile;
		} catch (Exception e) {
			FLog.e("error attaching file " + filePath, e);
			return null;
		}
	}

	public void viewArchEntAttachedFiles(String uuid) {
		if (uuid == null) {
			showWarning("Attached Files",
					"Please load/save a record to see attached files");
		} else {
			ArchEntity fetchedArchEntity = (ArchEntity) fetchArchEnt(uuid);
			List<String> attachedFiles = new ArrayList<String>();
			for (EntityAttribute attribute : fetchedArchEntity.getAttributes()) {
				if ("file".equalsIgnoreCase(attribute.getType())) {
					if (!attribute.isDeleted()) {
						attachedFiles.add(attribute.getText());
					}
				}
			}
			viewAttachedFiles(attachedFiles);
		}
	}

	public void viewRelAttachedFiles(String relId) {
		if (relId == null) {
			showWarning("Attached Files",
					"Please load/save a record to see attached files");
		} else {
			Relationship fetchedRelationship = (Relationship) fetchRel(relId);
			List<String> attachedFiles = new ArrayList<String>();
			for (RelationshipAttribute attribute : fetchedRelationship
					.getAttributes()) {
				if ("file".equalsIgnoreCase(attribute.getType())) {
					if (!attribute.isDeleted()) {
						attachedFiles.add(attribute.getText());
					}
				}
			}
			viewAttachedFiles(attachedFiles);
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
						if (file.getPath().contains("files/server")) {
							showWarning(
								"Attached File",
								"Cannot open the selected file. The selected file only syncs to the server.");
						} else {
							showWarning(
								"Attached File",
								"Cannot open the selected file. Please wait for the file to finish syncing to the app.");
						}
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
					mapView.setCreateCallback(new CustomMapView.CreateCallback() {
						
						@Override
						public void onCreate(int geomId) {
							try {
								interpreter.set("_map_geometry_created", geomId);
								execute(callback);
							} catch (Exception e) {
								FLog.e("error setting geometry created", e);
							}
						}
					});
				} else if ("load".equals(type)) {
					mapView.setLoadToolVisible(true);
					mapView.setLoadCallback(new CustomMapView.LoadCallback() {
						
						@Override
						public void onLoad(String id, boolean isEntity) {
							try {
								interpreter.set("_map_geometry_loaded", id);
								interpreter.set("_map_geometry_loaded_type", isEntity ? "entity" : "relationship");
								execute(callback);
							} catch (Exception e) {
								FLog.e("error setting geometry loaded", e);
							}
						}
					});	
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
				gallery.addPicture(file);
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
				gallery.addVideo(file);
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
	public void saveTo(Bundle savedInstanceState) {
		String persistedObjectName = getPersistedObjectName();
		if (persistedObjectName != null) {
			try {
				Object persistedObject = interpreter.get(persistedObjectName);
				savedInstanceState.putSerializable(persistedObjectName,
						(Serializable) persistedObject);
			} catch (EvalError e) {
				FLog.e("error storing bean shell data", e);
			}
		}
	}

	@Override
	public void restoreFrom(Bundle savedInstanceState) {
		if (persistedObjectName != null) {
			Object object = savedInstanceState
					.getSerializable(persistedObjectName);
			try {
				interpreter.set(persistedObjectName, object);
			} catch (EvalError e) {
				FLog.e("error restoring bean shell data", e);
			}
		}
	}
	
	@Override
	public void pause() {
		stopTrackingGPSForOnPause();
	}
	
	@Override
	public void resume() {
		if (gpsDataManager.isTrackingStarted()) {
			startTrackingGPS(gpsDataManager.getTrackingType(),
					gpsDataManager.getTrackingValue(),
					gpsDataManager.getTrackingExec());
		}
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
		try {
			this.interpreter.eval(hardwareReadingCallBack);
		} catch (EvalError e) {
			FLog.e("error when executing the callback for hardware capture", e);
		}
	}
	
	public void clearHardwareDeviceBuffer() {
		this.activityRef.get().clearDeviceBuffer();
	}
	
}