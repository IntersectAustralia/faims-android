package au.org.intersect.faims.android.ui.form;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.Time;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.data.Project;
import au.org.intersect.faims.android.data.User;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.gps.GPSDataManager;
import au.org.intersect.faims.android.gps.GPSLocation;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.WKTUtil;
import au.org.intersect.faims.android.ui.activity.ShowProjectActivity;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.FileUtil;
import bsh.EvalError;
import bsh.Interpreter;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Marker;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.layers.raster.GdalMapLayer;
import com.nutiteq.layers.vector.OgrLayer;
import com.nutiteq.layers.vector.SpatialiteLayer;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.MarkerStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.Style;
import com.nutiteq.style.StyleSet;
import com.nutiteq.utils.UnscaledBitmapLoader;
import com.nutiteq.vectorlayers.MarkerLayer;

public class BeanShellLinker {
	
	private static final int MAX_OBJECTS = 500;

	private Interpreter interpreter;

	private UIRenderer renderer;

	private AssetManager assets;
	
	private ShowProjectActivity activity;
	
	private DatabaseManager databaseManager;

	private GPSDataManager gpsDataManager;

	private String baseDir;

	private static final String FREETEXT = "freetext";
	private static final String MEASURE = "measure";
	private static final String VOCAB = "vocab";
	
	private HandlerThread handlerThread;
	private Handler currentLocationHandler;
	private Runnable currentLocationTask;

	private MarkerLayer currentPositionLayer;
	private GPSLocation previousLocation;

	private Arch16n arch16n;
	private Project project;
	
	private String persistedObjectName;

	@SuppressWarnings("unused")
	private User user;

	private String lastFileBrowserCallback;

	private HandlerThread trackingHandlerThread;
	private Handler trackingHandler;
	private Runnable trackingTask;
	private Double prevLong;
	private Double prevLat;

	public BeanShellLinker(ShowProjectActivity activity, Arch16n arch16n, AssetManager assets, UIRenderer renderer, 
			DatabaseManager databaseManager, GPSDataManager gpsDataManager, Project project) {
		this.activity = activity;
		this.assets = assets;
		this.renderer = renderer;
		this.databaseManager = databaseManager;
		this.gpsDataManager = gpsDataManager;
		this.interpreter = new Interpreter();
		this.arch16n = arch16n;
		this.project = project;
		try {
			interpreter.set("linker", this);
		} catch (EvalError e) {
			FLog.e("error setting linker", e);
		}
	}
	
	public void sourceFromAssets(String filename) {
		try {
    		interpreter.eval(FileUtil.convertStreamToString(assets.open(filename)));
    	} catch(Exception e) {
    		FLog.w("error sourcing script from assets", e);
    		showWarning("Logic Error", "Error encountered in logic commands");
    	}
	}

	public void persistObject(String name){
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
	
	public void startTrackingGPS(final String type, final int value){
		
		if (trackingHandlerThread == null && trackingHandler == null) {
//			if (!gpsDataManager.isExternalGPSStarted()
//					&& !gpsDataManager.isInternalGPSStarted()) {
//				showWarning("GPS", "No GPS is being used");
//				return;
//			}
			trackingHandlerThread = new HandlerThread("tracking");
			trackingHandlerThread.start();
			trackingHandler = new Handler(trackingHandlerThread.getLooper());
			if("time".equals(type)){
				trackingTask = new Runnable() {
	
					@Override
					public void run() {
						trackingHandler.postDelayed(this, value * 1000);
						if (getGPSPosition() != null) {
							GPSLocation currentLocation = (GPSLocation) getGPSPosition();
							float heading = (Float) getGPSHeading();
							float accuracy = (Float) getGPSEstimatedAccuracy();
							Double longitude = currentLocation.getLongitude();
							Double latitude = currentLocation.getLatitude();
							if (longitude != null && latitude != null) {
								List<Geometry> geo_data = new ArrayList<Geometry>();
								geo_data.add(createGeometryPoint(new MapPos(Float.parseFloat(Double.toString(longitude)),
										Float.parseFloat(Double.toString(latitude)))));
								databaseManager.saveGPSTrack(geo_data, Double.toString(currentLocation.getLongitude()),
										Double.toString(currentLocation.getLatitude()), Float.toString(heading),
										Float.toString(accuracy),type);
							}
						} else {
							showToast("no gps signal at the moment");
						}
					}
				};
				trackingHandler.postDelayed(trackingTask, value * 1000);
			}else if("distance".equals(type)){
				trackingTask = new Runnable() {
					
					@Override
					public void run() {
						trackingHandler.postDelayed(this, 1000);
						if (getGPSPosition() != null) {
							GPSLocation currentLocation = (GPSLocation) getGPSPosition();
							float heading = (Float) getGPSHeading();
							float accuracy = (Float) getGPSEstimatedAccuracy();
							Double longitude = currentLocation.getLongitude();
							Double latitude = currentLocation.getLatitude();
							if (longitude != null && latitude != null) {
								double distance = 0;
								if(prevLong != null && prevLat != null){
									float[] results = new float[1];
									Location.distanceBetween(prevLat, prevLong, latitude, longitude, results);
									distance = results[0];
									if(distance > value){
										List<Geometry> geo_data = new ArrayList<Geometry>();
										Geometry point = createGeometryPoint(new MapPos(Float.parseFloat(Double.toString(longitude)),
												Float.parseFloat(Double.toString(latitude))));
										geo_data.add(point);
										databaseManager.saveGPSTrack(geo_data, Double.toString(currentLocation.getLongitude()),
												Double.toString(currentLocation.getLatitude()), Float.toString(heading),
												Float.toString(accuracy),type);
										prevLong = longitude;
										prevLat = latitude;
									}
								}else{
									prevLong = longitude;
									prevLat = latitude;
								}
							}
						} else {
							showToast("no gps signal at the moment");
						}
					}
				};
				trackingHandler.postDelayed(trackingTask, 1000);
			}else{
				FLog.e("wrong type format is used");
			}
			FLog.d("gps tracking is started");
		}else{
			showToast("gps tracking has been started, please stop it before starting");
		}
	}
	
	public void stopTrackingGPS(){
		if(trackingHandler !=  null){
			trackingHandler.removeCallbacks(trackingTask);
			trackingHandler = null;
		}
		if(trackingHandlerThread != null){
			trackingHandlerThread.quit();
			trackingHandlerThread = null;
		}
	}

	public void bindViewToEvent(String ref, String type, final String code) {
		try{
			
			if ("click".equals(type.toLowerCase(Locale.ENGLISH))) {
				View view = renderer.getViewByRef(ref);
				if (view ==  null) {
					FLog.w("cannot find view " + ref);
					showWarning("Logic Error", "Error cannot find view " + ref);
					return;
				}
				else {
					if (view instanceof CustomListView) {
						final CustomListView listView = (CustomListView) view;
						listView.setOnItemClickListener(new ListView.OnItemClickListener() {

							@Override
							public void onItemClick(AdapterView<?> arg0,
									View arg1, int index, long arg3) {
								try {
									NameValuePair pair = (NameValuePair) listView.getItemAtPosition(index);
									interpreter.set("_list_item_value", pair.getValue());
									execute(code);
								} catch (Exception e) {
									FLog.e("error setting list item value", e);
								}
							}
							
						});
					} else {
						if (view instanceof Spinner) {
							((Spinner) view).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

								@Override
								public void onItemSelected(AdapterView<?> arg0,
										View arg1, int arg2, long arg3) {
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
			}
			else if ("load".equals(type.toLowerCase(Locale.ENGLISH))) {
				TabGroup tg = renderer.getTabGroupByLabel(ref);
				if (tg == null){
					FLog.w("cannot find tabgroup " + ref);
					showWarning("Logic Error", "Error cannot find tabgroup " + tg);
					return;
				}
				else{
					tg.addOnLoadCommand(code);
				}
			} 
			else if ("show".equals(type.toLowerCase(Locale.ENGLISH))) {
				TabGroup tg = renderer.getTabGroupByLabel(ref);
				if (tg == null){
					FLog.w("cannot find tabgroup " + ref);
					showWarning("Logic Error", "Error cannot find tabgroup " + tg);
					return;
				}
				else{
					tg.addOnShowCommand(code);
				}
			}
			else {
				FLog.w("cannot find event type " + type);
				showWarning("Logic Error", "Error bind event type " + type);
			}
		}
		catch(Exception e){
			FLog.e("exception binding event to view " + ref,e);
			showWarning("Logic Error", "Error binding event to view " + ref);
		}
	}

	public void bindFocusAndBlurEvent(String ref, final String focusCallback, final String blurCallBack){
		try {
			View view = renderer.getViewByRef(ref);
			if (view ==  null) {
				FLog.w("cannot find view " + ref);
				showWarning("Logic Error", "Error cannot find view " + ref);
				return;
			}
			else {
				view.setOnFocusChangeListener(new OnFocusChangeListener() {
					
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						if(hasFocus){
							if(focusCallback != null && !focusCallback.isEmpty()){
								execute(focusCallback);
							}
						}else{
							if(blurCallBack != null && !blurCallBack.isEmpty()){
								execute(blurCallBack);
							}
						}
					}
				});
			}
		} catch (Exception e) {
			FLog.e("exception binding focus and blur event to view " + ref,e);
			showWarning("Logic Error", "Error cannot bind focus and blur event to view " + ref);
		}
	}
	
	public void bindMapEvent(String ref, final String clickCallback, final String selectCallback) {
		try{
			View view = renderer.getViewByRef(ref);
			if (view instanceof CustomMapView) {
				final CustomMapView mapView = (CustomMapView) view;
				mapView.getOptions().setMapListener(new CustomMapView.CustomMapListener() {

					@Override
					public void onMapClicked(double x, double y,
							boolean arg2) {
						try {
							interpreter.set("_map_point_clicked", (new EPSG3857()).toWgs84(x, y));
							execute(clickCallback);
						} catch (Exception e) {
							FLog.e("error setting map point clicked", e);
						}
					}

					@Override
					public void onVectorElementClicked(
							VectorElement element, double arg1,
							double arg2, boolean arg3) {
						try {
							int geomId = mapView.getGeometryId((Geometry) element);
							interpreter.set("_map_geometry_selected", geomId);
							execute(selectCallback);
						} catch (Exception e) {
							FLog.e("error setting map geometry selected", e);
						}
					}
					
				});
			}
			else {
				FLog.w("cannot bind map event to view " + ref);
				showWarning("Logic Error", "Error cannot bind map event to view " + ref);
			}
		}catch(Exception e){
			FLog.e("exception binding map event to view " + ref,e);
			showWarning("Logic Error", "Error cannot bind map event to view " + ref);
		}
	}
	
	public void newTabGroup(String label) {
		try{
			TabGroup tabGroup = showTabGroup(label);
			if (tabGroup == null) {
				showWarning("Logic Error", "Error showing new tab group " + label);
				return;
			}
			
			tabGroup.clearTabs();
		}
		catch(Exception e){
			FLog.e("error showing new tabgroup " + label,e);
			showWarning("Logic Error", "Error showing new tab group " + label);
		}
	}
	
	public void newTab(String label) {
		try {
			Tab tab = showTab(label);
			if (tab == null) {
				showWarning("Logic Error", "Error showing new tab group " + label);
				return;
			}
			
			tab.clearViews();
		}
		catch(Exception e) {
			FLog.e("error showing new tab " + label,e);
			showWarning("Logic Error", "Error showing new tab " + label);
		}
	}

	public TabGroup showTabGroup(String label){
		try{
			TabGroup tabGroup = renderer.showTabGroup(this.activity, label);
			if (tabGroup == null) {
				showWarning("Logic Error", "Error showing tabgroup " + label);
				return null;
			}
			return tabGroup;
		}
		catch(Exception e){
			FLog.e("error showing tabgroup " + label,e);
			showWarning("Logic Error", "Error showing tab group " + label);
		}
		return null;
	}
	
	public Tab showTab(String label) {
		try{
			Tab tab = renderer.showTab(label);
			if (tab == null) {
				showWarning("Logic Error", "Error showing tab " + label);
				return null;
			}
			return tab;
		}
		catch(Exception e){
			FLog.e("error showing tab " + label,e);
			showWarning("Logic Error", "Error showing tab " + label);
		}
		return null;
	}

	public void showTabGroup(String id, String uuid){
		try {
			TabGroup tabGroup = renderer.showTabGroup(activity, id);
			if (tabGroup == null) {
				showWarning("Logic Error", "Error showing tab group " + id);
				return ;
			}
			if(tabGroup.getArchEntType() != null){
				showArchEntityTabGroup(uuid, tabGroup);
			}else if(tabGroup.getRelType() != null){
				showRelationshipTabGroup(uuid, tabGroup);
			}else{
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
				return ;
			}
			String[] ids = id.split("/");
			if (ids.length < 2) {
				showWarning("Logic Error", "Error showing tab " + id);
				return ;
			}
			String groupId = ids[0];
			String tabId = ids[1];
			TabGroup tabGroup = renderer.getTabGroupByLabel(groupId);
			if (tabGroup == null) {
				showWarning("Logic Error", "Error showing tab " + id);
				return ;
			}
			Tab tab = tabGroup.showTab(tabId);
			if (tab == null) {
				showWarning("Logic Error", "Error showing tab " + id);
				return ;
			}
			if(tabGroup.getArchEntType() != null){
				showArchEntityTab(uuid, tab);
			}else if(tabGroup.getRelType() != null){
				showRelationshipTab(uuid, tab);
			}else{
				showTab(id);
			}
		} catch (Exception e) {
			FLog.e("error showing tab " + id);
			showWarning("Logic Error", "Error showing tab " + id);
		}
	}

	public void cancelTabGroup(String id, boolean warn){
		try {
			if (id == null) {
				showWarning("Logic Error", "Error cancelling tab group" + id);
				return ;
			}
			final TabGroup tabGroup = renderer.getTabGroupByLabel(id);
			if (tabGroup == null) {
				showWarning("Logic Error", "Error cancelling tab group" + id);
				return ;
			}
			if(warn){
				boolean hasChanges = false;
				if(tabGroup.getArchEntType() != null || tabGroup.getRelType() != null){
					for(Tab tab : tabGroup.getTabs()){
						if(hasChanges(tab)){
							hasChanges = true;
						}
					}
				}
				if(hasChanges){
					AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);
					
					builder.setTitle("Warning");
					builder.setMessage("Are you sure you want to cancel the tab group? You have unsaved changes there.");
					builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					               // User clicked OK button
					        	   goBack();
					           }
					       });
					builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					               // User cancelled the dialog
					           }
					       });
					
					builder.create().show();
				}else{
					goBack();
				}
			}else{
	     		goBack();
			}
		} catch (Exception e) {
			FLog.e("error cancelling tab group " + id);
			showWarning("Logic Error", "Error cancelling tab group " + id);
		}
	}

	public void cancelTab(String id, boolean warn){
		try {
			if (id == null) {
				showWarning("Logic Error", "Error cancelling tab " + id);
				return ;
			}
			String[] ids = id.split("/");
			if (ids.length < 2) {
				showWarning("Logic Error", "Error cancelling tab " + id);
				return ;
			}
			String groupId = ids[0];
			final String tabId = ids[1];
			final TabGroup tabGroup = renderer.getTabGroupByLabel(groupId);
			if (tabGroup == null) {
				showWarning("Logic Error", "Error cancelling tab " + id);
				return ;
			}
			Tab tab = tabGroup.getTab(tabId);
			if(warn){
				if(hasChanges(tab) && (tabGroup.getArchEntType() != null || tabGroup.getRelType() != null)){
					AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);
					
					builder.setTitle("Warning");
					builder.setMessage("Are you sure you want to cancel the tab? You have unsaved changes there.");
					builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					               // User clicked OK button
					        	   if(tabGroup.getTabs().size() == 1){
					        		   goBack();
					        	   }else{
					        		   tabGroup.hideTab(tabId);
					        	   }
					           }
					       });
					builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					               // User cancelled the dialog
					        	   tabGroup.showTab(tabId);
					           }
					       });
					
					builder.create().show();
				}else{
					if(tabGroup.getTabs().size() == 1){
		        	   goBack();
		        	}else{
		        	   tabGroup.hideTab(tabId);
		           }
				}
			}else{
				if(tabGroup.getTabs().size() == 1){
	     		   goBack();
	     	   }else{
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
				if(!getFieldValue(customEditText.getRef()).equals(tab.getStoredValue(customEditText.getRef()))){
					return true;
				}
				if(customEditText.getCertainty() != customEditText.getCurrentCertainty()){
					return true;
				}
				if(!customEditText.getAnnotation().equals(customEditText.getCurrentAnnotation())){
					return true;
				}
				
			} else if (v instanceof CustomDatePicker) {
				CustomDatePicker customDatePicker = (CustomDatePicker) v;
				if(!getFieldValue(customDatePicker.getRef()).equals(tab.getStoredValue(customDatePicker.getRef()))){
					return true;
				}
				if(customDatePicker.getCertainty() != customDatePicker.getCurrentCertainty()){
					return true;
				}
				
			} else if (v instanceof CustomTimePicker) {
				CustomTimePicker customTimePicker = (CustomTimePicker) v;
				if(!getFieldValue(customTimePicker.getRef()).equals(tab.getStoredValue(customTimePicker.getRef()))){
					return true;
				}
				if(customTimePicker.getCertainty() != customTimePicker.getCurrentCertainty()){
					return true;
				}
				
			} else if (v instanceof CustomLinearLayout) {
				CustomLinearLayout customLinearLayout = (CustomLinearLayout) v;
				if(!getFieldValue(customLinearLayout.getRef()).equals(tab.getStoredValue(customLinearLayout.getRef()))){
					return true;
				}
				if(customLinearLayout.getCertainty() != customLinearLayout.getCurrentCertainty()){
					return true;
				}
				if(!customLinearLayout.getAnnotation().equals(customLinearLayout.getCurrentAnnotation())){
					return true;
				}
				
			} else if (v instanceof CustomSpinner) {
				CustomSpinner customSpinner = (CustomSpinner) v;
				if(!getFieldValue(customSpinner.getRef()).equals(tab.getStoredValue(customSpinner.getRef()))){
					return true;
				}
				if(customSpinner.getCertainty() != customSpinner.getCurrentCertainty()){
					return true;
				}
				if(!customSpinner.getAnnotation().equals(customSpinner.getCurrentAnnotation())){
					return true;
				}
				
			} else if (v instanceof CustomHorizontalScrollView) {
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) v;
				if(!getFieldValue(horizontalScrollView.getRef()).equals(tab.getStoredValue(horizontalScrollView.getRef()))){
					return true;
				}
				if(horizontalScrollView.getCertainty() != horizontalScrollView.getCurrentCertainty()){
					return true;
				}
				if(!horizontalScrollView.getAnnotation().equals(horizontalScrollView.getCurrentAnnotation())){
					return true;
				}
			}
		}
		return false;
	}

	public void goBack(){
		this.activity.onBackPressed();
	}

	public int getGpsUpdateInterval(){
		return this.gpsDataManager.getGpsUpdateInterval();
	}

	public void setGpsUpdateInterval(int gpsUpdateInterval) {
		this.gpsDataManager.setGpsUpdateInterval(gpsUpdateInterval);
	}

	public void destroyListener(){
		if(this.currentLocationHandler != null){
			this.currentLocationHandler.removeCallbacks(this.currentLocationTask);
		}
		if(this.handlerThread != null){
			handlerThread.quit();
		}
	}

	private void showArchEntityTabGroup(String uuid, TabGroup tabGroup) {
		Object archEntityObj = fetchArchEnt(uuid);
		if (archEntityObj == null) {
			FLog.d("cannot find arch entity " + tabGroup.getLabel());
			showWarning("Logic Error", "Error showing tab group " + tabGroup.getLabel());
			return ;
		}
		if(archEntityObj instanceof ArchEntity){
			ArchEntity archEntity = (ArchEntity) archEntityObj;
			try {
				for(Tab tab : tabGroup.getTabs()){
					reinitiateArchEntFieldsValue(tab, archEntity);
			    }
			} catch (Exception e) {
				FLog.e("error showing arch entity tab group " + tabGroup.getLabel(),e);
				showWarning("Logic Error", "Error showing tab group " + tabGroup.getLabel());
			}
		}
	}

	private void showRelationshipTabGroup(String uuid, TabGroup tabGroup) {
		Object relationshipObj = fetchRel(uuid);
		if (relationshipObj == null) {
			FLog.d("cannot find relationship " + tabGroup.getLabel());
			showWarning("Logic Error", "Error showing tab group " + tabGroup.getLabel());
			return ;
		}
		if(relationshipObj instanceof Relationship){
			Relationship relationship = (Relationship) relationshipObj;
			try {
				for(Tab tab : tabGroup.getTabs()){
					reinitiateRelationshipFieldsValue(tab, relationship);
			    }
			} catch (Exception e) {
				FLog.e("error showing relationship tab group " + tabGroup.getLabel(),e);
				showWarning("Logic Error", "Error showing tab group " + tabGroup.getLabel());
			}
		}
	}
	
	private void showArchEntityTab(String uuid, Tab tab) {
		Object archEntityObj = fetchArchEnt(uuid);
		if (archEntityObj == null) {
			showWarning("Logic Error", "Error showing tab " + tab.getLabel());
			return ;
		}
		if(archEntityObj instanceof ArchEntity){
			ArchEntity archEntity = (ArchEntity) archEntityObj;
			try {
				reinitiateArchEntFieldsValue(tab, archEntity);
			} catch (Exception e) {
				FLog.e("error showing arch entity tab " + tab.getLabel(),e);
				showWarning("Logic Error", "Error showing tab " + tab.getLabel());
			}
		}
	}

	private void reinitiateArchEntFieldsValue(Tab tab, ArchEntity archEntity) {
		tab.clearViews();
		for (EntityAttribute entityAttribute : archEntity.getAttributes()) {
			if(tab.hasView(entityAttribute.getName())){
				List<View> views = tab.getViews(entityAttribute.getName());
				if (views != null)
					loadArchEntFieldsValue(tab, entityAttribute, views);
			}
		}
	}

	private void showRelationshipTab(String uuid, Tab tab) {
		Object relationshipObj = fetchRel(uuid);
		if (relationshipObj == null) {
			showWarning("Logic Error", "Error showing tab " + tab.getLabel());
			return ;
		}
		if(relationshipObj instanceof Relationship){
			Relationship relationship = (Relationship) relationshipObj;
			try {
				reinitiateRelationshipFieldsValue(tab, relationship);
			} catch (Exception e) {
				FLog.e("error showing relationship tab " + tab.getLabel(),e);
				showWarning("Logic Error", "Error showing tab " + tab.getLabel());
			}
		}
	}

	private void reinitiateRelationshipFieldsValue(Tab tab,
			Relationship relationship) {
		tab.clearViews();
		for (RelationshipAttribute relationshipAttribute : relationship.getAttributes()) {
			if(tab.hasView(relationshipAttribute.getName())){
				List<View> views = tab.getViews(relationshipAttribute.getName());
				if (views != null)
					loadRelationshipFieldsValue(tab, relationshipAttribute, views);
			}
		}
	}

	private void loadArchEntFieldsValue(Tab tab, EntityAttribute entityAttribute, List<View> views) {
		for (View v : views) {
			if (v instanceof CustomEditText) {
				CustomEditText customEditText = (CustomEditText) v;
				setArchEntityFieldValueForType(tab, customEditText.getAttributeType(), customEditText.getRef(), entityAttribute);
				
			} else if (v instanceof CustomDatePicker) {
				CustomDatePicker customDatePicker = (CustomDatePicker) v;
				setArchEntityFieldValueForType(tab, customDatePicker.getAttributeType(), customDatePicker.getRef(), entityAttribute);
				
			} else if (v instanceof CustomTimePicker) {
				CustomTimePicker customTimePicker = (CustomTimePicker) v;
				setArchEntityFieldValueForType(tab, customTimePicker.getAttributeType(), customTimePicker.getRef(), entityAttribute);
				
			} else if (v instanceof CustomLinearLayout) {
				CustomLinearLayout customLinearLayout = (CustomLinearLayout) v;
				setArchEntityFieldValueForType(tab, customLinearLayout.getAttributeType(), customLinearLayout.getRef(), entityAttribute);
				
			} else if (v instanceof CustomSpinner) {
				CustomSpinner customSpinner = (CustomSpinner) v;
				setArchEntityFieldValueForType(tab, customSpinner.getAttributeType(), customSpinner.getRef(), entityAttribute);
			} else if (v instanceof CustomHorizontalScrollView) {
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) v;
				setArchEntityFieldValueForType(tab, horizontalScrollView.getAttributeType(), horizontalScrollView.getRef(), entityAttribute);
			}
		}
	}

	private void loadRelationshipFieldsValue(Tab tab, RelationshipAttribute relationshipAttribute, List<View> views) {
		for (View v : views) {
			if (v instanceof CustomEditText) {
				CustomEditText customEditText = (CustomEditText) v;
				setRelationshipFieldValueForType(tab, customEditText.getAttributeType(), customEditText.getRef(), relationshipAttribute);
				
			} else if (v instanceof CustomDatePicker) {
				CustomDatePicker customDatePicker = (CustomDatePicker) v;
				setRelationshipFieldValueForType(tab, customDatePicker.getAttributeType(), customDatePicker.getRef(), relationshipAttribute);
				
			} else if (v instanceof CustomTimePicker) {
				CustomTimePicker customTimePicker = (CustomTimePicker) v;
				setRelationshipFieldValueForType(tab, customTimePicker.getAttributeType(), customTimePicker.getRef(), relationshipAttribute);
				
			} else if (v instanceof CustomLinearLayout) {
				CustomLinearLayout customLinearLayout = (CustomLinearLayout) v;
				setRelationshipFieldValueForType(tab, customLinearLayout.getAttributeType(), customLinearLayout.getRef(), relationshipAttribute);
				
			} else if (v instanceof CustomSpinner) {
				CustomSpinner customSpinner = (CustomSpinner) v;
				setRelationshipFieldValueForType(tab, customSpinner.getAttributeType(), customSpinner.getRef(), relationshipAttribute);
			} else if (v instanceof CustomHorizontalScrollView) {
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) v;
				setRelationshipFieldValueForType(tab, horizontalScrollView.getAttributeType(), horizontalScrollView.getRef(), relationshipAttribute);
			}
		}
	}

	private void setArchEntityFieldValueForType(Tab tab, String type, String ref, EntityAttribute attribute){
		if(FREETEXT.equals(type)){
			setFieldValue(ref,attribute.getText());
		}else if(MEASURE.equals(type)){
			setFieldValue(ref,attribute.getMeasure());
			setFieldAnnotation(ref, attribute.getText());
		}else if(VOCAB.equals(type)){
			setFieldValue(ref,attribute.getVocab());
			setFieldAnnotation(ref, attribute.getText());
		}
		setFieldCertainty(ref,attribute.getCertainty());
		tab.setValueReference(ref, getFieldValue(ref));
	}
	
	private void setRelationshipFieldValueForType(Tab tab, String type, String ref, RelationshipAttribute relationshipAttribute){
		if(FREETEXT.equals(type)){
			setFieldValue(ref,relationshipAttribute.getText());
		}else if(VOCAB.equals(type)){
			setFieldValue(ref,relationshipAttribute.getVocab());
			setFieldAnnotation(ref, relationshipAttribute.getText());
		}
		setFieldCertainty(ref,relationshipAttribute.getCertainty());
		tab.setValueReference(ref, getFieldValue(ref));
	}

	public void showToast(String message){
		try {
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(activity.getApplicationContext(), message, duration);
			toast.show();
		}
		catch(Exception e){
			FLog.e("error showing toast",e);
			showWarning("Logic Error", "Error showing toast");
		}
	}
	
	public void showAlert(final String title, final String message, final String okCallback, final String cancelCallback){
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);
		
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // User clicked OK button
		        	   execute(okCallback);
		           }
		       });
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // User cancelled the dialog
		        	   execute(cancelCallback);
		           }
		       });
		
		builder.create().show();
		
	}
	
	public void showWarning(final String title, final String message){
		
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
	
	public void setFieldValue(String ref, Object valueObj) {
		try{
			Object obj = renderer.getViewByRef(ref);
			
			if (valueObj instanceof Number) {
				valueObj = valueObj.toString();
			}
			
			if (valueObj instanceof String){
				
				String value = (String) valueObj;
				value = arch16n.substituteValue(value);
				
				if (obj instanceof TextView){
					TextView tv = (TextView) obj;
					tv.setText(value);
				}
				else if (obj instanceof Spinner){
					Spinner spinner = (Spinner) obj;
					
					for( int i = 0; i < spinner.getAdapter().getCount(); ++i ){
						NameValuePair pair = (NameValuePair) spinner.getItemAtPosition(i);
						if (value.equalsIgnoreCase(pair.getValue())){
							spinner.setSelection(i);
							break;
						}
					}
				}
				else if (obj instanceof LinearLayout){
					LinearLayout ll = (LinearLayout) obj;
					
					View child0 = ll.getChildAt(0);
					
					if (child0 instanceof RadioGroup){
						RadioGroup rg = (RadioGroup) child0;
						List<CustomRadioButton> buttons = new ArrayList<CustomRadioButton>();
						for(int i = 0; i < rg.getChildCount(); ++i){
							View view = rg.getChildAt(i);
							if (view instanceof CustomRadioButton){
								buttons.add((CustomRadioButton) view);
							}
						}
						rg.removeAllViews();
						for (CustomRadioButton rb : buttons) {
							CustomRadioButton radioButton = new CustomRadioButton(rg.getContext());
                            radioButton.setText(rb.getText());
                            radioButton.setValue(rb.getValue());
                            if (rb.getValue().toString().equalsIgnoreCase(value)){
                            	radioButton.setChecked(true);
							}
                            rg.addView(radioButton);
                        	
                        }
						
					}else if (child0 instanceof CheckBox){
						for(int i = 0; i < ll.getChildCount(); ++i){
							View view = ll.getChildAt(i);
							if (view instanceof CustomCheckBox){
								CustomCheckBox cb = (CustomCheckBox) view;
								if (cb.getValue().toString().equalsIgnoreCase(value)){
									cb.setChecked(true);
									break;
								}
							}
						}
					} else {
						FLog.w("cannot find view " + ref);
						showWarning("Logic Error", "Cannot find view " + ref);
					}
				}
				else if (obj instanceof DatePicker) {
					DatePicker date = (DatePicker) obj;
					DateUtil.setDatePicker(date, value);
				} 
				else if (obj instanceof TimePicker) {
					TimePicker time = (TimePicker) obj;
					DateUtil.setTimePicker(time, value);
				}else if (obj instanceof CustomHorizontalScrollView){
					CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
					for (CustomImageView customImageView : horizontalScrollView.getImageViews()) {
						if(customImageView.getPicture().getId().equals(value)){
							customImageView.setBackgroundColor(Color.GREEN);
							horizontalScrollView.setSelectedImageView(customImageView);
							break;
						}
					};
				} else {
					FLog.w("cannot find view " + ref);
					showWarning("Logic Error", "Cannot find view " + ref);
				}
			}
			
			else if (valueObj instanceof List<?>){
				
				@SuppressWarnings("unchecked")
				List<NameValuePair> valueList = (List<NameValuePair>) valueObj;
				
				if (obj instanceof LinearLayout){
					LinearLayout ll = (LinearLayout) obj;
					
					for(NameValuePair pair : valueList){
						for(int i = 0; i < ll.getChildCount(); ++i){
							View view = ll.getChildAt(i);
							if (view instanceof CustomCheckBox){
								CustomCheckBox cb = (CustomCheckBox) view;
								if (cb.getValue().toString().equalsIgnoreCase(arch16n.substituteValue(pair.getName()))){
									cb.setChecked("true".equals(pair.getValue()));
									break;
								}
							}
						}
					}
				} else {
					FLog.w("cannot find view " + ref);
					showWarning("Logic Error", "Cannot find view " + ref);
				}
			}
			else {
				FLog.w("cannot set field value " + ref + " = " + valueObj.toString());
				showWarning("Logic Error", "Cannot set field value " + ref + " = " + valueObj.toString());
			}
		}
		catch(Exception e){
			FLog.e("error setting field value " + ref,e);
			showWarning("Logic Error", "Error setting field value " + ref);
		}
	}

	public void setFieldCertainty(String ref, Object valueObj) {
		try{
			Object obj = renderer.getViewByRef(ref);
			
			if (valueObj instanceof Number) {
				valueObj = valueObj.toString();
			}
			
			if (valueObj instanceof String){
				
				float value = Float.valueOf((String) valueObj);
				
				if (obj instanceof CustomEditText){
					CustomEditText tv = (CustomEditText) obj;
					tv.setCertainty(value);
					tv.setCurrentCertainty(value);
				}
				else if (obj instanceof CustomSpinner){
					CustomSpinner spinner = (CustomSpinner) obj;
					spinner.setCertainty(value);
					spinner.setCurrentCertainty(value);
				}
				else if (obj instanceof CustomLinearLayout){
					CustomLinearLayout layout = (CustomLinearLayout) obj;
					layout.setCertainty(value);
					layout.setCurrentCertainty(value);
				}
				else if (obj instanceof CustomDatePicker) {
					CustomDatePicker date = (CustomDatePicker) obj;
					date.setCertainty(value);
					date.setCurrentCertainty(value);
				} 
				else if (obj instanceof CustomTimePicker) {
					CustomTimePicker time = (CustomTimePicker) obj;
					time.setCertainty(value);
					time.setCurrentCertainty(value);
				}else if (obj instanceof CustomHorizontalScrollView){
					CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
					horizontalScrollView.setCertainty(value);
					horizontalScrollView.setCurrentCertainty(value);
				}else {
					FLog.w("cannot set field certainty " + ref + " = " + valueObj.toString());
					showWarning("Logic Error", "Cannot set field certainty " + ref + " = " + valueObj.toString());
				}
			} else {
				FLog.w("cannot find view " + ref);
				showWarning("Logic Error", "Cannot find view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error setting field certainty " + ref,e);
			showWarning("Logic Error", "Error setting field certainty " + ref);
		}
	}

	public void setFieldAnnotation(String ref, Object valueObj) {
		try{
			Object obj = renderer.getViewByRef(ref);
			
			if (valueObj instanceof String){
				
				String value = (String) valueObj;
				
				if (obj instanceof CustomEditText){
					CustomEditText tv = (CustomEditText) obj;
					tv.setAnnotation(value);
					tv.setCurrentAnnotation(value);
				}
				else if (obj instanceof CustomSpinner){
					CustomSpinner spinner = (CustomSpinner) obj;
					spinner.setAnnotation(value);
					spinner.setCurrentAnnotation(value);
				}
				else if (obj instanceof CustomLinearLayout){
					CustomLinearLayout layout = (CustomLinearLayout) obj;
					layout.setAnnotation(value);
					layout.setCurrentAnnotation(value);
				}else if (obj instanceof CustomHorizontalScrollView){
					CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
					horizontalScrollView.setAnnotation(value);
					horizontalScrollView.setCurrentAnnotation(value);
				}else {
					FLog.w("cannot set field certainty " + ref + " = " + valueObj.toString());
					showWarning("Logic Error", "Cannot set field certainty " + ref + " = " + valueObj.toString());
				}
			} else {
				FLog.w("cannot find view " + ref);
				showWarning("Logic Error", "Cannot find view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error setting field annotation " + ref,e);
			showWarning("Logic Error", "Error setting field annotation " + ref);
		}
	}

	public Object getFieldValue(String ref){
		
		try{
			Object obj = renderer.getViewByRef(ref);
			
			if (obj instanceof TextView){
				TextView tv = (TextView) obj;
				return tv.getText().toString();
			}
			else if (obj instanceof Spinner){
				Spinner spinner = (Spinner) obj;
				NameValuePair pair = (NameValuePair) spinner.getSelectedItem();
				if (pair == null) return "";
				return pair.getValue();
			}
			else if (obj instanceof LinearLayout){
				LinearLayout ll = (LinearLayout) obj;
				
				View child0 = ll.getChildAt(0);
				
				if( child0 instanceof CheckBox){
					List<NameValuePair> valueList = new ArrayList<NameValuePair>();
					
					for(int i = 0; i < ll.getChildCount(); ++i){
						View view = ll.getChildAt(i);
						
						if (view instanceof CustomCheckBox){
							CustomCheckBox cb = (CustomCheckBox) view;
							if (cb.isChecked()) {
								valueList.add(new NameValuePair(cb.getValue(), "true"));
							}
						}
					}
					return valueList;
				}
				else if (child0 instanceof RadioGroup){
					RadioGroup rg = (RadioGroup) child0;
					String value = "";
					for(int i = 0; i < rg.getChildCount(); ++i){
						View view = rg.getChildAt(i);
						
						if (view instanceof CustomRadioButton){
							CustomRadioButton rb = (CustomRadioButton) view;
							if (rb.isChecked()){
								value = rb.getValue();
								break;
							}
						}
					}
					return value;
				}
				else{
					FLog.w("cannot find view " + ref);
					showWarning("Logic Error", "Cannot find view " + ref);
					return null;
				}
			}
			else if (obj instanceof DatePicker) {
				DatePicker date = (DatePicker) obj;
				return DateUtil.getDate(date);
			} 
			else if (obj instanceof TimePicker) {
				TimePicker time = (TimePicker) obj;
				return DateUtil.getTime(time);
			}
			else if (obj instanceof CustomHorizontalScrollView){
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				if(horizontalScrollView.getSelectedImageView() != null){
					return horizontalScrollView.getSelectedImageView().getPicture().getId();
				}else{
					return "";
				}
			}
			else {
				FLog.w("cannot find view " + ref);
				showWarning("Logic Error", "Cannot find view " + ref);
				return null;
			}
		}
		catch(Exception e){
			FLog.e("error getting field value " + ref,e);
			showWarning("Logic Error", "Error getting field value " + ref);
		}
		return null;
	}

	public Object getFieldCertainty(String ref){
		
		try{
			Object obj = renderer.getViewByRef(ref);
			
			if (obj instanceof CustomEditText){
				CustomEditText tv = (CustomEditText) obj;
				return String.valueOf(tv.getCurrentCertainty());
			}
			else if (obj instanceof CustomSpinner){
				CustomSpinner spinner = (CustomSpinner) obj;
				return String.valueOf(spinner.getCurrentCertainty());
			}
			else if (obj instanceof CustomLinearLayout){
				CustomLinearLayout layout = (CustomLinearLayout) obj;
				return String.valueOf(layout.getCurrentCertainty());
			}
			else if (obj instanceof CustomDatePicker) {
				CustomDatePicker date = (CustomDatePicker) obj;
				return String.valueOf(date.getCurrentCertainty());
			} 
			else if (obj instanceof CustomTimePicker) {
				CustomTimePicker time = (CustomTimePicker) obj;
				return String.valueOf(time.getCurrentCertainty());
			}
			else if (obj instanceof CustomHorizontalScrollView){
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				return String.valueOf(horizontalScrollView.getCurrentCertainty());
			}
			else {
				FLog.w("cannot find view " + ref);
				showWarning("Logic Error", "Cannot find view " + ref);
				return null;
			}
		}
		catch(Exception e){
			FLog.e("error getting field certainty " + ref, e);
			showWarning("Logic Error", "Error getting field certainty " + ref);
		}
		return null;
	}

	public Object getFieldAnnotation(String ref){
		
		try{
			Object obj = renderer.getViewByRef(ref);
			
			if (obj instanceof CustomEditText){
				CustomEditText tv = (CustomEditText) obj;
				return tv.getCurrentAnnotation();
			}
			else if (obj instanceof CustomSpinner){
				CustomSpinner spinner = (CustomSpinner) obj;
				return spinner.getCurrentAnnotation();
			}
			else if (obj instanceof CustomLinearLayout){
				CustomLinearLayout layout = (CustomLinearLayout) obj;
				return layout.getCurrentAnnotation();
			}
			else if (obj instanceof CustomHorizontalScrollView){
				CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
				return horizontalScrollView.getCurrentAnnotation();
			}
			else {
				FLog.w("cannot find view " + ref);
				showWarning("Logic Error", "Cannot find view " + ref);
				return null;
			}
		}
		catch(Exception e){
			FLog.e("error getting field annotation " + ref,e);
			showWarning("Logic Error", "Error getting field annotation " + ref);
		}
		return null;
	}

	public String getCurrentTime(){
		Time timeNow = new Time();
        timeNow.setToNow();
        return timeNow.format("%Y-%m-%d %H:%M:%S");
	}

	public String saveArchEnt(String entity_id, String entity_type, List<Geometry> geo_data, List<EntityAttribute> attributes) {
		try {
			return databaseManager.saveArchEnt(entity_id, entity_type, WKTUtil.collectionToWKT(geo_data), attributes);
			
		} catch (Exception e) {
			FLog.e("error saving arch entity");
			showWarning("Logic Error", "Error saving arch entity");
		}
		return null;
	}
	
	public String saveRel(String rel_id, String rel_type, List<Geometry> geo_data, List<RelationshipAttribute> attributes) {
		try {
			
			return databaseManager.saveRel(rel_id, rel_type, WKTUtil.collectionToWKT(geo_data), attributes);

		} catch (Exception e) {
			FLog.e("error saving relationship");
			showWarning("Logic Error", "Error saving relationship");
		}
		return null;
	}
	
	public boolean addReln(String entity_id, String rel_id, String verb) {
		try {
			return databaseManager.addReln(entity_id, rel_id, verb);
		} catch (Exception e) {
			FLog.e("error saving arch entity relationship");
			showWarning("Logic Error", "Error saving arch entity relationship");
		}
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	public void populateDropDown(String ref, Collection valuesObj){
		
		try {
			Object obj = renderer.getViewByRef(ref);
	
			if (obj instanceof Spinner && valuesObj instanceof ArrayList){
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
						pairs.add(new NameValuePair(arch16n.substituteValue(list.get(1)), list.get(0)));
					}
				}
				
				ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
	                    this.activity,
	                    android.R.layout.simple_spinner_dropdown_item,
	                    pairs);
	            spinner.setAdapter(arrayAdapter);
	            renderer.getTabForView(ref).setValueReference(ref, getFieldValue(ref));
			}
			else {
				showWarning("Logic Error", "Cannot populate drop down " + ref);
			}
		} catch (Exception e) {
			FLog.e("error populate drop down " + ref);
			showWarning("Logic Error", "Error populate drop down " + ref);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void populateList(String ref, Collection valuesObj){
		try {
			Object obj = renderer.getViewByRef(ref);
	
			if (valuesObj instanceof ArrayList){
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
						pairs.add(new NameValuePair(arch16n.substituteValue(list.get(1)), list.get(0)));
					}
				}
				if(obj instanceof LinearLayout){
					LinearLayout ll = (LinearLayout) obj;
					
					View child0 = ll.getChildAt(0);
					
					
					if( child0 instanceof CheckBox){
						ll.removeAllViews();
						
						for (NameValuePair pair : pairs) {
							CustomCheckBox checkBox = new CustomCheckBox(ll.getContext());
		                    checkBox.setText(pair.getName());
		                    checkBox.setValue(pair.getValue());
		                    ll.addView(checkBox);
						}
					}
					else if (child0 instanceof RadioGroup){
						RadioGroup rg = (RadioGroup) child0;
						rg.removeAllViews();
						
						for (NameValuePair pair : pairs) {
							CustomRadioButton radioButton = new CustomRadioButton(ll.getContext());
		                    radioButton.setText(pair.getName());
		                    radioButton.setValue(pair.getValue());
		                    rg.addView(radioButton);
						}
					}
				}else if(obj instanceof CustomListView){
					CustomListView list = (CustomListView) obj;
	                ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
	                        list.getContext(),
	                        android.R.layout.simple_list_item_1,
	                        pairs);
	                list.setAdapter(arrayAdapter);
				} else {
					showWarning("Logic Error", "Cannot populate list " + ref);
				}
			}
		} catch (Exception e) {
			FLog.e("error populate list " + ref);
			showWarning("Logic Error", "Error populate list " + ref);
		}
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void populatePictureGallery(String ref, Collection valuesObj){
		try {
			Object obj = renderer.getViewByRef(ref);
			
			List<Picture> pictures = new ArrayList<Picture>();
			if(valuesObj instanceof ArrayList<?>){
				ArrayList<List<String>> arrayList = (ArrayList<List<String>>) valuesObj;
				for(List<String> pictureList : arrayList){
					Picture picture = new Picture(pictureList.get(0), pictureList.get(1), pictureList.get(2));
					pictures.add(picture);
				}
			}
			
			if(obj instanceof HorizontalScrollView){
				final CustomHorizontalScrollView horizontalScrollView = (CustomHorizontalScrollView) obj;
		        LinearLayout galleriesLayout = (LinearLayout) horizontalScrollView.getChildAt(0);
		        galleriesLayout.removeAllViews();
		        final List<CustomImageView> galleryImages = new ArrayList<CustomImageView>();
		        for (Picture picture : pictures) {
		        	File pictureFile = new File(baseDir + "/" + picture.getUrl());
		        	if(pictureFile.exists()){
		        		LinearLayout galleryLayout = new LinearLayout(galleriesLayout.getContext());
		        		galleryLayout.setOrientation(LinearLayout.VERTICAL);
		        		final CustomImageView gallery = new CustomImageView(galleriesLayout.getContext());
		        		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(400, 400);
		                gallery.setImageURI(Uri.parse(baseDir + "/" + picture.getUrl()));
		                gallery.setBackgroundColor(Color.RED);
		                gallery.setPadding(10, 10, 10, 10);
		                gallery.setLayoutParams(layoutParams);
		                gallery.setPicture(picture);
		                gallery.setOnClickListener(new OnClickListener() {
		
		                    @Override
		                    public void onClick(View v) {
		                    	CustomImageView selectedImageView = (CustomImageView) v;
		                        horizontalScrollView.setSelectedImageView(selectedImageView);
		                        for (ImageView view : galleryImages) {
		                            if (view.equals(selectedImageView)) {
		                                view.setBackgroundColor(Color.GREEN);
		                            } else {
		                                view.setBackgroundColor(Color.RED);
		                            }
		                        }
		                    }
		                });
		                TextView textView = new TextView(galleriesLayout.getContext());
		                textView.setText(picture.getName());
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
				showWarning("Logic Error", "Cannot populate picture gallery " + ref);
			}
		} catch (Exception e) {
			FLog.e("error populate picture gallery " + ref , e);
			showWarning("Logic Error", "Error populate picture gallery " + ref);
		}
	}

	public Object fetchArchEnt(String id){
		try {
			return databaseManager.fetchArchEnt(id);
		} catch (Exception e) {
			FLog.e("error fetching arch entity");
			showWarning("Logic Error", "Error fetching arch entity");
		}
		return null;
	}

	public Object fetchRel(String id){
		try {
			return databaseManager.fetchRel(id);
		} catch (Exception e) {
			FLog.e("error fetching relationship");
			showWarning("Logic Error", "Error fetching relationship");
		}
		return null;
	}

	public Object fetchOne(String query){
		try {
			return databaseManager.fetchOne(query);
		} catch (Exception e) {
			FLog.e("error fetching one");
			showWarning("Logic Error", "Error fetching one");
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public Collection fetchAll(String query){
		try {
			return databaseManager.fetchAll(query);
		} catch (Exception e) {
			FLog.e("error fetching all");
			showWarning("Logic Error", "Error fetching all");
		}
		return null;
	}
	
	private void initialiseBluetoohConnection(BluetoothAdapter adapter) {
        if (adapter != null && adapter.isEnabled()) {
            final Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                final List<CharSequence> sequences = new ArrayList<CharSequence>();
            	for (BluetoothDevice bluetoothDevice : pairedDevices) {
            		sequences.add(bluetoothDevice.getName());
                }
            	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            	builder.setTitle("Select bluetooth to connect");
            	builder.setItems(sequences.toArray(new CharSequence[sequences.size()]), new DialogInterface.OnClickListener() {
            	    public void onClick(DialogInterface dialog, int item) {
            	    	for (BluetoothDevice bluetoothDevice : pairedDevices) {
	                		if(bluetoothDevice.getName().equals(sequences.get(item))){
	                			gpsDataManager.setGpsDevice(bluetoothDevice);
	                			gpsDataManager.startExternalGPSListener();
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
	
	public void startExternalGPS(){
		initialiseBluetoohConnection(BluetoothAdapter.getDefaultAdapter());
	}
	
	public void startInternalGPS(){
		gpsDataManager.startInternalGPSListener();
	}
	
	public Object getGPSPosition(){
		return this.gpsDataManager.getGPSPosition();
	}

	public Object getGPSEstimatedAccuracy(){
		return this.gpsDataManager.getGPSEstimatedAccuracy();
	}

	public Object getGPSHeading(){
		return this.gpsDataManager.getGPSHeading();
	}

	public Object getGPSPosition(String gps){
		return this.gpsDataManager.getGPSPosition(gps);
	}

	public Object getGPSEstimatedAccuracy(String gps){
		return this.gpsDataManager.getGPSEstimatedAccuracy(gps);
	}

	public Object getGPSHeading(String gps){
		return this.gpsDataManager.getGPSHeading(gps);
	}

	public void showRasterMap(final String ref, String filename) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				final CustomMapView mapView = (CustomMapView) obj;
				
				String filepath = baseDir + "/maps/" + filename;
				if (!new File(filepath).exists()) {
					FLog.w("Map file " + filepath + " does not exist");
                    showWarning("Logic Error", "Erorr map does not exist " + filename);
					return;
				}
				
        		final GdalMapLayer gdalLayer;
	            gdalLayer = new GdalMapLayer(new EPSG3857(), 0, 18, CustomMapView.nextId(), filepath, mapView, true);
	            gdalLayer.setShowAlways(true);
	            mapView.getLayers().setBaseLayer(gdalLayer);
	            if(this.handlerThread != null){
	            	this.handlerThread.quit();
	            }
	            if(this.currentLocationHandler != null){
	            	if(this.currentLocationTask != null){
	            		this.currentLocationHandler.removeCallbacks(currentLocationTask);
	            	}
	            }
	            this.handlerThread = new HandlerThread("MapHandler");
	    		this.handlerThread.start();
	    		this.currentLocationHandler = new Handler(this.handlerThread.getLooper());
	            this.currentLocationTask = new Runnable() {
						
					@Override
					public void run() {
						Object currentLocation = getGPSPosition();
						if(currentLocation != null){
							GPSLocation location = (GPSLocation) currentLocation;
							previousLocation = location;
							Bitmap pointMarker = UnscaledBitmapLoader.decodeResource(
									activity.getResources(), R.drawable.blue_dot);
		                    MarkerStyle markerStyle = MarkerStyle.builder().setBitmap(pointMarker)
		                            .setSize(1.0f).setAnchorX(MarkerStyle.CENTER).setAnchorY(MarkerStyle.CENTER).build();
		                    MapPos markerLocation = gdalLayer.getProjection().fromWgs84(
		                            location.getLongitude(), location.getLatitude());
		                    if(currentPositionLayer == null){
		                    	currentPositionLayer = new MarkerLayer(gdalLayer.getProjection());
			                    currentPositionLayer.add(new Marker(markerLocation, null, markerStyle, null));
			                    mapView.getLayers().addLayer(currentPositionLayer);
		                    }else{
		                    	currentPositionLayer.clear();
		                    	currentPositionLayer.add(new Marker(markerLocation, null, markerStyle, null));
		                    	currentPositionLayer.getComponents().mapRenderers.getMapRenderer().frustumChanged();
		                    }
		                    
						}else{
							if(previousLocation != null){
								// when there is no gps signal for two minutes, change the color of the marker to be grey
								if(System.currentTimeMillis() - previousLocation.getTimeStamp() > 120 * 1000){
									Bitmap pointMarker = UnscaledBitmapLoader.decodeResource(
				                            activity.getResources(), R.drawable.grey_dot);
				                    MarkerStyle markerStyle = MarkerStyle.builder().setBitmap(pointMarker)
				                            .setSize(0.5f).build();
				                    MapPos markerLocation = gdalLayer.getProjection().fromWgs84(
				                    		previousLocation.getLongitude(), previousLocation.getLatitude());
				                    if(currentPositionLayer == null){
				                    	currentPositionLayer = new MarkerLayer(gdalLayer.getProjection());
					                    currentPositionLayer.add(new Marker(markerLocation, null, markerStyle, null));
					                    mapView.getLayers().addLayer(currentPositionLayer);
				                    }else{
				                    	currentPositionLayer.clear();
				                    	currentPositionLayer.add(new Marker(markerLocation, null, markerStyle, null));
				                    	currentPositionLayer.getComponents().mapRenderers.getMapRenderer().frustumChanged();
				                    }
									previousLocation = null;
								}
							}
						}
						currentLocationHandler.postDelayed(this, getGpsUpdateInterval());
					}
				};
                this.currentLocationHandler.postDelayed(currentLocationTask, getGpsUpdateInterval());
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error rendering raster map",e);
			showWarning("Logic Error", "Error cannot render raster map " + ref);
		}
	}
	
	public void setMapFocusPoint(String ref, float longitude, float latitude) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				if (latitude < -90.0f || latitude > 90.0f) {
					FLog.w("latitude out of range " + latitude);
					showWarning("Logic Error", "Error map latitude out of range " + latitude);
				}
				
				mapView.setFocusPoint(new EPSG3857().fromWgs84(longitude, latitude));
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error setting map focus point " + ref,e);
			showWarning("Logic Error", "Error setting map focus point " + ref);
		}
	}
	
	public void setMapRotation(String ref, float rotation) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				// rotation - 0 = north-up
                mapView.setRotation(rotation);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error setting map rotation " + ref,e);
			showWarning("Logic Error", "Error setting map rotation " + ref);
		}
	}
	
	public void setMapZoom(String ref, float zoom) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				// zoom - 0 = world, like on most web maps
                mapView.setZoom(zoom);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error setting map zoom " + ref,e);
			showWarning("Logic Error", "Error setting map zoom " + ref);
		}
	}
	
	public void setMapTilt(String ref, float tilt) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				// tilt means perspective view. Default is 90 degrees for "normal" 2D map view, minimum allowed is 30 degrees.
                mapView.setTilt(tilt);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error setting map tilt " + ref,e);
			showWarning("Logic Error", "Error setting map tilt " + ref);
		}
	}
	
	public void centerOnCurrentPosition(String ref){
		Object currentLocation = getGPSPosition();
		if(currentLocation != null){
			GPSLocation location = (GPSLocation) currentLocation;
			setMapFocusPoint(ref, (float) location.getLongitude(), (float) location.getLatitude());
		}
	}

	public int showVectorLayer(String ref, String filename, StyleSet<PointStyle> pointStyleSet, StyleSet<LineStyle> lineStyleSet, StyleSet<PolygonStyle> polygonStyleSet) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				OgrLayer ogrLayer = new OgrLayer(new EPSG3857(), baseDir + "/maps/" + filename, null, 
						MAX_OBJECTS, pointStyleSet, lineStyleSet, polygonStyleSet);
                // ogrLayer.printSupportedDrivers();
                // ogrLayer.printLayerDetails(table);
				return mapView.addVectorLayer(ogrLayer);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error showing vector layer" + ref,e);
			showWarning("Logic Error", "Error showing vector layer " + ref);
		}
		return -1;
	}
	
	public int showSpatialLayer(String ref, String filename, String tablename, String labelColumn, StyleSet<PointStyle> pointStyleSet, StyleSet<LineStyle> lineStyleSet, StyleSet<PolygonStyle> polygonStyleSet) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				SpatialiteLayer spatialLayer = new SpatialiteLayer(new EPSG3857(), baseDir + "/maps/" + filename, tablename, "Geometry",
                        new String[]{labelColumn}, MAX_OBJECTS, pointStyleSet, lineStyleSet, polygonStyleSet);
				return mapView.addVectorLayer(spatialLayer);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error showing spatialite layer" + ref,e);
			showWarning("Logic Error", "Error showing spatialite layer " + ref);
		}
		return -1;
	}
	
	public void clearVectorLayer(String ref, int layerId) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				mapView.removeVectorLayer(layerId);
				
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error clearing vector layer" + ref,e);
			showWarning("Logic Error", "Error clearing vector layer " + ref);
		}
	}
	
	public int createVectorLayer(String ref) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				CanvasLayer layer = new CanvasLayer(new EPSG3857());
				return mapView.addVectorLayer(layer);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error creating vector layer " + ref,e);
			showWarning("Logic Error", "Error creating vector layer " + ref);
		}
		return -1;
	}
	
	public void setVectorLayerVisible(String ref, int layerId, boolean visible) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				mapView.setLayerVisible(layerId, visible);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error setting vector layer visiblity " + ref,e);
			showWarning("Logic Error", "Error setting vector layer visibility " + ref);
		}
	}
	
	public int drawPoint(String ref, int layerId, MapPos point, StyleSet<PointStyle> styleSet) {
		
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				CanvasLayer canvas = (CanvasLayer) mapView.getVectorLayer(layerId);
				
				int id = canvas.addPoint(point, styleSet);
				canvas.updateRenderer();
				return id;
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error drawing point " + ref,e);
			showWarning("Logic Error", "Error drawing point " + ref);
		}
		return -1;
	}
	
	public int drawLine(String ref, int layerId, List<MapPos> points, StyleSet<LineStyle> styleSet) {
		
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				CanvasLayer canvas = (CanvasLayer) mapView.getVectorLayer(layerId);
				
				int id = canvas.addLine(points, styleSet);
				canvas.updateRenderer();
				return id;
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error drawing line " + ref,e);
			showWarning("Logic Error", "Error drawing line " + ref);
		}
		return -1;
	}
	
	public int drawPolygon(String ref, int layerId, List<MapPos> points, StyleSet<PolygonStyle> styleSet) {
		
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				CanvasLayer canvas = (CanvasLayer) mapView.getVectorLayer(layerId);
				
				int id = canvas.addPolygon(points, styleSet);
				canvas.updateRenderer();
				return id;
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error drawing polygon " + ref,e);
			showWarning("Logic Error", "Error drawing polygon " + ref);
		}
		return -1;
	}
	
	public void clearGeometry(String ref, int layerId, int geomId) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				CanvasLayer canvas = (CanvasLayer) mapView.getVectorLayer(layerId);
				
				canvas.removeGeometry(geomId);
				canvas.updateRenderer();
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error clearing geometry " + ref,e);
			showWarning("Logic Error", "Error clearing geometry " + ref);
		}
	}
	
	public void clearGeometryList(String ref, int layerId, List<Integer> geomList) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				CanvasLayer canvas = (CanvasLayer) mapView.getVectorLayer(layerId);
				
				if (geomList.size() > 0) {
					for (Integer geomId : geomList) {
						canvas.removeGeometry(geomId);
					}
					canvas.updateRenderer();
				}
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error clearing geometry list " + ref,e);
			showWarning("Logic Error", "Error clearing geometry list " + ref);
		}
	}
	
	public List<Geometry> getGeometryList(String ref, int layerId) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;

				CanvasLayer canvas = (CanvasLayer) mapView.getVectorLayer(layerId);
				
				return canvas.getTransformedGeometryList();
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error getting geometry list " + ref,e);
			showWarning("Logic Error", "Error getting geometry list " + ref);
		}
		return null;
	}
	
	public Geometry getGeometry(String ref, int layerId, int geomId) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				CanvasLayer canvas = (CanvasLayer) mapView.getVectorLayer(layerId);
				
				return canvas.getTransformedGeometry(geomId);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error getting geomtry " + ref,e);
			showWarning("Logic Error", "Error getting geometry " + ref);
		}
		return null;
	}
	
	public void lockMapView(String ref, boolean lock) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.setViewLocked(lock);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error locking map view " + ref,e);
			showWarning("Logic Error", "Error locking map view " + ref);
		}
	}
	
	public void drawGeometryOverlay(String ref, int geomId) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				Geometry geom = mapView.getGeometry(geomId);
				if (geom == null) {
					FLog.d("cannot find geometry to overlay");
					showWarning("Logic Error", "Cannot find geometry to overlay");
					return ;
				}
				mapView.drawGeometrOverlay(geom);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error drawing geometry overlay " + ref,e);
			showWarning("Logic Error", "Error drawing geometry overlay " + ref);
		}
	}
	
	public void clearGeometryOverlay(String ref) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.drawGeometrOverlay(null);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error clearing geometry overlay " + ref,e);
			showWarning("Logic Error", "Error clearing geometry overlay " + ref);
		}
	}
	
	public void replaceGeometryOverlay(String ref, int geomId) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				Geometry geom = mapView.getGeometry(geomId);
				if (geom == null) {
					FLog.d("cannot find geometry overlay");
					showWarning("Logic Error", "Cannot find geometry overlay");
					return ;
				}
				mapView.replaceGeometryOverlay(geomId);
			} else {
				FLog.w("cannot find map view " + ref);
				showWarning("Logic Error", "Error cannot find map view " + ref);
			}
		}
		catch(Exception e){
			FLog.e("error replacing geometry overlay " + ref,e);
			showWarning("Logic Error", "Error replacing geometry overlay " + ref);
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
	
	public void addSyncListener(final String startCallback, final String successCallback, final String failureCallback) {
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
	
	public String getProjectName(){
		return this.project.getName();
	}
	
	public String getProjectId(){
		return this.project.getKey();
	}
	
	public String getProjectSeason(){
		return this.project.getSeason();
	}
	
	public String getProjectDescription(){
		return this.project.getDescription();
	}
	
	public String getPermitNo(){
		return this.project.getPermitNo();
	}
	
	public String getPermitHolder(){
		return this.project.getPermitHolder();
	}
	
	public String getContactAndAddress(){
		return this.project.getContactAndAddress();
	}
	
	public String getParticipants(){
		return this.project.getParticipants();
	}

	public UIRenderer getUIRenderer(){
		return this.renderer;
	}

	public void setBaseDir(String dir) {
		this.baseDir = dir;
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
		this.user = user;
		this.databaseManager.setUserId(user.getUserId());
	}
	
	public void showFileBrowser(String callback) {
		this.lastFileBrowserCallback = callback;
		this.activity.showFileBrowser();
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
	
	public PointStyle createPointStyle(int color, float size, float pickSize) {
		return PointStyle.builder().setColor(color).setSize(size).setPickingSize(pickSize).build();
	}
	
	public LineStyle createLineStyle(int color, float width, float pickWidth) {
		return LineStyle.builder().setColor(color).setWidth(width).setPickingWidth(pickWidth).build();
	}
	
	public LineStyle createLineStyle(int color, float width, float pickWidth, PointStyle pointStyle) {
		return LineStyle.builder().setColor(color).setWidth(width).setPickingWidth(pickWidth).setPointStyle(pointStyle).build();
	}
	
	public PolygonStyle createPolygonStyle(int color) {
		return PolygonStyle.builder().setColor(color).build();
	}
	
	public PolygonStyle createPolygonStyle(int color, LineStyle lineStyle) {
		return PolygonStyle.builder().setColor(color).setLineStyle(lineStyle).build();
	}
	
	public StyleSet<? extends Style> createStyleSet(int minZoom, Style style) {
		if (style instanceof PointStyle) {
			StyleSet<PointStyle> pointStyleSet = new StyleSet<PointStyle>();
			pointStyleSet.setZoomStyle(minZoom, (PointStyle) style);
			return pointStyleSet;
		} else if (style instanceof LineStyle) {
			StyleSet<LineStyle> lineStyleSet = new StyleSet<LineStyle>();
			lineStyleSet.setZoomStyle(minZoom, (LineStyle) style);
			return lineStyleSet;
		} else if (style instanceof PolygonStyle) {
			StyleSet<PolygonStyle> polygonStyleSet = new StyleSet<PolygonStyle>();
			polygonStyleSet.setZoomStyle(minZoom, (PolygonStyle) style);
			return polygonStyleSet;
		} else {
			FLog.e("cannot create style set");
			showWarning("Logic Error", "Error cannot create style set");
			return null;
		}
	}
	
	public void setFileSyncEnabled(boolean enabled) {
		if (enabled) {
			activity.enableFileSync();
		} else {
			activity.disableFileSync();
		}
	}
	
	public String attachFile(String filePath, boolean sync, String dir) {
		try {
			File file = new File(filePath);
			if (!file.exists()) {
				showWarning("Logic Error", "Attach file cannot find file.");
				return null;
			}
			
			String attachFile = "";
			
			if (sync) {
				attachFile += activity.getResources().getString(R.string.app_dir);
			} else {
				attachFile += activity.getResources().getString(R.string.server_dir);
			}
			
			if (dir != null && !"".equals(dir)) {
				attachFile += "/" + dir;
			}
			
			// create directories
			FileUtil.makeDirs(baseDir + "/" + attachFile);
			
			// create random file path
			attachFile += "/" + UUID.randomUUID() + "_" + file.getName();
			
			activity.copyFile(filePath, baseDir + "/" + attachFile);
			
			return attachFile;
		} catch (Exception e) {
			FLog.e("error attaching file " + filePath, e);
			return null;
		}
	}

	public void storeBeanShellData(Bundle savedInstanceState) {
		String persistedObjectName = getPersistedObjectName();
		if(persistedObjectName != null){
			try {
				Object persistedObject = interpreter.get(persistedObjectName);
				savedInstanceState.putSerializable(persistedObjectName, (Serializable) persistedObject);
			} catch (EvalError e) {
				FLog.e("error storing bean shell data", e);
			}
		}
	}

	public void restoreBeanShellData(Bundle savedInstanceState){
		if(persistedObjectName != null){
			Object object = savedInstanceState.getSerializable(persistedObjectName);
			try {
				interpreter.set(persistedObjectName, object);
			} catch (EvalError e) {
				FLog.e("error restoring bean shell data", e);
			}
		}
	}
	
	public Geometry createGeometryPoint(MapPos point) {
		return new Point(point, null, createPointStyle(0,0,0), null);
	}
}