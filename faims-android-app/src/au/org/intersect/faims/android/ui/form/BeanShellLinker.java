package au.org.intersect.faims.android.ui.form;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.VideoView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.data.User;
import au.org.intersect.faims.android.exceptions.MapException;
import au.org.intersect.faims.android.gps.GPSLocation;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.FileManager;
import au.org.intersect.faims.android.nutiteq.GeometryData;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.nutiteq.GeometryTextStyle;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.nutiteq.WKTUtil;
import au.org.intersect.faims.android.ui.activity.ShowProjectActivity;
import au.org.intersect.faims.android.ui.dialog.BusyDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.LegacyQueryBuilder;
import au.org.intersect.faims.android.ui.map.QueryBuilder;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FileUtil;
import bsh.EvalError;
import bsh.Interpreter;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;

public class BeanShellLinker {

	private class AudioListAdapter extends BaseAdapter {

		private List<NameValuePair> audioLists;
		private ArrayList<View> audioViews;

		public AudioListAdapter(Map<NameValuePair, Boolean> audioList) {
			this.audioLists = new ArrayList<NameValuePair>();
			this.audioLists.addAll(audioList.keySet());
			this.audioViews = new ArrayList<View>();

			for (Entry<NameValuePair, Boolean> audio : audioList.entrySet()) {
				AudioListItem item = new AudioListItem(activity);
				item.init(audio.getKey(), audio.getValue());
				audioViews.add(item);
			}
		}

		@Override
		public int getCount() {
			return audioLists.size();
		}

		@Override
		public Object getItem(int position) {
			return audioLists.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View arg1, ViewGroup arg2) {
			return audioViews.get(position);
		}

	}

	private Interpreter interpreter;

	private ShowProjectActivity activity;

	private static final String FREETEXT = "freetext";
	private static final String MEASURE = "measure";
	private static final String VOCAB = "vocab";

	private String persistedObjectName;

	private String lastFileBrowserCallback;

	private HandlerThread trackingHandlerThread;
	private Handler trackingHandler;
	private Runnable trackingTask;
	private Double prevLong;
	private Double prevLat;

	private String cameraPicturepath;
	private String cameraCallBack;

	private String videoCallBack;
	private String cameraVideoPath;

	private Project project;

	private String audioFileNamePath;

	private MediaRecorder recorder;

	private String audioCallBack;

	private MediaPlayer mediaPlayer;

	public BeanShellLinker(ShowProjectActivity activity, Project project) {
		this.activity = activity;
		this.project = project;
		this.interpreter = new Interpreter();
		try {
			interpreter.set("linker", this);
		} catch (EvalError e) {
			FLog.e("error setting linker", e);
		}
		this.activity.getFileManager().addListener(
				ShowProjectActivity.FILE_BROWSER_REQUEST_CODE,
				new FileManager.FileManagerListener() {

					@Override
					public void onFileSelected(File file) {
						BeanShellLinker.this.setLastSelectedFile(file);
					}

				});
	}

	public void sourceFromAssets(String filename) {
		try {
			interpreter.eval(FileUtil.convertStreamToString(this.activity
					.getAssets().open(filename)));
		} catch (Exception e) {
			FLog.w("error sourcing script from assets", e);
			showWarning("Logic Error", "Error encountered in logic commands");
		}
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
		try {
			interpreter.eval(code);
		} catch (EvalError e) {
			FLog.i("error executing code", e);
			showWarning("Logic Error", "Error encountered in logic script");
		}
	}

	public void startTrackingGPS(final String type, final int value, final String callback) {
		FLog.d("gps tracking is started");
		
		this.activity.getGPSDataManager().setTrackingType(type);
		this.activity.getGPSDataManager().setTrackingValue(value);
		this.activity.getGPSDataManager().setTrackingExec(callback);

		if (trackingHandlerThread == null && trackingHandler == null) {
			if (!this.activity.getGPSDataManager().isExternalGPSStarted()
					&& !this.activity.getGPSDataManager()
							.isInternalGPSStarted()) {
				showWarning("GPS", "No GPS is being used");
				return;
			}
			trackingHandlerThread = new HandlerThread("tracking");
			trackingHandlerThread.start();
			trackingHandler = new Handler(trackingHandlerThread.getLooper());
			if ("time".equals(type)) {
				trackingTask = new Runnable() {

					@Override
					public void run() {
						trackingHandler.postDelayed(this, value * 1000);
						if (getGPSPosition() != null) {
							execute(callback);
						} else {
							showToast("no gps signal at the moment");
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
							showToast("no gps signal at the moment");
						}
					}
				};
				trackingHandler.postDelayed(trackingTask, 1000);
			} else {
				FLog.e("wrong type format is used");
			}
		} else {
			showToast("gps tracking has been started, please stop it before starting");
		}
	}

	public void stopTrackingGPS() {
		FLog.d("gps tracking is stopped");
		
		if (trackingHandler != null) {
			trackingHandler.removeCallbacks(trackingTask);
			trackingHandler = null;
		}
		if (trackingHandlerThread != null) {
			trackingHandlerThread.quit();
			trackingHandlerThread = null;
		}
	}

	public void bindViewToEvent(String ref, String type, final String code) {
		try {

			if ("click".equals(type.toLowerCase(Locale.ENGLISH))) {
				View view = activity.getUIRenderer().getViewByRef(ref);
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
					} else {
						if (view instanceof Spinner) {
							((Spinner) view)
									.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

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
						} else {
							view.setOnClickListener(new OnClickListener() {

								@Override
								public void onClick(View v) {
									execute(code);
								}
							});
						}
					}
				}
			} else if ("delayclick".equals(type.toLowerCase(Locale.ENGLISH))) {
					View view = activity.getUIRenderer().getViewByRef(ref);
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
				TabGroup tg = activity.getUIRenderer().getTabGroupByLabel(ref);
				if (tg == null) {
					FLog.w("cannot find tabgroup " + ref);
					showWarning("Logic Error", "Error cannot find tabgroup "
							+ tg);
					return;
				} else {
					tg.addOnLoadCommand(code);
				}
			} else if ("show".equals(type.toLowerCase(Locale.ENGLISH))) {
				TabGroup tg = activity.getUIRenderer().getTabGroupByLabel(ref);
				if (tg == null) {
					FLog.w("cannot find tabgroup " + ref);
					showWarning("Logic Error", "Error cannot find tabgroup "
							+ tg);
					return;
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
			final String blurCallBack) {
		try {
			View view = activity.getUIRenderer().getViewByRef(ref);
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
			View view = activity.getUIRenderer().getViewByRef(ref);
			if (view instanceof CustomMapView) {
				final CustomMapView mapView = (CustomMapView) view;
				mapView.setMapListener(new CustomMapView.CustomMapListener() {

					@Override
					public void onMapClicked(double x, double y, boolean arg2) {
						try {
							MapPos p = GeometryUtil.convertFromProjToProj(GeometryUtil.EPSG3785, project.getSrid(), new MapPos(x, y));
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

	public void newTabGroup(String label) {
		try {
			TabGroup tabGroup = showTabGroup(label);
			if (tabGroup == null) {
				showWarning("Logic Error", "Error showing new tab group "
						+ label);
				return;
			}

			tabGroup.clearTabs();
		} catch (Exception e) {
			FLog.e("error showing new tabgroup " + label, e);
			showWarning("Logic Error", "Error showing new tab group " + label);
		}
	}

	public void newTab(String label) {
		try {
			Tab tab = showTab(label);
			if (tab == null) {
				showWarning("Logic Error", "Error showing new tab group "
						+ label);
				return;
			}

			tab.clearViews();
		} catch (Exception e) {
			FLog.e("error showing new tab " + label, e);
			showWarning("Logic Error", "Error showing new tab " + label);
		}
	}

	public TabGroup showTabGroup(String label) {
		try {
			TabGroup tabGroup = activity.getUIRenderer().showTabGroup(
					this.activity, label);
			if (tabGroup == null) {
				showWarning("Logic Error", "Error showing tabgroup " + label);
				return null;
			}
			return tabGroup;
		} catch (Exception e) {
			FLog.e("error showing tabgroup " + label, e);
			showWarning("Logic Error", "Error showing tab group " + label);
		}
		return null;
	}

	public Tab showTab(String label) {
		try {
			Tab tab = activity.getUIRenderer().showTab(label);
			if (tab == null) {
				showWarning("Logic Error", "Error showing tab " + label);
				return null;
			}
			return tab;
		} catch (Exception e) {
			FLog.e("error showing tab " + label, e);
			showWarning("Logic Error", "Error showing tab " + label);
		}
		return null;
	}

	public void showTabGroup(String id, String uuid) {
		try {
			TabGroup tabGroup = activity.getUIRenderer().showTabGroup(activity,
					id);
			if (tabGroup == null) {
				showWarning("Logic Error", "Error showing tab group " + id);
				return;
			}
			if (tabGroup.getArchEntType() != null) {
				showArchEntityTabGroup(uuid, tabGroup);
			} else if (tabGroup.getRelType() != null) {
				showRelationshipTabGroup(uuid, tabGroup);
			} else {
				showTabGroup(id);
			}
		} catch (Exception e) {
			FLog.e("error showing tabgroup " + id);
			showWarning("Logic Error", "Error showing tab group " + id);
		}
	}

	public void showTab(String id, String uuid) {
		try {
			if (id == null) {
				showWarning("Logic Error", "Error showing tab " + id);
				return;
			}
			String[] ids = id.split("/");
			if (ids.length < 2) {
				showWarning("Logic Error", "Error showing tab " + id);
				return;
			}
			String groupId = ids[0];
			String tabId = ids[1];
			TabGroup tabGroup = activity.getUIRenderer().getTabGroupByLabel(
					groupId);
			if (tabGroup == null) {
				showWarning("Logic Error", "Error showing tab " + id);
				return;
			}
			Tab tab = tabGroup.showTab(tabId);
			if (tab == null) {
				showWarning("Logic Error", "Error showing tab " + id);
				return;
			}
			if (tabGroup.getArchEntType() != null) {
				showArchEntityTab(uuid, tab);
			} else if (tabGroup.getRelType() != null) {
				showRelationshipTab(uuid, tab);
			} else {
				showTab(id);
			}
		} catch (Exception e) {
			FLog.e("error showing tab " + id);
			showWarning("Logic Error", "Error showing tab " + id);
		}
	}

	public void cancelTabGroup(String id, boolean warn) {
		try {
			if (id == null) {
				showWarning("Logic Error", "Error cancelling tab group" + id);
				return;
			}
			final TabGroup tabGroup = activity.getUIRenderer()
					.getTabGroupByLabel(id);
			if (tabGroup == null) {
				showWarning("Logic Error", "Error cancelling tab group" + id);
				return;
			}
			if (warn) {
				boolean hasChanges = false;
				if (tabGroup.getArchEntType() != null
						|| tabGroup.getRelType() != null) {
					for (Tab tab : tabGroup.getTabs()) {
						if (hasChanges(tab)) {
							hasChanges = true;
						}
					}
				}
				if (hasChanges) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							this.activity);

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
			FLog.e("error cancelling tab group " + id);
			showWarning("Logic Error", "Error cancelling tab group " + id);
		}
	}

	public void cancelTab(String id, boolean warn) {
		try {
			if (id == null) {
				showWarning("Logic Error", "Error cancelling tab " + id);
				return;
			}
			String[] ids = id.split("/");
			if (ids.length < 2) {
				showWarning("Logic Error", "Error cancelling tab " + id);
				return;
			}
			String groupId = ids[0];
			final String tabId = ids[1];
			final TabGroup tabGroup = activity.getUIRenderer()
					.getTabGroupByLabel(groupId);
			if (tabGroup == null) {
				showWarning("Logic Error", "Error cancelling tab " + id);
				return;
			}
			Tab tab = tabGroup.getTab(tabId);
			if (warn) {
				if (hasChanges(tab)
						&& (tabGroup.getArchEntType() != null || tabGroup
								.getRelType() != null)) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							this.activity);

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
										tabGroup.hideTab(tabId);
									}
								}
							});
					builder.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// User cancelled the dialog
									tabGroup.showTab(tabId);
								}
							});

					builder.create().show();
				} else {
					if (tabGroup.getTabs().size() == 1) {
						goBack();
					} else {
						tabGroup.hideTab(tabId);
					}
				}
			} else {
				if (tabGroup.getTabs().size() == 1) {
					goBack();
				} else {
					tabGroup.hideTab(tabId);
				}
			}
		} catch (Exception e) {
			FLog.e("error cancelling tab " + id);
			showWarning("Logic Error", "Error cancelling tab " + id);
		}
	}

	private boolean hasChanges(Tab tab) {
		List<View> views = tab.getAllViews();
		for (View v : views) {
			if (v instanceof CustomEditText) {
				CustomEditText customEditText = (CustomEditText) v;
				if (!getFieldValue(customEditText.getRef()).equals(
						tab.getStoredValue(customEditText.getRef()))) {
					return true;
				}
				if (customEditText.getCertainty() != customEditText
						.getCurrentCertainty()) {
					return true;
				}
				if (!customEditText.getAnnotation().equals(
						customEditText.getCurrentAnnotation())) {
					return true;
				}

			} else if (v instanceof CustomDatePicker) {
				CustomDatePicker customDatePicker = (CustomDatePicker) v;
				if (!getFieldValue(customDatePicker.getRef()).equals(
						tab.getStoredValue(customDatePicker.getRef()))) {
					return true;
				}
				if (customDatePicker.getCertainty() != customDatePicker
						.getCurrentCertainty()) {
					return true;
				}

			} else if (v instanceof CustomTimePicker) {
				CustomTimePicker customTimePicker = (CustomTimePicker) v;
				if (!getFieldValue(customTimePicker.getRef()).equals(
						tab.getStoredValue(customTimePicker.getRef()))) {
					return true;
				}
				if (customTimePicker.getCertainty() != customTimePicker
						.getCurrentCertainty()) {
					return true;
				}

			} else if (v instanceof CustomLinearLayout) {
				CustomLinearLayout customLinearLayout = (CustomLinearLayout) v;
				if (!getFieldValue(customLinearLayout.getRef()).equals(
						tab.getStoredValue(customLinearLayout.getRef()))) {
					return true;
				}
				if (customLinearLayout.getCertainty() != customLinearLayout
						.getCurrentCertainty()) {
					return true;
				}
				if (!customLinearLayout.getAnnotation().equals(
						customLinearLayout.getCurrentAnnotation())) {
					return true;
				}

			} else if (v instanceof CustomSpinner) {
				CustomSpinner customSpinner = (CustomSpinner) v;
				if (!getFieldValue(customSpinner.getRef()).equals(
						tab.getStoredValue(customSpinner.getRef()))) {
					return true;
				}
				if (customSpinner.getCertainty() != customSpinner
						.getCurrentCertainty()) {
					return true;
				}
				if (!customSpinner.getAnnotation().equals(
						customSpinner.getCurrentAnnotation())) {
					return true;
				}

			} else if (v instanceof CustomHorizontalScrollView) {
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) v;
				if (!getFieldValue(horizontalScrollView.getRef()).equals(
						tab.getStoredValue(horizontalScrollView.getRef()))) {
					return true;
				}
				if (horizontalScrollView.getCertainty() != horizontalScrollView
						.getCurrentCertainty()) {
					return true;
				}
				if (!horizontalScrollView.getAnnotation().equals(
						horizontalScrollView.getCurrentAnnotation())) {
					return true;
				}
			}
		}
		return false;
	}

	public void goBack() {
		this.activity.onBackPressed();
	}

	public int getGpsUpdateInterval() {
		return this.activity.getGPSDataManager().getGpsUpdateInterval();
	}

	public void setGpsUpdateInterval(int gpsUpdateInterval) {
		this.activity.getGPSDataManager().setGpsUpdateInterval(
				gpsUpdateInterval);
	}

	private void showArchEntityTabGroup(String uuid, TabGroup tabGroup) {
		Object archEntityObj = fetchArchEnt(uuid);
		if (archEntityObj == null) {
			FLog.d("cannot find arch entity " + tabGroup.getLabel());
			showWarning("Logic Error",
					"Error showing tab group " + tabGroup.getLabel());
			return;
		}
		if (archEntityObj instanceof ArchEntity) {
			ArchEntity archEntity = (ArchEntity) archEntityObj;
			try {
				for (Tab tab : tabGroup.getTabs()) {
					reinitiateArchEntFieldsValue(tab, archEntity);
				}
			} catch (Exception e) {
				FLog.e("error showing arch entity tab group "
						+ tabGroup.getLabel(), e);
				showWarning("Logic Error", "Error showing tab group "
						+ tabGroup.getLabel());
			}
		}
	}

	private void showRelationshipTabGroup(String uuid, TabGroup tabGroup) {
		Object relationshipObj = fetchRel(uuid);
		if (relationshipObj == null) {
			FLog.d("cannot find relationship " + tabGroup.getLabel());
			showWarning("Logic Error",
					"Error showing tab group " + tabGroup.getLabel());
			return;
		}
		if (relationshipObj instanceof Relationship) {
			Relationship relationship = (Relationship) relationshipObj;
			try {
				for (Tab tab : tabGroup.getTabs()) {
					reinitiateRelationshipFieldsValue(tab, relationship);
				}
			} catch (Exception e) {
				FLog.e("error showing relationship tab group "
						+ tabGroup.getLabel(), e);
				showWarning("Logic Error", "Error showing tab group "
						+ tabGroup.getLabel());
			}
		}
	}

	private void showArchEntityTab(String uuid, Tab tab) {
		Object archEntityObj = fetchArchEnt(uuid);
		if (archEntityObj == null) {
			showWarning("Logic Error", "Error showing tab " + tab.getLabel());
			return;
		}
		if (archEntityObj instanceof ArchEntity) {
			ArchEntity archEntity = (ArchEntity) archEntityObj;
			try {
				reinitiateArchEntFieldsValue(tab, archEntity);
			} catch (Exception e) {
				FLog.e("error showing arch entity tab " + tab.getLabel(), e);
				showWarning("Logic Error",
						"Error showing tab " + tab.getLabel());
			}
		}
	}
	
	private String joinStr(String s1, String s2) {
		if (s1 == null) {
			return s2;
		} else if (s2 == null) {
			return s1;
		} 
		return s1 + ";" + s2;
	}

	private void reinitiateArchEntFieldsValue(Tab tab, ArchEntity archEntity) {
		tab.clearViews();
		HashMap<String, String> dirtyMap = new HashMap<String, String>();
		for (EntityAttribute entityAttribute : archEntity.getAttributes()) {
			String s;
			if (dirtyMap.containsKey(entityAttribute.getName())) {
				s = dirtyMap.get(entityAttribute.getName());
			} else {
				s = null;
			}
			
			dirtyMap.put(entityAttribute.getName(), joinStr(s, entityAttribute.getDirtyReason()));
		}
		for (EntityAttribute entityAttribute : archEntity.getAttributes()) {
			if (tab.hasView(entityAttribute.getName())) {
				List<View> views = tab.getViews(entityAttribute.getName());
				if (views != null) {
					String s = dirtyMap.get(entityAttribute.getName());
					entityAttribute.setDirty(s != null);
					entityAttribute.setDirtyReason(s);
					loadArchEntFieldsValue(tab, entityAttribute, views);
				}
			}
		}
	}

	private void showRelationshipTab(String uuid, Tab tab) {
		Object relationshipObj = fetchRel(uuid);
		if (relationshipObj == null) {
			showWarning("Logic Error", "Error showing tab " + tab.getLabel());
			return;
		}
		if (relationshipObj instanceof Relationship) {
			Relationship relationship = (Relationship) relationshipObj;
			try {
				reinitiateRelationshipFieldsValue(tab, relationship);
			} catch (Exception e) {
				FLog.e("error showing relationship tab " + tab.getLabel(), e);
				showWarning("Logic Error",
						"Error showing tab " + tab.getLabel());
			}
		}
	}

	private void reinitiateRelationshipFieldsValue(Tab tab,
			Relationship relationship) {
		tab.clearViews();
		HashMap<String, String> dirtyMap = new HashMap<String, String>();
		for (RelationshipAttribute relationshipAttribute : relationship.getAttributes()) {
			String s;
			if (dirtyMap.containsKey(relationshipAttribute.getName())) {
				s = dirtyMap.get(relationshipAttribute.getName());
			} else {
				s = null;
			}
			
			dirtyMap.put(relationshipAttribute.getName(), joinStr(s, relationshipAttribute.getDirtyReason()));
		}
		for (RelationshipAttribute relationshipAttribute : relationship
				.getAttributes()) {
			if (tab.hasView(relationshipAttribute.getName())) {
				List<View> views = tab
						.getViews(relationshipAttribute.getName());
				if (views != null) {
					String s = dirtyMap.get(relationshipAttribute.getName());
					relationshipAttribute.setDirty(s != null);
					relationshipAttribute.setDirtyReason(s);
					loadRelationshipFieldsValue(tab, relationshipAttribute,
							views);
				}
			}
		}
	}

	private void loadArchEntFieldsValue(Tab tab,
			EntityAttribute entityAttribute, List<View> views) {
		for (View v : views) {
			if (v instanceof CustomEditText) {
				CustomEditText customEditText = (CustomEditText) v;
				setArchEntityFieldValueForType(tab,
						customEditText.getAttributeType(),
						customEditText.getRef(), entityAttribute);

			} else if (v instanceof CustomDatePicker) {
				CustomDatePicker customDatePicker = (CustomDatePicker) v;
				setArchEntityFieldValueForType(tab,
						customDatePicker.getAttributeType(),
						customDatePicker.getRef(), entityAttribute);

			} else if (v instanceof CustomTimePicker) {
				CustomTimePicker customTimePicker = (CustomTimePicker) v;
				setArchEntityFieldValueForType(tab,
						customTimePicker.getAttributeType(),
						customTimePicker.getRef(), entityAttribute);

			} else if (v instanceof CustomLinearLayout) {
				CustomLinearLayout customLinearLayout = (CustomLinearLayout) v;
				setArchEntityFieldValueForType(tab,
						customLinearLayout.getAttributeType(),
						customLinearLayout.getRef(), entityAttribute);

			} else if (v instanceof CustomSpinner) {
				CustomSpinner customSpinner = (CustomSpinner) v;
				setArchEntityFieldValueForType(tab,
						customSpinner.getAttributeType(),
						customSpinner.getRef(), entityAttribute);
			} else if (v instanceof CustomHorizontalScrollView) {
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) v;
				setArchEntityFieldValueForType(tab,
						horizontalScrollView.getAttributeType(),
						horizontalScrollView.getRef(), entityAttribute);
			}
		}
	}

	private void loadRelationshipFieldsValue(Tab tab,
			RelationshipAttribute relationshipAttribute, List<View> views) {
		for (View v : views) {
			if (v instanceof CustomEditText) {
				CustomEditText customEditText = (CustomEditText) v;
				setRelationshipFieldValueForType(tab,
						customEditText.getAttributeType(),
						customEditText.getRef(), relationshipAttribute);

			} else if (v instanceof CustomDatePicker) {
				CustomDatePicker customDatePicker = (CustomDatePicker) v;
				setRelationshipFieldValueForType(tab,
						customDatePicker.getAttributeType(),
						customDatePicker.getRef(), relationshipAttribute);

			} else if (v instanceof CustomTimePicker) {
				CustomTimePicker customTimePicker = (CustomTimePicker) v;
				setRelationshipFieldValueForType(tab,
						customTimePicker.getAttributeType(),
						customTimePicker.getRef(), relationshipAttribute);

			} else if (v instanceof CustomLinearLayout) {
				CustomLinearLayout customLinearLayout = (CustomLinearLayout) v;
				setRelationshipFieldValueForType(tab,
						customLinearLayout.getAttributeType(),
						customLinearLayout.getRef(), relationshipAttribute);

			} else if (v instanceof CustomSpinner) {
				CustomSpinner customSpinner = (CustomSpinner) v;
				setRelationshipFieldValueForType(tab,
						customSpinner.getAttributeType(),
						customSpinner.getRef(), relationshipAttribute);
			} else if (v instanceof CustomHorizontalScrollView) {
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) v;
				setRelationshipFieldValueForType(tab,
						horizontalScrollView.getAttributeType(),
						horizontalScrollView.getRef(), relationshipAttribute);
			}
		}
	}

	private void setArchEntityFieldValueForType(Tab tab, String type,
			String ref, EntityAttribute attribute) {
		if (FREETEXT.equals(type)) {
			setFieldValue(ref, attribute.getText());
		} else if (MEASURE.equals(type)) {
			setFieldValue(ref, attribute.getMeasure());
			setFieldAnnotation(ref, attribute.getText());
		} else if (VOCAB.equals(type)) {
			setFieldValue(ref, attribute.getVocab());
			setFieldAnnotation(ref, attribute.getText());
		}
		setFieldCertainty(ref, attribute.getCertainty());
		setFieldDirty(ref, attribute.isDirty(), attribute.getDirtyReason());
		tab.setValueReference(ref, getFieldValue(ref));
	}

	private void setRelationshipFieldValueForType(Tab tab, String type,
			String ref, RelationshipAttribute relationshipAttribute) {
		if (FREETEXT.equals(type)) {
			setFieldValue(ref, relationshipAttribute.getText());
		} else if (VOCAB.equals(type)) {
			setFieldValue(ref, relationshipAttribute.getVocab());
			setFieldAnnotation(ref, relationshipAttribute.getText());
		}
		setFieldCertainty(ref, relationshipAttribute.getCertainty());
		setFieldDirty(ref, relationshipAttribute.isDirty(), relationshipAttribute.getDirtyReason());
		tab.setValueReference(ref, getFieldValue(ref));
	}

	public void showToast(String message) {
		try {
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(activity.getApplicationContext(),
					message, duration);
			toast.show();
		} catch (Exception e) {
			FLog.e("error showing toast", e);
			showWarning("Logic Error", "Error showing toast");
		}
	}

	public void showAlert(final String title, final String message,
			final String okCallback, final String cancelCallback) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);

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

	}

	public void showWarning(final String title, final String message) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);

		builder.setTitle(title);
		builder.setMessage(message);
		builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// User clicked OK button
			}
		});
		builder.create().show();

	}
	
	public Dialog showBusy(final String title, final String message) {

		BusyDialog d = new BusyDialog(this.activity, title, message, null);
		d.show();
		
		return d;
	}

	public void setFieldValue(String ref, Object valueObj) {
		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);

			if (valueObj instanceof Number) {
				valueObj = valueObj.toString();
			}

			if (valueObj instanceof String) {

				String value = (String) valueObj;
				value = activity.getArch16n().substituteValue(value);

				if (obj instanceof TextView) {
					TextView tv = (TextView) obj;
					tv.setText(value);
				} else if (obj instanceof Spinner) {
					Spinner spinner = (Spinner) obj;

					for (int i = 0; i < spinner.getAdapter().getCount(); ++i) {
						NameValuePair pair = (NameValuePair) spinner
								.getItemAtPosition(i);
						if (value.equalsIgnoreCase(pair.getValue())) {
							spinner.setSelection(i);
							break;
						}
					}
				} else if (obj instanceof LinearLayout) {
					LinearLayout ll = (LinearLayout) obj;

					View child0 = ll.getChildAt(0);

					if (child0 instanceof HorizontalScrollView) {
						HorizontalScrollView horizontalScrollView = (HorizontalScrollView) child0;
						View child1 = horizontalScrollView.getChildAt(0);
						if(child1 instanceof RadioGroup){
							RadioGroup rg = (RadioGroup) child1;
							List<CustomRadioButton> buttons = new ArrayList<CustomRadioButton>();
							for (int i = 0; i < rg.getChildCount(); ++i) {
								View view = rg.getChildAt(i);
								if (view instanceof CustomRadioButton) {
									buttons.add((CustomRadioButton) view);
								}
							}
							rg.removeAllViews();
							for (CustomRadioButton rb : buttons) {
								CustomRadioButton radioButton = new CustomRadioButton(
										rg.getContext());
								radioButton.setText(rb.getText());
								radioButton.setValue(rb.getValue());
								if (rb.getValue().toString()
										.equalsIgnoreCase(value)) {
									radioButton.setChecked(true);
								}
								rg.addView(radioButton);
	
							}
						}

					} else if (child0 instanceof CheckBox) {
						for (int i = 0; i < ll.getChildCount(); ++i) {
							View view = ll.getChildAt(i);
							if (view instanceof CustomCheckBox) {
								CustomCheckBox cb = (CustomCheckBox) view;
								if (cb.getValue().toString()
										.equalsIgnoreCase(value)) {
									cb.setChecked(true);
									break;
								}
							}
						}
					}
				} else if (obj instanceof DatePicker) {
					DatePicker date = (DatePicker) obj;
					DateUtil.setDatePicker(date, value);
				} else if (obj instanceof TimePicker) {
					TimePicker time = (TimePicker) obj;
					DateUtil.setTimePicker(time, value);
				} else if (obj instanceof CustomHorizontalScrollView) {
					CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
					for (CustomImageView customImageView : horizontalScrollView
							.getImageViews()) {
						if (!horizontalScrollView.isMulti()) {
							if (customImageView.getPicture().getId().equals(value)) {
								customImageView.setBackgroundColor(Color.BLUE);
								horizontalScrollView
									.addSelectedImageView(customImageView);
								break;
							}
						}else{
							if (customImageView.getPicture().getUrl().equals(value)) {
								customImageView.setBackgroundColor(Color.BLUE);
								horizontalScrollView
									.addSelectedImageView(customImageView);
								break;
							}
						}
					}
				} else {
					FLog.w("cannot find view " + ref);
					showWarning("Logic Error", "Cannot find view " + ref);
				}
			}

			else if (valueObj instanceof List<?>) {
				
				if (obj instanceof LinearLayout) {
					LinearLayout ll = (LinearLayout) obj;
					
					List<NameValuePair> valueList = convertToNameValuePairs((Collection<?>) valueObj);

					for (NameValuePair pair : valueList) {
						for (int i = 0; i < ll.getChildCount(); ++i) {
							View view = ll.getChildAt(i);
							if (view instanceof CustomCheckBox) {
								CustomCheckBox cb = (CustomCheckBox) view;
								if (cb.getValue()
										.toString()
										.equalsIgnoreCase(
												activity.getArch16n()
														.substituteValue(
																pair.getName()))) {
									cb.setChecked("true".equals(pair.getValue()));
									break;
								}
							}
						}
					}
				} else if (obj instanceof CustomHorizontalScrollView) {
					CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
					
					@SuppressWarnings("unchecked")
					List<String> valueList = (List<String>) valueObj;
					
					for (String value : valueList) {
						for (CustomImageView customImageView : horizontalScrollView
								.getImageViews()) {
							if (!horizontalScrollView.isMulti()) {
								if (customImageView.getPicture().getId().equals(value)) {
									customImageView.setBackgroundColor(Color.BLUE);
									horizontalScrollView
											.addSelectedImageView(customImageView);
								}
							}else{
								if (customImageView.getPicture().getUrl().equals(value)) {
									customImageView.setBackgroundColor(Color.BLUE);
									horizontalScrollView
											.addSelectedImageView(customImageView);
								}
							}
						}
					}
				} else {
					FLog.w("cannot find view " + ref);
					showWarning("Logic Error", "Cannot find view " + ref);
				}
			}
		} catch (Exception e) {
			FLog.e("error setting field value " + ref, e);
			showWarning("Logic Error", "Error setting field value " + ref);
		}
	}

	public void setFieldCertainty(String ref, Object valueObj) {
		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);

			if (valueObj instanceof Number) {
				valueObj = valueObj.toString();
			}

			if (valueObj instanceof String) {

				float value = Float.valueOf((String) valueObj);

				if (obj instanceof CustomEditText) {
					CustomEditText tv = (CustomEditText) obj;
					tv.setCertainty(value);
					tv.setCurrentCertainty(value);
				} else if (obj instanceof CustomSpinner) {
					CustomSpinner spinner = (CustomSpinner) obj;
					spinner.setCertainty(value);
					spinner.setCurrentCertainty(value);
				} else if (obj instanceof CustomLinearLayout) {
					CustomLinearLayout layout = (CustomLinearLayout) obj;
					layout.setCertainty(value);
					layout.setCurrentCertainty(value);
				} else if (obj instanceof CustomDatePicker) {
					CustomDatePicker date = (CustomDatePicker) obj;
					date.setCertainty(value);
					date.setCurrentCertainty(value);
				} else if (obj instanceof CustomTimePicker) {
					CustomTimePicker time = (CustomTimePicker) obj;
					time.setCertainty(value);
					time.setCurrentCertainty(value);
				} else if (obj instanceof CustomHorizontalScrollView) {
					CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
					horizontalScrollView.setCertainty(value);
					horizontalScrollView.setCurrentCertainty(value);
				} else {
					FLog.w("cannot set field certainty " + ref + " = "
							+ valueObj);
					showWarning("Logic Error", "Cannot set field certainty "
							+ ref + " = " + valueObj);
				}
			}
		} catch (Exception e) {
			FLog.e("error setting field certainty " + ref, e);
			showWarning("Logic Error", "Error setting field certainty " + ref);
		}
	}

	public void setFieldAnnotation(String ref, Object valueObj) {
		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);

			if (valueObj instanceof String) {

				String value = (String) valueObj;

				if (obj instanceof CustomEditText) {
					CustomEditText tv = (CustomEditText) obj;
					tv.setAnnotation(value);
					tv.setCurrentAnnotation(value);
				} else if (obj instanceof CustomSpinner) {
					CustomSpinner spinner = (CustomSpinner) obj;
					spinner.setAnnotation(value);
					spinner.setCurrentAnnotation(value);
				} else if (obj instanceof CustomLinearLayout) {
					CustomLinearLayout layout = (CustomLinearLayout) obj;
					layout.setAnnotation(value);
					layout.setCurrentAnnotation(value);
				} else if (obj instanceof CustomHorizontalScrollView) {
					CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
					horizontalScrollView.setAnnotation(value);
					horizontalScrollView.setCurrentAnnotation(value);
				} else {
					FLog.w("cannot set field annotation " + ref + " = "
							+ valueObj);
					showWarning("Logic Error", "Cannot set field annotation "
							+ ref + " = " + valueObj);
				}
			}
		} catch (Exception e) {
			FLog.e("error setting field annotation " + ref, e);
			showWarning("Logic Error", "Error setting field annotation " + ref);
		}
	}
	
	public void setFieldDirty(String ref, boolean isDirty, String isDirtyReason) {
		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			
			if (obj != null) {
				Button dirtyButton = activity.getUIRenderer().getTabForView(ref).getDirtyButton(ref);
				if (dirtyButton != null) {
					dirtyButton.setVisibility(isDirty ? View.VISIBLE : View.GONE);
				}
			}

			if (obj instanceof CustomEditText) {
				CustomEditText tv = (CustomEditText) obj;
				tv.setDirty(isDirty);
				tv.setDirtyReason(isDirtyReason);
			} else if (obj instanceof CustomSpinner) {
				CustomSpinner spinner = (CustomSpinner) obj;
				spinner.setDirty(isDirty);
				spinner.setDirtyReason(isDirtyReason);
			} else if (obj instanceof CustomLinearLayout) {
				CustomLinearLayout layout = (CustomLinearLayout) obj;
				layout.setDirty(isDirty);
				layout.setDirtyReason(isDirtyReason);
			} else if (obj instanceof CustomDatePicker) {
				CustomDatePicker date = (CustomDatePicker) obj;
				date.setDirty(isDirty);
				date.setDirtyReason(isDirtyReason);
			} else if (obj instanceof CustomTimePicker) {
				CustomTimePicker time = (CustomTimePicker) obj;
				time.setDirty(isDirty);
				time.setDirtyReason(isDirtyReason);
			} else if (obj instanceof CustomHorizontalScrollView) {
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				horizontalScrollView.setDirty(isDirty);
				horizontalScrollView.setDirtyReason(isDirtyReason);
			} else {
				FLog.w("cannot set field isDirty " + ref + " = "
						+ isDirty);
				showWarning("Logic Error", "Cannot set field isDirty "
						+ ref + " = " + isDirty);
			}
		} catch (Exception e) {
			FLog.e("error setting field isDirty " + ref, e);
			showWarning("Logic Error", "Error setting field isDirty " + ref);
		}
	}

	public Object getFieldValue(String ref) {

		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);

			if (obj instanceof TextView) {
				TextView tv = (TextView) obj;
				return tv.getText().toString();
			} else if (obj instanceof Spinner) {
				Spinner spinner = (Spinner) obj;
				NameValuePair pair = (NameValuePair) spinner.getSelectedItem();
				if (pair == null)
					return "";
				return pair.getValue();
			} else if (obj instanceof LinearLayout) {
				LinearLayout ll = (LinearLayout) obj;

				View child0 = ll.getChildAt(0);

				if (child0 instanceof CheckBox) {
					List<NameValuePair> valueList = new ArrayList<NameValuePair>();

					for (int i = 0; i < ll.getChildCount(); ++i) {
						View view = ll.getChildAt(i);

						if (view instanceof CustomCheckBox) {
							CustomCheckBox cb = (CustomCheckBox) view;
							if (cb.isChecked()) {
								valueList.add(new NameValuePair(cb.getValue(),
										"true"));
							}
						}
					}
					return valueList;
				} else if (child0 instanceof HorizontalScrollView) {
					
					HorizontalScrollView horizontalScrollView = (HorizontalScrollView) child0;
					View child1 = horizontalScrollView.getChildAt(0);
					if(child1 instanceof RadioGroup){
						RadioGroup rg = (RadioGroup) child1;
						String value = "";
						for (int i = 0; i < rg.getChildCount(); ++i) {
							View view = rg.getChildAt(i);
	
							if (view instanceof CustomRadioButton) {
								CustomRadioButton rb = (CustomRadioButton) view;
								if (rb.isChecked()) {
									value = rb.getValue();
									break;
								}
							}
						}
						return value;
					}
				} else {
					return null;
				}
			} else if (obj instanceof DatePicker) {
				DatePicker date = (DatePicker) obj;
				return DateUtil.getDate(date);
			} else if (obj instanceof TimePicker) {
				TimePicker time = (TimePicker) obj;
				return DateUtil.getTime(time);
			} else if (obj instanceof CustomHorizontalScrollView) {
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				if (!horizontalScrollView.isMulti()) {
					if(horizontalScrollView.getSelectedImageViews() != null && !horizontalScrollView.getSelectedImageViews().isEmpty()){
						return horizontalScrollView.getSelectedImageViews().get(0)
								.getPicture().getId();
					}else{
						return "";
					}
				} else {
					if (horizontalScrollView.getSelectedImageViews() != null && !horizontalScrollView.getSelectedImageViews().isEmpty()) {
						List<String> selectedPictures = new ArrayList<String>();
						for (CustomImageView imageView : horizontalScrollView
								.getSelectedImageViews()) {
							selectedPictures.add(imageView.getPicture()
									.getUrl());
						}
						return selectedPictures;
					}
					return "";
				}
			} else if (obj instanceof CustomListView) {
				CustomListView listView = (CustomListView) obj;
				if (listView.getSelectedItems() != null) {
					List<String> audios = new ArrayList<String>();
					for (Object item : listView.getSelectedItems()) {
						NameValuePair pair = (NameValuePair) item;
						audios.add(pair.getValue());
					}
					return audios;
				} else {
					return "";
				}
			} else {
				FLog.w("cannot find view " + ref);
				showWarning("Logic Error", "Cannot find view " + ref);
				return null;
			}
		} catch (Exception e) {
			FLog.e("error getting field value " + ref, e);
			showWarning("Logic Error", "Error getting field value " + ref);
		}
		return null;
	}

	public Object getFieldCertainty(String ref) {

		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);

			if (obj instanceof CustomEditText) {
				CustomEditText tv = (CustomEditText) obj;
				return String.valueOf(tv.getCurrentCertainty());
			} else if (obj instanceof CustomSpinner) {
				CustomSpinner spinner = (CustomSpinner) obj;
				return String.valueOf(spinner.getCurrentCertainty());
			} else if (obj instanceof CustomLinearLayout) {
				CustomLinearLayout layout = (CustomLinearLayout) obj;
				return String.valueOf(layout.getCurrentCertainty());
			} else if (obj instanceof CustomDatePicker) {
				CustomDatePicker date = (CustomDatePicker) obj;
				return String.valueOf(date.getCurrentCertainty());
			} else if (obj instanceof CustomTimePicker) {
				CustomTimePicker time = (CustomTimePicker) obj;
				return String.valueOf(time.getCurrentCertainty());
			} else if (obj instanceof CustomHorizontalScrollView) {
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				return String.valueOf(horizontalScrollView
						.getCurrentCertainty());
			} else {
				FLog.w("cannot find view " + ref);
				showWarning("Logic Error", "Cannot find view " + ref);
				return null;
			}
		} catch (Exception e) {
			FLog.e("error getting field certainty " + ref, e);
			showWarning("Logic Error", "Error getting field certainty " + ref);
		}
		return null;
	}

	public Object getFieldAnnotation(String ref) {

		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);

			if (obj instanceof CustomEditText) {
				CustomEditText tv = (CustomEditText) obj;
				return tv.getCurrentAnnotation();
			} else if (obj instanceof CustomSpinner) {
				CustomSpinner spinner = (CustomSpinner) obj;
				return spinner.getCurrentAnnotation();
			} else if (obj instanceof CustomLinearLayout) {
				CustomLinearLayout layout = (CustomLinearLayout) obj;
				return layout.getCurrentAnnotation();
			} else if (obj instanceof CustomHorizontalScrollView) {
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				return horizontalScrollView.getCurrentAnnotation();
			} else {
				FLog.w("cannot find view " + ref);
				showWarning("Logic Error", "Cannot find view " + ref);
				return null;
			}
		} catch (Exception e) {
			FLog.e("error getting field annotation " + ref, e);
			showWarning("Logic Error", "Error getting field annotation " + ref);
		}
		return null;
	}

	public String getCurrentTime() {
		return DateUtil.getCurrentTimestampGMT();
	}

	public String saveArchEnt(String entity_id, String entity_type,
			List<Geometry> geo_data, List<EntityAttribute> attributes) {
		try {
			List<Geometry> geomList = GeometryUtil.convertGeometryFromProjToProj(this.project.getSrid(), GeometryUtil.EPSG4326, geo_data);
			return activity.getDatabaseManager().saveArchEnt(entity_id,
					entity_type, WKTUtil.collectionToWKT(geomList), attributes);

		} catch (Exception e) {
			FLog.e("error saving arch entity", e);
			showWarning("Logic Error", "Error saving arch entity");
		}
		return null;
	}

	public Boolean deleteArchEnt(String entity_id){
		try {
			activity.getDatabaseManager().deleteArchEnt(entity_id);
			for(Tab tab : activity.getUIRenderer().getTabList()){
				for(CustomMapView mapView : tab.getMapViewList()){
					mapView.removeFromAllSelections(entity_id);
					mapView.updateSelections();
				}
			}
			return true;
		} catch (jsqlite.Exception e) {
			FLog.e("can not delete arch entity with the supplied id", e);
		}
		return false;
	}

	public String saveRel(String rel_id, String rel_type,
			List<Geometry> geo_data, List<RelationshipAttribute> attributes) {
		try {
			List<Geometry> geomList = GeometryUtil.convertGeometryFromProjToProj(this.project.getSrid(), GeometryUtil.EPSG4326, geo_data);
			return activity.getDatabaseManager().saveRel(rel_id, rel_type,
					WKTUtil.collectionToWKT(geomList), attributes);

		} catch (Exception e) {
			FLog.e("error saving relationship", e);
			showWarning("Logic Error", "Error saving relationship");
		}
		return null;
	}

	public Boolean deleteRel(String rel_id){
		try {
			activity.getDatabaseManager().deleteRel(rel_id);
			for(Tab tab : activity.getUIRenderer().getTabList()){
				for(CustomMapView mapView : tab.getMapViewList()){
					mapView.removeFromAllSelections(rel_id);
					mapView.updateSelections();
				}
			}
			return true;
		} catch (jsqlite.Exception e) {
			FLog.e("can not delete relationship with the supplied id", e);
		}
		return false;
	}
	
	public boolean addReln(String entity_id, String rel_id, String verb) {
		try {
			return activity.getDatabaseManager().addReln(entity_id, rel_id,
					verb);
		} catch (Exception e) {
			FLog.e("error saving arch entity relationship", e);
			showWarning("Logic Error", "Error saving arch entity relationship");
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	public void populateDropDown(String ref, Collection valuesObj) {

		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);

			if (obj instanceof Spinner && valuesObj instanceof ArrayList) {
				Spinner spinner = (Spinner) obj;

				ArrayList<NameValuePair> pairs = null;
				boolean isList = false;
				try {
					@SuppressWarnings("unchecked")
					ArrayList<String> values = (ArrayList<String>) valuesObj;
					pairs = new ArrayList<NameValuePair>();
					for (String s : values) {
						pairs.add(new NameValuePair(s, s));
					}
				} catch (Exception e) {
					isList = true;
				}

				if (isList) {
					@SuppressWarnings("unchecked")
					ArrayList<List<String>> values = (ArrayList<List<String>>) valuesObj;
					pairs = new ArrayList<NameValuePair>();
					for (List<String> list : values) {
						pairs.add(new NameValuePair(activity.getArch16n()
								.substituteValue(list.get(1)), list.get(0)));
					}
				}

				ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
						this.activity,
						android.R.layout.simple_spinner_dropdown_item, pairs);
				spinner.setAdapter(arrayAdapter);
				activity.getUIRenderer().getTabForView(ref)
						.setValueReference(ref, getFieldValue(ref));
			} else {
				showWarning("Logic Error", "Cannot populate drop down " + ref);
			}
		} catch (Exception e) {
			FLog.e("error populate drop down " + ref, e);
			showWarning("Logic Error", "Error populate drop down " + ref);
		}
	}
	
	private ArrayList<NameValuePair> convertToNameValuePairs(Collection<?> valuesObj) throws Exception {
		ArrayList<NameValuePair> pairs = null;
		try {
			@SuppressWarnings("unchecked")
			List<NameValuePair> values = (List<NameValuePair>) valuesObj;
			pairs = new ArrayList<NameValuePair>();
			for (NameValuePair p : values) {
				pairs.add(new NameValuePair(activity.getArch16n()
						.substituteValue(p.getName()), p.getValue()));
			}
		} catch (Exception e) {
			try {
				@SuppressWarnings("unchecked")
				List<List<String>> values = (List<List<String>>) valuesObj;
				pairs = new ArrayList<NameValuePair>();
				for (List<String> list : values) {
					pairs.add(new NameValuePair(activity.getArch16n()
							.substituteValue(list.get(1)), list.get(0)));
				}
			} catch (Exception ee) {
				@SuppressWarnings("unchecked")
				List<String> values = (List<String>) valuesObj;
				pairs = new ArrayList<NameValuePair>();
				for (String value : values) {
					pairs.add(new NameValuePair(activity.getArch16n()
							.substituteValue(value), activity.getArch16n()
							.substituteValue(value)));
				}
			}
		}
		return pairs;
	}

	@SuppressWarnings("rawtypes")
	public void populateList(String ref, Collection valuesObj) {
		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);

			if (valuesObj instanceof ArrayList) {
				ArrayList<NameValuePair> pairs = convertToNameValuePairs(valuesObj);
				
				if (obj instanceof LinearLayout) {
					LinearLayout ll = (LinearLayout) obj;
					
					View child0 = ll.getChildAt(0);
					
					if (child0 instanceof HorizontalScrollView) {
						
						HorizontalScrollView horizontalScrollView = (HorizontalScrollView) child0;
						View child1 = horizontalScrollView.getChildAt(0);
						if(child1 instanceof RadioGroup){
							RadioGroup rg = (RadioGroup) child1;
							rg.removeAllViews();
	
							for (NameValuePair pair : pairs) {
								CustomRadioButton radioButton = new CustomRadioButton(
										ll.getContext());
								radioButton.setText(pair.getName());
								radioButton.setValue(pair.getValue());
								rg.addView(radioButton);
							}
						}
					} else {
						ll.removeAllViews();

						for (NameValuePair pair : pairs) {
							CustomCheckBox checkBox = new CustomCheckBox(
									ll.getContext());
							checkBox.setText(pair.getName());
							checkBox.setValue(pair.getValue());
							ll.addView(checkBox);
						}
					}
				} else if (obj instanceof CustomListView) {
					CustomListView list = (CustomListView) obj;
					ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
							list.getContext(),
							android.R.layout.simple_list_item_1, pairs);
					list.setAdapter(arrayAdapter);
				} else {
					showWarning("Logic Error", "Cannot populate list " + ref);
				}
			}
		} catch (Exception e) {
			FLog.e("error populate list " + ref, e);
			showWarning("Logic Error", "Error populate list " + ref);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void populatePictureGallery(String ref, Collection valuesObj) {
		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);

			List<Picture> pictures = new ArrayList<Picture>();
			if (valuesObj instanceof ArrayList<?>) {
				try {
					ArrayList<List<String>> arrayList = (ArrayList<List<String>>) valuesObj;
					for (List<String> pictureList : arrayList) {
						Picture picture = new Picture(pictureList.get(0),
								activity.getArch16n().substituteValue(pictureList.get(1)), pictureList.get(2));
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

			if (obj instanceof HorizontalScrollView) {
				final CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				horizontalScrollView.removeSelectedImageViews();
				LinearLayout galleriesLayout = (LinearLayout) horizontalScrollView
						.getChildAt(0);
				galleriesLayout.removeAllViews();
				final List<CustomImageView> galleryImages = new ArrayList<CustomImageView>();
				for (Picture picture : pictures) {
					String path = null;
					if(picture.getUrl() != null){
						path = picture.getUrl()
								.contains(
										Environment.getExternalStorageDirectory()
												.getPath()) ? picture.getUrl()
								: activity.getProjectDir() + "/" + picture.getUrl();
					}
					LinearLayout galleryLayout = new LinearLayout(
							galleriesLayout.getContext());
					galleryLayout.setOrientation(LinearLayout.VERTICAL);
					final CustomImageView gallery = new CustomImageView(
							galleriesLayout.getContext());
					LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
							400, 400);
					if(path != null && new File(path).exists()){
						gallery.setImageURI(Uri.parse(path));
					}
					gallery.setBackgroundColor(Color.LTGRAY);
					gallery.setPadding(10, 10, 10, 10);
					gallery.setLayoutParams(layoutParams);
					gallery.setPicture(picture);
					gallery.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							CustomImageView selectedImageView = (CustomImageView) v;
							if (horizontalScrollView.isMulti()) {
								for (ImageView view : galleryImages) {
									if (view.equals(selectedImageView)) {
										if (horizontalScrollView
												.getSelectedImageViews() != null) {
											if (horizontalScrollView
													.getSelectedImageViews()
													.contains(
															selectedImageView)) {
												view.setBackgroundColor(Color.LTGRAY);
												horizontalScrollView
														.removeSelectedImageView(selectedImageView);
											} else {
												view.setBackgroundColor(Color.BLUE);
												horizontalScrollView
														.addSelectedImageView(selectedImageView);
											}
										} else {
											view.setBackgroundColor(Color.BLUE);
											horizontalScrollView
													.addSelectedImageView(selectedImageView);
										}
									}
								}
							} else {
								horizontalScrollView
										.removeSelectedImageViews();
								for (ImageView view : galleryImages) {
									if (view.equals(selectedImageView)) {
										view.setBackgroundColor(Color.BLUE);
										horizontalScrollView
											.addSelectedImageView(selectedImageView);
									} else {
										view.setBackgroundColor(Color.LTGRAY);
									}
								}
							}
						}
					});
					TextView textView = new TextView(
							galleriesLayout.getContext());
					String name = picture.getName() != null ? picture
							.getName() : new File(path).getName();
					textView.setText(name);
					textView.setGravity(Gravity.CENTER_HORIZONTAL);
					textView.setTextSize(20);
					galleryLayout.addView(textView);
					galleryImages.add(gallery);
					galleryLayout.addView(gallery);
					galleriesLayout.addView(galleryLayout);
				}
				horizontalScrollView.setImageViews(galleryImages);
			} else {
				showWarning("Logic Error", "Cannot populate picture gallery "
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error populate picture gallery " + ref, e);
			showWarning("Logic Error", "Error populate picture gallery " + ref);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void populateCameraPictureGallery(String ref, Collection valuesObj) {
		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);

			List<Picture> pictures = new ArrayList<Picture>();
			if (valuesObj instanceof ArrayList<?>) {
				ArrayList<String> values = (ArrayList<String>) valuesObj;
				for (String value : values) {
					Picture picture = new Picture(null, null, value);
					pictures.add(picture);
				}
			}

			if (obj instanceof HorizontalScrollView) {
				final CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				horizontalScrollView.removeSelectedImageViews();
				LinearLayout galleriesLayout = (LinearLayout) horizontalScrollView
						.getChildAt(0);
				galleriesLayout.removeAllViews();
				final List<CustomImageView> galleryImages = new ArrayList<CustomImageView>();
				for (Picture picture : pictures) {
					String path = picture.getUrl();
					File pictureFile = new File(path);
					if (pictureFile.exists()) {
						LinearLayout galleryLayout = new LinearLayout(
								galleriesLayout.getContext());
						galleryLayout.setOrientation(LinearLayout.VERTICAL);
						final CustomImageView gallery = new CustomImageView(
								galleriesLayout.getContext());
						LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
								400, 400);
						gallery.setImageBitmap(decodeFile(new File(path), 400,
								400));
						gallery.setBackgroundColor(Color.LTGRAY);
						gallery.setPadding(10, 10, 10, 10);
						gallery.setLayoutParams(layoutParams);
						gallery.setPicture(picture);
						gallery.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								CustomImageView selectedImageView = (CustomImageView) v;
								if (horizontalScrollView.isMulti()) {
									for (ImageView view : galleryImages) {
										if (view.equals(selectedImageView)) {
											if (horizontalScrollView
													.getSelectedImageViews() != null) {
												if (horizontalScrollView
														.getSelectedImageViews()
														.contains(
																selectedImageView)) {
													view.setBackgroundColor(Color.LTGRAY);
													horizontalScrollView
															.removeSelectedImageView(selectedImageView);
												} else {
													view.setBackgroundColor(Color.BLUE);
													horizontalScrollView
															.addSelectedImageView(selectedImageView);
												}
											} else {
												view.setBackgroundColor(Color.BLUE);
												horizontalScrollView
														.addSelectedImageView(selectedImageView);
											}
										}
									}
								}
							}
						});
						gallery.setOnLongClickListener(new OnLongClickListener() {

							@Override
							public boolean onLongClick(View v) {
								previewCameraPicture(v);
								return true;
							}
						});
						TextView textView = new TextView(
								galleriesLayout.getContext());
						String filePath = new File(path).getName();
						if (filePath.indexOf("_") > 0) {
							filePath = filePath
									.substring(filePath.indexOf("_") + 1);
						}
						String name = picture.getName() != null ? picture
								.getName() : filePath;
						textView.setText(name);
						textView.setGravity(Gravity.CENTER_HORIZONTAL);
						textView.setTextSize(20);
						galleryLayout.addView(textView);
						galleryImages.add(gallery);
						galleryLayout.addView(gallery);
						galleriesLayout.addView(galleryLayout);
					}
				}
				horizontalScrollView.setImageViews(galleryImages);
			} else {
				showWarning("Logic Error", "Cannot populate picture gallery "
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error populate picture gallery " + ref, e);
			showWarning("Logic Error", "Error populate picture gallery " + ref);
		}
	}

	private void previewCameraPicture(View v) {
		CustomImageView selectedImageView = (CustomImageView) v;
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setTitle("Image Preview");

		ScrollView scrollView = new ScrollView(activity);
		LinearLayout layout = new LinearLayout(activity);
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);

		builder.setView(scrollView);
		ImageView view = new ImageView(activity);
		String path = selectedImageView.getPicture().getUrl()
				.contains(Environment.getExternalStorageDirectory().getPath()) ? selectedImageView
				.getPicture().getUrl() : activity.getProjectDir() + "/"
				+ selectedImageView.getPicture().getUrl();
		view.setImageBitmap(decodeFile(new File(path), 500, 500));
		layout.addView(view);
		TextView text = new TextView(activity);
		text.setText(getCameraMetaData(path));
		layout.addView(text);
		builder.setNeutralButton("Done", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// Nothing
			}

		});
		builder.create().show();
	}

	private String getCameraMetaData(String path) {
		File videoFile = new File(path);

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Picture Metadata:");
		stringBuilder.append("\n");
		stringBuilder.append("File name: " + videoFile.getName());
		stringBuilder.append("\n");
		stringBuilder.append("File size: " + videoFile.length() + " bytes");
		stringBuilder.append("\n");
		Date lastModifiedDate = new Date(videoFile.lastModified());
		stringBuilder.append("Picture date: " + lastModifiedDate.toString());
		return stringBuilder.toString();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void populateVideoGallery(String ref, Collection valuesObj) {
		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);

			List<Picture> pictures = new ArrayList<Picture>();
			if (valuesObj instanceof ArrayList<?>) {
				ArrayList<String> values = (ArrayList<String>) valuesObj;
				for (String value : values) {
					Picture picture = new Picture(null, null, value);
					pictures.add(picture);
				}
			}

			if (obj instanceof HorizontalScrollView) {
				final CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				horizontalScrollView.removeSelectedImageViews();
				LinearLayout galleriesLayout = (LinearLayout) horizontalScrollView
						.getChildAt(0);
				galleriesLayout.removeAllViews();
				final List<CustomImageView> galleryImages = new ArrayList<CustomImageView>();
				for (Picture picture : pictures) {
					String path = picture.getUrl();
					File videoFile = new File(path);
					if (videoFile.exists()) {
						LinearLayout galleryLayout = new LinearLayout(
								galleriesLayout.getContext());
						galleryLayout.setOrientation(LinearLayout.VERTICAL);
						final CustomImageView gallery = new CustomImageView(
								galleriesLayout.getContext());
						LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
								400, 400);
						Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(
								path, MediaStore.Images.Thumbnails.MINI_KIND);
						gallery.setImageBitmap(thumbnail);
						gallery.setBackgroundColor(Color.LTGRAY);
						gallery.setPadding(10, 10, 10, 10);
						gallery.setLayoutParams(layoutParams);
						gallery.setPicture(picture);
						gallery.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								CustomImageView selectedImageView = (CustomImageView) v;
								if (horizontalScrollView.isMulti()) {
									for (ImageView view : galleryImages) {
										if (view.equals(selectedImageView)) {
											if (horizontalScrollView
													.getSelectedImageViews() != null) {
												if (horizontalScrollView
														.getSelectedImageViews()
														.contains(
																selectedImageView)) {
													view.setBackgroundColor(Color.LTGRAY);
													horizontalScrollView
															.removeSelectedImageView(selectedImageView);
												} else {
													view.setBackgroundColor(Color.BLUE);
													horizontalScrollView
															.addSelectedImageView(selectedImageView);
												}
											} else {
												view.setBackgroundColor(Color.BLUE);
												horizontalScrollView
														.addSelectedImageView(selectedImageView);
											}
										}
									}
								}
							}
						});
						gallery.setOnLongClickListener(new OnLongClickListener() {

							@Override
							public boolean onLongClick(View v) {
								previewVideo(v);
								return true;
							}
						});
						TextView textView = new TextView(
								galleriesLayout.getContext());
						String filePath = new File(path).getName();
						if (filePath.indexOf("_") > 0) {
							filePath = filePath
									.substring(filePath.indexOf("_") + 1);
						}
						String name = picture.getName() != null ? picture
								.getName() : filePath;
						textView.setText(name);
						textView.setGravity(Gravity.CENTER_HORIZONTAL);
						textView.setTextSize(20);
						galleryLayout.addView(textView);
						galleryImages.add(gallery);
						galleryLayout.addView(gallery);
						galleriesLayout.addView(galleryLayout);
					}
				}
				horizontalScrollView.setImageViews(galleryImages);
			} else {
				showWarning("Logic Error", "Cannot populate video gallery "
						+ ref);
			}
		} catch (Exception e) {
			FLog.e("error populate video gallery " + ref, e);
			showWarning("Logic Error", "Error populate video gallery " + ref);
		}
	}

	private void previewVideo(View v) {
		CustomImageView selectedImageView = (CustomImageView) v;
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setTitle("Video Preview");

		LinearLayout layout = new LinearLayout(activity);
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		layout.setOrientation(LinearLayout.VERTICAL);

		builder.setView(layout);
		VideoView videoView = new VideoView(activity);
		final String path = selectedImageView.getPicture().getUrl()
				.contains(Environment.getExternalStorageDirectory().getPath()) ? selectedImageView
				.getPicture().getUrl() : activity.getProjectDir() + "/"
				+ selectedImageView.getPicture().getUrl();
		videoView.setVideoPath(path);
		videoView.setMediaController(new MediaController(activity));
		videoView.requestFocus();
		videoView.start();
		layout.addView(videoView, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));

		builder.setNegativeButton("Done",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub

					}
				});
		builder.setPositiveButton("View Metadata",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						AlertDialog.Builder builder = new AlertDialog.Builder(
								activity);

						builder.setTitle("Video Preview");

						LinearLayout layout = new LinearLayout(activity);
						layout.setLayoutParams(new LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.MATCH_PARENT));
						layout.setOrientation(LinearLayout.VERTICAL);

						builder.setView(layout);
						TextView text = new TextView(activity);
						text.setText(getVideoMetaData(path));
						layout.addView(text);
						builder.setPositiveButton("Done",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// TODO Auto-generated method stub

									}
								});
						builder.create().show();

					}

				});
		builder.create().show();
	}

	private String getVideoMetaData(String path) {
		File videoFile = new File(path);

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Video Metadata:");
		stringBuilder.append("\n");
		stringBuilder.append("File name: " + videoFile.getName());
		stringBuilder.append("\n");
		stringBuilder.append("File size: " + videoFile.length() + " bytes");
		stringBuilder.append("\n");
		Date lastModifiedDate = new Date(videoFile.lastModified());
		stringBuilder.append("Video date: " + lastModifiedDate.toString());
		MediaPlayer player = new MediaPlayer();
		try {
			player.setDataSource(path);
			player.prepare();
			long duration = player.getDuration();
			stringBuilder.append("\n");
			stringBuilder.append("Video duration: " + duration / 1000
					+ " seconds");
			player.release();
		} catch (Exception e) {
			FLog.e("error obtaining video file duration", e);
		}
		return stringBuilder.toString();
	}

	public static Bitmap decodeFile(File f, int WIDTH, int HIGHT) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			// The new size we want to scale to
			final int REQUIRED_WIDTH = WIDTH;
			final int REQUIRED_HIGHT = HIGHT;
			// Find the correct scale value. It should be the power of 2.
			int scale = 1;
			while (o.outWidth / scale / 2 >= REQUIRED_WIDTH
					&& o.outHeight / scale / 2 >= REQUIRED_HIGHT)
				scale *= 2;

			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (Exception e) {
			FLog.e("error when decode the bitmap", e);
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public void populateAudioList(String ref, Collection valuesObj) {
		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);

			if (valuesObj instanceof ArrayList) {
				final ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
				try {
					@SuppressWarnings("unchecked")
					ArrayList<String> values = (ArrayList<String>) valuesObj;
					for (String s : values) {
						File file = new File(s);
						String filePath = file.getName();
						if (filePath.indexOf("_") > 0) {
							filePath = filePath
									.substring(filePath.indexOf("_") + 1);
						}
						pairs.add(new NameValuePair(filePath, s));
					}
				} catch (Exception e) {
					FLog.e("error passing collections type", e);
				}

				if (obj instanceof CustomListView) {
					final CustomListView list = (CustomListView) obj;
					Map<NameValuePair, Boolean> audios = new HashMap<NameValuePair, Boolean>();
					if(!pairs.isEmpty()){
						for (NameValuePair audio : pairs) {
							if (list.getSelectedItems() != null) {
								if(containsAudio(audio, list.getSelectedItems())){
									audios.put(audio, true);
								}else{
									if(containsAudio(audio, list.getAllItems())){
										audios.put(audio, false);
									}else{
										audios.put(audio, true);
									}
								}
							} else {
								audios.put(audio, true);
							}
						}
					}else{
						audios.clear();
					}
					list.removeSelectedItems();
					for(Entry<NameValuePair, Boolean> audio : audios.entrySet()){
						if(audio.getValue()){
							list.addSelectedItem(audio.getKey());
						}
						list.addAllItem(audio.getKey());
					}

					AudioListAdapter adapter = new AudioListAdapter(audios);
					list.setAdapter(adapter);
					list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> arg0, View view,
								int arg2, long arg3) {
							AudioListItem item = (AudioListItem) view;
							item.toggle();
							if (item.isChecked()) {
								list.addSelectedItem(item.getAudioPair());
							} else {
								list.removeSelectedItem(item.getAudioPair());
							}
						}
					});
					list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

						@Override
						public boolean onItemLongClick(AdapterView<?> arg0,
								View view, int arg2, long arg3) {
							NameValuePair audioPair = pairs.get(arg2);
							String path = audioPair.getValue().contains(
									Environment.getExternalStorageDirectory()
											.getPath()) ? audioPair.getValue()
									: activity.getProjectDir() + "/"
											+ audioPair.getValue();
							previewAudio(path);
							return true;
						}
					});
				} else {
					showWarning("Logic Error", "Cannot populate list " + ref);
				}
			}
		} catch (Exception e) {
			FLog.e("error populate list " + ref, e);
			showWarning("Logic Error", "Error populate list " + ref);
		}
	}

	private boolean containsAudio(NameValuePair audio, List<Object> selectedAudios){
		for (Object object : selectedAudios) {
			NameValuePair nameValuePair = (NameValuePair) object;
			if (nameValuePair.getName().equals(audio.getName())) {
				return true;
			}
		}
		return false;
	}
	private void previewAudio(final String path) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setTitle("Audio Preview");

		LinearLayout layout = new LinearLayout(activity);
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		layout.setOrientation(LinearLayout.VERTICAL);

		builder.setView(layout);

		ToggleButton playButton = new ToggleButton(activity);
		playButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					startPlaying(buttonView, path);
				} else {
					stopPlaying();
				}
			}
		});
		playButton.setTextOn("Stop Audio");
		playButton.setTextOff("Play Audio");
		playButton.setChecked(false);
		layout.addView(playButton);
		TextView text = new TextView(activity);
		text.setText(getAudioMetaData(path));
		layout.addView(text);
		builder.setNeutralButton("Done", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				if (mediaPlayer != null) {
					stopPlaying();
				}
			}

		});
		builder.create().show();
	}

	private String getAudioMetaData(String path) {
		File audioFile = new File(path);

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Audio Metadata:");
		stringBuilder.append("\n");
		stringBuilder.append("File name: " + audioFile.getName());
		stringBuilder.append("\n");
		stringBuilder.append("File size: " + audioFile.length() + " bytes");
		stringBuilder.append("\n");
		Date lastModifiedDate = new Date(audioFile.lastModified());
		stringBuilder.append("Audio date: " + lastModifiedDate.toString());
		MediaPlayer player = new MediaPlayer();
		try {
			player.setDataSource(path);
			player.prepare();
			long duration = player.getDuration();
			stringBuilder.append("\n");
			stringBuilder.append("Audio duration: " + duration / 1000
					+ " seconds");
			player.release();
		} catch (Exception e) {
			FLog.e("error obtaining audio file duration", e);
		}

		return stringBuilder.toString();
	}

	private void startPlaying(final CompoundButton buttonView, String path) {
		mediaPlayer = new MediaPlayer();
		try {
			mediaPlayer.setDataSource(path);
			mediaPlayer.prepare();
			mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

				@Override
				public void onCompletion(MediaPlayer arg0) {
					buttonView.setChecked(false);
				}
			});
			mediaPlayer.start();
		} catch (IOException e) {
			FLog.e("prepare() failed", e);
		}
	}

	private void stopPlaying() {
		mediaPlayer.release();
		mediaPlayer = null;
	}

	public Object fetchArchEnt(String id) {
		try {
			ArchEntity e = activity.getDatabaseManager().fetchArchEnt(id);
			if (e != null) {
				List<Geometry> geomList = e.getGeometryList();
				if (geomList != null) {
					e.setGeometryList(GeometryUtil.convertGeometryFromProjToProj(GeometryUtil.EPSG4326, project.getSrid(), geomList));
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
			Relationship r = activity.getDatabaseManager().fetchRel(id);
			if (r != null) {
				List<Geometry> geomList = r.getGeometryList();
				if (geomList != null) {
					r.setGeometryList(GeometryUtil.convertGeometryFromProjToProj(GeometryUtil.EPSG4326, project.getSrid(), geomList));
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
			return activity.getDatabaseManager().fetchOne(query);
		} catch (Exception e) {
			FLog.e("error fetching one", e);
			showWarning("Logic Error", "Error fetching one");
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public Collection fetchAll(String query) {
		try {
			return activity.getDatabaseManager().fetchAll(query);
		} catch (Exception e) {
			FLog.e("error fetching all", e);
			showWarning("Logic Error", "Error fetching all");
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public Collection fetchEntityList(String type) {
		try {
			return activity.getDatabaseManager().fetchEntityList(type);
		} catch (Exception e) {
			FLog.e("error fetching entity list", e);
			showWarning("Logic Error", "Error fetching entity list");
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public Collection fetchRelationshipList(String type) {
		try {
			return activity.getDatabaseManager().fetchRelationshipList(type);
		} catch (Exception e) {
			FLog.e("error fetching relationship list", e);
			showWarning("Logic Error", "Error fetching relationship list");
		}
		return null;
	}

	private void initialiseBluetoohConnection(BluetoothAdapter adapter) {
		if (adapter != null && adapter.isEnabled()) {
			final Set<BluetoothDevice> pairedDevices = adapter
					.getBondedDevices();
			if (pairedDevices.size() > 0) {
				final List<CharSequence> sequences = new ArrayList<CharSequence>();
				for (BluetoothDevice bluetoothDevice : pairedDevices) {
					sequences.add(bluetoothDevice.getName());
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setTitle("Select bluetooth to connect");
				builder.setItems(
						sequences.toArray(new CharSequence[sequences.size()]),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								for (BluetoothDevice bluetoothDevice : pairedDevices) {
									if (bluetoothDevice.getName().equals(
											sequences.get(item))) {
										BeanShellLinker.this.activity
												.getGPSDataManager()
												.setGpsDevice(bluetoothDevice);
										BeanShellLinker.this.activity
												.getGPSDataManager()
												.startExternalGPSListener();
										break;
									}
								}
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			}
		}
	}

	public void startExternalGPS() {
		initialiseBluetoohConnection(BluetoothAdapter.getDefaultAdapter());
	}

	public void startInternalGPS() {
		this.activity.getGPSDataManager().startInternalGPSListener();
	}

	public Object getGPSPosition() {
		return this.activity.getGPSDataManager().getGPSPosition();
	}
	
	public Object getGPSPositionProjected() {
		GPSLocation l = (GPSLocation) this.activity.getGPSDataManager().getGPSPosition();
		if (l == null) return l;
		MapPos p = GeometryUtil.convertFromProjToProj(GeometryUtil.EPSG4326, project.getSrid(), new MapPos(l.getLongitude(), l.getLatitude()));
		l.setLongitude(p.x);
		l.setLatitude(p.y);
		return l;
	}

	public Object getGPSEstimatedAccuracy() {
		return this.activity.getGPSDataManager().getGPSEstimatedAccuracy();
	}

	public Object getGPSHeading() {
		return this.activity.getGPSDataManager().getGPSHeading();
	}

	public Object getGPSPosition(String gps) {
		return this.activity.getGPSDataManager().getGPSPosition(gps);
	}

	public Object getGPSEstimatedAccuracy(String gps) {
		return this.activity.getGPSDataManager().getGPSEstimatedAccuracy(gps);
	}

	public Object getGPSHeading(String gps) {
		return this.activity.getGPSDataManager().getGPSHeading(gps);
	}

	public void showBaseMap(final String ref, String layerName,
			String filename) {
		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				final CustomMapView mapView = (CustomMapView) obj;

				String filepath = activity.getProjectDir() + "/" + filename;
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				final CustomMapView mapView = (CustomMapView) obj;

				String filepath = activity.getProjectDir() + "/" + filename;
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				MapPos p = GeometryUtil.convertFromProjToProj(project.getSrid(), GeometryUtil.EPSG4326, new MapPos(longitude, latitude));
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				String filepath = activity.getProjectDir() + "/" + filename;
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				String filepath = activity.getProjectDir() + "/" + filename;
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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

	public int drawPoint(String ref, int layerId, MapPos point,
			GeometryStyle style) {

		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				GeometryData geomData = (GeometryData) mapView.drawPoint(layerId, GeometryUtil.convertFromProjToProj(project.getSrid(), GeometryUtil.EPSG4326, point), style).userData;
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				GeometryData geomData = (GeometryData) mapView.drawLine(layerId, GeometryUtil.convertFromProjToProj(project.getSrid(), GeometryUtil.EPSG4326, points), style).userData;
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				GeometryData geomData = (GeometryData) mapView.drawPolygon(layerId, GeometryUtil.convertFromProjToProj(project.getSrid(), GeometryUtil.EPSG4326, points), style).userData;
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				return GeometryUtil.convertGeometryFromProjToProj(GeometryUtil.EPSG3785, project.getSrid(), mapView
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				return GeometryUtil.convertGeometryFromProjToProj(GeometryUtil.EPSG3785, project.getSrid(), mapView
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				return GeometryUtil.convertGeometryFromProjToProj(GeometryUtil.EPSG3785, project.getSrid(), mapView
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
		this.activity.uploadDatabaseToServer(callback);
	}

	public void pullDatabaseFromServer(final String callback) {
		this.activity.downloadDatabaseFromServer(callback);
	}

	public void setSyncEnabled(boolean value) {
		if (value) {
			this.activity.enableSync();
		} else {
			this.activity.disableSync();
		}
	}

	public void addSyncListener(final String startCallback,
			final String successCallback, final String failureCallback) {
		this.activity.addSyncListener(new ShowProjectActivity.SyncListener() {

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
		this.activity.startActivityForResult(cameraIntent,
				ShowProjectActivity.CAMERA_REQUEST_CODE);
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
		activity.startActivityForResult(videoIntent,
				ShowProjectActivity.VIDEO_REQUEST_CODE);
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
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setTitle("FAIMS recording");

		LinearLayout layout = new LinearLayout(activity);
		layout.setOrientation(LinearLayout.VERTICAL);

		builder.setView(layout);
		ToggleButton button = new ToggleButton(activity);
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

	public void setLastVideoFilePath(String path) {
		cameraVideoPath = path;
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

	public String getProjectName() {
		return this.project.getName();
	}
	
	public String getProjectSrid() {
		return this.project.getSrid();
	}

	public String getProjectId() {
		return this.project.getKey();
	}

	public String getProjectSeason() {
		return this.project.getSeason();
	}

	public String getProjectDescription() {
		return this.project.getDescription();
	}

	public String getPermitNo() {
		return this.project.getPermitNo();
	}

	public String getPermitHolder() {
		return this.project.getPermitHolder();
	}

	public String getContactAndAddress() {
		return this.project.getContactAndAddress();
	}

	public String getParticipants() {
		return this.project.getParticipants();
	}
	
	public String getPermitIssuedBy() {
		return this.project.getPermitIssuedBy();
	}

	public String getPermitType() {
		return this.project.getPermitType();
	}

	public String getCopyrightHolder() {
		return this.project.getCopyrightHolder();
	}

	public String getClientSponsor() {
		return this.project.getClientSponsor();
	}

	public String getLandOwner() {
		return this.project.getLandOwner();
	}

	public String hasSensitiveData() {
		return this.project.hasSensitiveData();
	}

	public void setSyncMinInterval(float value) {
		if (value < 0) {
			showWarning("Logic Error", "Invalid sync min interval " + value);
			return;
		}

		this.activity.setSyncMinInterval(value);
	}

	public void setSyncMaxInterval(float value) {
		if (value < 0 || value < this.activity.getSyncMinInterval()) {
			showWarning("Logic Error", "Invalid sync max interval " + value);
			return;
		}

		this.activity.setSyncMaxInterval(value);
	}

	public void setSyncDelay(float value) {
		if (value < 0) {
			showWarning("Logic Error", "Invalid sync delay " + value);
			return;
		}
		this.activity.setSyncDelay(value);
	}

	public void setUser(User user) {
		this.activity.getDatabaseManager().setUserId(user.getUserId());
	}

	public void showFileBrowser(String callback) {
		this.lastFileBrowserCallback = callback;
		this.activity
				.showFileBrowser(ShowProjectActivity.FILE_BROWSER_REQUEST_CODE);
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
			activity.enableFileSync();
		} else {
			activity.disableFileSync();
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
				attachFile += activity.getResources().getString(
						R.string.app_dir);
			} else {
				attachFile += activity.getResources().getString(
						R.string.server_dir);
			}

			if (dir != null && !"".equals(dir)) {
				attachFile += "/" + dir;
			}

			// create directories
			FileUtil.makeDirs(activity.getProjectDir() + "/" + attachFile);
			String name= file.getName();
			
			// create random file path
			attachFile += "/" + UUID.randomUUID() + "_" + name;

			activity.copyFile(filePath, activity.getProjectDir() + "/"
					+ attachFile, new ShowProjectActivity.AttachFileListener() {

						@Override
						public void handleComplete() {
							if (callback != null) {
								execute(callback);
							}
						}
				
			});

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
			final ListView listView = new ListView(activity);
			List<NameValuePair> attachedFiles = new ArrayList<NameValuePair>();
			Map<String, Integer> count = new HashMap<String, Integer>();
			for (String attachedFile : files) {
				String filename = (new File(activity.getProjectDir() + "/"
						+ attachedFile)).getName();
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
						activity.getProjectDir() + "/" + attachedFile);
				attachedFiles.add(file);
			}
			ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
					activity, android.R.layout.simple_list_item_1,
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

							activity.startActivity(intent);
						} catch (Exception e) {
							FLog.e("Can not open file with the extension", e);
							Intent intent = new Intent(Intent.ACTION_VIEW);
							Uri data = Uri.fromFile(file);

							intent.setDataAndType(data, "*/*");

							activity.startActivity(intent);
						}
					} else {
						showWarning(
								"Attached File",
								"The file does not currently exist in the FAIMS folder. If the file is still uploading or syncing please wait and try again when the process has finished");
					}
				}

			});
			AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);

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

	public void storeBeanShellData(Bundle savedInstanceState) {
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

	public void restoreBeanShellData(Bundle savedInstanceState) {
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

	public Geometry createGeometryPoint(MapPos point) {
		return new Point(point, null, createPointStyle(0, 0, 0, 0).toPointStyleSet(), null);
	}

	public void setToolsEnabled(String ref, boolean enabled) {
		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				String filepath = activity.getProjectDir() + "/" + dbPath;
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			return GeometryUtil.convertFromProjToProj(fromSrid, toSrid, p);
		} catch (Exception e) {
			FLog.e("error converting project from " + fromSrid + " to " + toSrid, e);
			showWarning("Logic Error", "Error converting projection from " + fromSrid + " to " + toSrid);
		}
		return null;
	}
	
	public void bindToolEvent(String ref, String type, final String callback) {
		try {
			Object obj = activity.getUIRenderer().getViewByRef(ref);
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
			Object obj = activity.getUIRenderer().getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.updateRenderer();
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
			return activity.getCopyFileCount() > 0;
		} catch (Exception e) {
			FLog.e("error checking for attached files", e);
			showWarning("Logic Error", "Error checking for attached files");
		}
		return false;
	}
	
	public String getAttachedFilePath(String file) {
		try {
			return activity.getProjectDir() + "/" + file;
		} catch (Exception e) {
			FLog.e("error getting attached file path", e);
			showWarning("Logic Error", "Error getting attached file path");
		}
		return null;
	}
	
	public String stripAttachedFilePath(String file) {
		try {
			return file.replace(activity.getProjectDir() + "/", "");
		} catch (Exception e) {
			FLog.e("error stripping attached file path", e);
			showWarning("Logic Error", "Error stripping attached file path");
		}
		return null;
	}
	
}