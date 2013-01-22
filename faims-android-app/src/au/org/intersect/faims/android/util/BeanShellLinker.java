package au.org.intersect.faims.android.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.gps.GPSDataManager;
import au.org.intersect.faims.android.gps.GPSLocation;
import au.org.intersect.faims.android.ui.form.ArchEntity;
import au.org.intersect.faims.android.ui.form.CustomCheckBox;
import au.org.intersect.faims.android.ui.form.CustomDatePicker;
import au.org.intersect.faims.android.ui.form.CustomEditText;
import au.org.intersect.faims.android.ui.form.CustomLinearLayout;
import au.org.intersect.faims.android.ui.form.CustomListView;
import au.org.intersect.faims.android.ui.form.CustomMapView;
import au.org.intersect.faims.android.ui.form.CustomRadioButton;
import au.org.intersect.faims.android.ui.form.CustomSpinner;
import au.org.intersect.faims.android.ui.form.CustomTimePicker;
import au.org.intersect.faims.android.ui.form.EntityAttribute;
import au.org.intersect.faims.android.ui.form.NameValuePair;
import au.org.intersect.faims.android.ui.form.Relationship;
import au.org.intersect.faims.android.ui.form.RelationshipAttribute;
import au.org.intersect.faims.android.ui.form.Tab;
import au.org.intersect.faims.android.ui.form.TabGroup;
import bsh.EvalError;
import bsh.Interpreter;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Marker;
import com.nutiteq.layers.raster.GdalMapLayer;
import com.nutiteq.layers.vector.WKBLayer;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.MarkerStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.utils.UnscaledBitmapLoader;
import com.nutiteq.vectorlayers.MarkerLayer;

public class BeanShellLinker {
	
	private Interpreter interpreter;

	private UIRenderer renderer;

	private AssetManager assets;
	
	private FragmentActivity activity;
	
	private DatabaseManager databaseManager;

	private GPSDataManager gpsDataManager;

	private String baseDir;

	private static final String FREETEXT = "freetext";
	private static final String MEASURE = "measure";
	private static final String CERTAINTY = "certainty";
	private static final String VOCAB = "vocab";
	
	private HandlerThread handlerThread;
	private Handler currentLocationHandler;
	private Runnable currentLocationTask;

	private MarkerLayer currentPositionLayer;
	private GPSLocation previousLocation;

	public BeanShellLinker(FragmentActivity activity, AssetManager assets, UIRenderer renderer, DatabaseManager databaseManager, GPSDataManager gpsDataManager) {
		this.activity = activity;
		this.assets = assets;
		this.renderer = renderer;
		this.databaseManager = databaseManager;
		this.gpsDataManager = gpsDataManager;
		interpreter = new Interpreter();
		try {
			interpreter.set("linker", this);
		} catch (EvalError e) {
			FAIMSLog.log(e);
		}
	}
	
	public void sourceFromAssets(String filename) {
		try {
    		interpreter.eval(convertStreamToString(assets.open(filename)));
    	} catch (EvalError e) {
    		FAIMSLog.log(e); 
    	} catch (IOException e) {
    		FAIMSLog.log(e);
    		showWarning("Logic Error", "Error encountered in logic script");
    	}
	}

	public void execute(String code) {
		try {
    		interpreter.eval(code);
    	} catch (EvalError e) {
    		FAIMSLog.log(e); 
    		//FAIMSLog.log(code);
    		showWarning("Logic Error", "Error encountered in logic script");
    	}
	}
	
	public void bindViewToEvent(String ref, String type, final String code) {
		FAIMSLog.log(ref);
		try{
			
			if (type == "click") {
				View view = renderer.getViewByRef(ref);
				if (view ==  null) {
					Log.e("FAIMS","Can't find view for " + ref);
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
									FAIMSLog.log(e);
								}
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
			else if (type == "load") {
				TabGroup tg = renderer.getTabGroupByLabel(ref);
				if (tg == null){
					Log.e("FAIMS","Could not find TabGroup with label: " + ref);
					return;
				}
				else{
					tg.addOnLoadCommand(code);
				}
			} 
			else if (type == "show") {
				TabGroup tg = renderer.getTabGroupByLabel(ref);
				if (tg == null){
					Log.e("FAIMS","Could not find TabGroup with label: " + ref);
					return;
				}
				else{
					tg.addOnShowCommand(code);
				}
			}
			else {
				FAIMSLog.log("Not implemented");
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception binding event to view",e);
		}
	}

	public void bindFocusAndBlurEvent(String ref, final String focusCallback, final String blurCallBack){
		View view = renderer.getViewByRef(ref);
		if (view ==  null) {
			Log.e("FAIMS","Can't find view for " + ref);
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
	}
	
	public void newTabGroup(String label) {
		try{
			TabGroup tabGroup = showTabGroup(label);
			if (tabGroup == null) return;
			
			tabGroup.clearTabs();
		}
		catch(Exception e){
			Log.e("FAIMS", "Exception showing tab group",e);
		}
	}
	
	public void newTab(String label) {
		try {
			Tab tab = showTab(label);
			if (tab == null) return;
			
			tab.clearViews();
		}
		catch(Exception e) {
			Log.e("FAIMS", "Exception showing tab",e);
		}
	}

	public TabGroup showTabGroup(String label){
		try{
			TabGroup tabGroup = renderer.showTabGroup(this.activity, label);
			if (tabGroup == null) {
				showWarning("Logic Error", "Could not show tab group");
				return null;
			}
			return tabGroup;
		}
		catch(Exception e){
			Log.e("FAIMS", "Exception showing tab group",e);
		}
		return null;
	}
	
	public Tab showTab(String label) {
		try{
			Tab tab = renderer.showTab(label);
			if (tab == null) {
				showWarning("Logic Error", "Could not show tab");
				return null;
			}
			return tab;
		}
		catch(Exception e){
			Log.e("FAIMS", "Exception showing tab",e);
		}
		return null;
	}

	public void showTabGroup(String id, String uuid){
		TabGroup tabGroup = renderer.showTabGroup(activity, id);
		if (tabGroup == null) {
			showWarning("Logic Error", "Could not show tab group");
			return ;
		}
		if(tabGroup.getArchEntType() != null){
			showArchEntityTabGroup(uuid, tabGroup);
		}else if(tabGroup.getRelType() != null){
			showRelationshipTabGroup(uuid, tabGroup);
		}else{
			showTabGroup(id);
		}
	}
	
	public void showTab(String id, String uuid) {
		if (id == null) {
			showWarning("Logic Error", "Could not show tab");
			return ;
		}
		String[] ids = id.split("/");
		if (ids.length < 2) {
			showWarning("Logic Error", "Could not show tab");
			return ;
		}
		String groupId = ids[0];
		String tabId = ids[1];
		TabGroup tabGroup = renderer.getTabGroupByLabel(groupId);
		if (tabGroup == null) {
			showWarning("Logic Error", "Could not show tab");
			return ;
		}
		Tab tab = tabGroup.showTab(tabId);
		if (tab == null) {
			showWarning("Logic Error", "Could not show tab");
			return ;
		}
		if(tabGroup.getArchEntType() != null){
			showArchEntityTab(uuid, tab);
		}else if(tabGroup.getRelType() != null){
			showRelationshipTab(uuid, tab);
		}else{
			showTab(id);
		}
	}

	public void goBack(){
		this.activity.onBackPressed();
	}

	public int getGpsUpdateInterval(){
		return this.gpsDataManager.getGpsUpdateInterval();
	}

	public void setGpsUpdateInterval(int gpsUpdateInterval) {
		destroyListener();
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
			showWarning("Logic Error", "Could not fetch arch entity");
			return ;
		}
		if(archEntityObj instanceof ArchEntity){
			ArchEntity archEntity = (ArchEntity) archEntityObj;
			try {
				for(Tab tab : tabGroup.getTabs()){
			    	for (EntityAttribute entityAttribute : archEntity.getAttributes()) {
			    		List<View> views = tab.getViews(entityAttribute.getName());
			    		if (views != null)
			    			clearCheckboxAndRadioButtonValues(views);
			    	}
			    }
				for(Tab tab : tabGroup.getTabs()){
					for (EntityAttribute entityAttribute : archEntity.getAttributes()) {
			    		if(tab.hasView(entityAttribute.getName())){
			    			List<View> views = tab.getViews(entityAttribute.getName());
			    			if (views != null)
			    				loadArchEntFieldsValue(entityAttribute, views);
			    		}
			    	}
			    }
			} catch (Exception e) {
				Log.e("FAIMS", "Exception showing tab group and load value",e);
			}
		}
	}

	private void showRelationshipTabGroup(String uuid, TabGroup tabGroup) {
		Object relationshipObj = fetchRel(uuid);
		if (relationshipObj == null) {
			showWarning("Logic Error", "Could not fetch relationship");
			return ;
		}
		if(relationshipObj instanceof Relationship){
			Relationship relationship = (Relationship) relationshipObj;
			try {
				for(Tab tab : tabGroup.getTabs()){
			    	for (RelationshipAttribute relationshipAttribute : relationship.getAttributes()) {
			    		List<View> views = tab.getViews(relationshipAttribute.getName());
			    		if (views != null)
			    			clearCheckboxAndRadioButtonValues(views);
			    	}
			    }
				for(Tab tab : tabGroup.getTabs()){
					for (RelationshipAttribute relationshipAttribute : relationship.getAttributes()) {
			    		if(tab.hasView(relationshipAttribute.getName())){
			    			List<View> views = tab.getViews(relationshipAttribute.getName());
			    			if (views != null)
			    				loadRelationshipFieldsValue(relationshipAttribute, views);
			    		}
			    	}
			    }
			} catch (Exception e) {
				Log.e("FAIMS", "Exception showing tab group and load value",e);
			}
		}
	}
	
	private void showArchEntityTab(String uuid, Tab tab) {
		Object archEntityObj = fetchArchEnt(uuid);
		if (archEntityObj == null) {
			showWarning("Logic Error", "Could not fetch arch entity");
			return ;
		}
		if(archEntityObj instanceof ArchEntity){
			ArchEntity archEntity = (ArchEntity) archEntityObj;
			try {
				Collection<EntityAttribute> attributes = archEntity.getAttributes();
				for (EntityAttribute entityAttribute : attributes) {
					List<View> views = tab.getViews(entityAttribute.getName());
					if (views != null)
						clearCheckboxAndRadioButtonValues(views);
			    }
			    for (EntityAttribute entityAttribute : attributes) {
			    	if(tab.hasView(entityAttribute.getName())){
			    		List<View> views = tab.getViews(entityAttribute.getName());
			    		if (views != null)
			    			loadArchEntFieldsValue(entityAttribute, views);
			    	}
			    }
			} catch (Exception e) {
				Log.e("FAIMS", "Exception showing tab and load value",e);
			}
		}
	}

	private void showRelationshipTab(String uuid, Tab tab) {
		Object relationshipObj = fetchRel(uuid);
		if (relationshipObj == null) {
			showWarning("Logic Error", "Could not fetch relationship");
			return ;
		}
		if(relationshipObj instanceof Relationship){
			Relationship relationship = (Relationship) relationshipObj;
			try {
				Collection<RelationshipAttribute> attributes = relationship.getAttributes();
				for (RelationshipAttribute relationshipAttribute : attributes) {
			    	List<View> views = tab.getViews(relationshipAttribute.getName());
			    	if (views != null)
			    		clearCheckboxAndRadioButtonValues(views);
			    }
			    for (RelationshipAttribute relationshipAttribute : attributes) {
			    	if(tab.hasView(relationshipAttribute.getName())){
			    		List<View> views = tab.getViews(relationshipAttribute.getName());
			    		if (views != null)
			    			loadRelationshipFieldsValue(relationshipAttribute, views);
			    	}
			    }
			} catch (Exception e) {
				Log.e("FAIMS", "Exception showing tab and load value",e);
			}
		}
	}

	private void clearCheckboxAndRadioButtonValues(List<View> views) {
		for (View v : views) {
			if(v instanceof CustomLinearLayout){
				CustomLinearLayout customLinearLayout = (CustomLinearLayout) v;
				if(customLinearLayout.getChildAt(0) instanceof CustomCheckBox){
					for(int i = 0; i < customLinearLayout.getChildCount(); ++i){
						View view = customLinearLayout.getChildAt(i);
						if (view instanceof CustomCheckBox){
							CustomCheckBox cb = (CustomCheckBox) view;
							cb.setChecked(false);
						}
					}
				}else if (customLinearLayout.getChildAt(0) instanceof RadioGroup){
					RadioGroup rg = (RadioGroup) customLinearLayout.getChildAt(0);
					for(int i = 0; i < rg.getChildCount(); ++i){
						View view = rg.getChildAt(i);
						if (view instanceof CustomRadioButton){
							CustomRadioButton rb = (CustomRadioButton) view;
							rb.setChecked(false);
						}
					}
				}
			}
		}
	}

	private void loadArchEntFieldsValue(EntityAttribute entityAttribute, List<View> views) {
		for (View v : views) {
			if (v instanceof CustomEditText) {
				CustomEditText customEditText = (CustomEditText) v;
				setArchEntityFieldValueForType(customEditText.getAttributeType(), customEditText.getRef(), entityAttribute);
				
			} else if (v instanceof CustomDatePicker) {
				CustomDatePicker customDatePicker = (CustomDatePicker) v;
				setArchEntityFieldValueForType(customDatePicker.getAttributeType(), customDatePicker.getRef(), entityAttribute);
				
			} else if (v instanceof CustomTimePicker) {
				CustomTimePicker customTimePicker = (CustomTimePicker) v;
				setArchEntityFieldValueForType(customTimePicker.getAttributeType(), customTimePicker.getRef(), entityAttribute);
				
			} else if (v instanceof CustomLinearLayout) {
				CustomLinearLayout customLinearLayout = (CustomLinearLayout) v;
				setArchEntityFieldValueForType(customLinearLayout.getAttributeType(), customLinearLayout.getRef(), entityAttribute);
				
			} else if (v instanceof CustomSpinner) {
				CustomSpinner customSpinner = (CustomSpinner) v;
				setArchEntityFieldValueForType(customSpinner.getAttributeType(), customSpinner.getRef(), entityAttribute);
			}
		}
	}

	private void loadRelationshipFieldsValue(RelationshipAttribute relationshipAttribute, List<View> views) {
		for (View v : views) {
			if (v instanceof CustomEditText) {
				CustomEditText customEditText = (CustomEditText) v;
				setRelationshipFieldValueForType(customEditText.getAttributeType(), customEditText.getRef(), relationshipAttribute);
				
			} else if (v instanceof CustomDatePicker) {
				CustomDatePicker customDatePicker = (CustomDatePicker) v;
				setRelationshipFieldValueForType(customDatePicker.getAttributeType(), customDatePicker.getRef(), relationshipAttribute);
				
			} else if (v instanceof CustomTimePicker) {
				CustomTimePicker customTimePicker = (CustomTimePicker) v;
				setRelationshipFieldValueForType(customTimePicker.getAttributeType(), customTimePicker.getRef(), relationshipAttribute);
				
			} else if (v instanceof CustomLinearLayout) {
				CustomLinearLayout customLinearLayout = (CustomLinearLayout) v;
				setRelationshipFieldValueForType(customLinearLayout.getAttributeType(), customLinearLayout.getRef(), relationshipAttribute);
				
			} else if (v instanceof CustomSpinner) {
				CustomSpinner customSpinner = (CustomSpinner) v;
				setRelationshipFieldValueForType(customSpinner.getAttributeType(), customSpinner.getRef(), relationshipAttribute);
			}
		}
	}

	private void setArchEntityFieldValueForType(String type, String ref, EntityAttribute attribute){
		if(FREETEXT.equals(type)){
			setFieldValue(ref,attribute.getText());
		}else if(MEASURE.equals(type)){
			setFieldValue(ref,attribute.getMeasure());
		}else if(VOCAB.equals(type)){
			setFieldValue(ref,attribute.getVocab());
		}else if(CERTAINTY.equals(type)){
			setFieldValue(ref,attribute.getCertainty());
		}
	}
	
	private void setRelationshipFieldValueForType(String type, String ref, RelationshipAttribute relationshipAttribute){
		if(FREETEXT.equals(type)){
			setFieldValue(ref,relationshipAttribute.getText());
		}else if(VOCAB.equals(type)){
			setFieldValue(ref,relationshipAttribute.getVocab());
		}
	}

	public void showToast(String message){
		try {
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(activity.getApplicationContext(), message, duration);
			toast.show();
		}
		catch(Exception e){
			Log.e("FAIMS","Exception showing toast message",e);
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
			
			if (valueObj instanceof String){
				
				String value = (String) valueObj;
				
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
						for(int i = 0; i < rg.getChildCount(); ++i){
							View view = rg.getChildAt(i);
							if (view instanceof CustomRadioButton){
								CustomRadioButton rb = (CustomRadioButton) view;
								if (rb.getValue().toString().equalsIgnoreCase(value)){
									rb.setChecked(true);
									break;
								}
							}
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
					}
				}
				else if (obj instanceof DatePicker) {
					DatePicker date = (DatePicker) obj;
					DateUtil.setDatePicker(date, value);
				} 
				else if (obj instanceof TimePicker) {
					TimePicker time = (TimePicker) obj;
					DateUtil.setTimePicker(time, value);
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
								if (cb.getValue().toString().equalsIgnoreCase(pair.getName())){
									cb.setChecked("true".equals(pair.getValue()));
									break;
								}
							}
						}
					}
				}
			}
			else {
				Log.w("FAIMS","Couldn't set value for ref= " + ref + " obj= " + obj.toString());
				showWarning("Logic Error", "View does not exist.");
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception setting field value",e);
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
					Log.w("FAIMS","Couldn't get value for ref= " + ref + " obj= " + obj.toString());
					showWarning("Logic Error", "View does not exist.");
					return "";
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
			else {
				Log.w("FAIMS","Couldn't get value for ref= " + ref + " obj= " + obj.toString());
				showWarning("Logic Error", "View does not exist.");
				return "";
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception getting field value",e);
			return "";
		}
	}
	
	public String saveArchEnt(String entity_id, String entity_type, String geo_data, List<EntityAttribute> attributes) {
		FAIMSLog.log();
		
		entity_id = databaseManager.saveArchEnt(entity_id, entity_type, geo_data, attributes);
		if (entity_id == null) {
			showWarning("Database Error", "Could not save arch entity.");
		}
		
		return entity_id;
	}
	
	public String saveRel(String rel_id, String rel_type, String geo_data, List<RelationshipAttribute> attributes) {
		FAIMSLog.log();
		
		rel_id = databaseManager.saveRel(rel_id, rel_type, geo_data, attributes);
		if (rel_id == null) {
			showWarning("Database Error", "Could not save relationship.");
		}
		
		return rel_id;
	}
	
	public void addReln(String entity_id, String rel_id, String verb) {
		FAIMSLog.log();
		
		if (!databaseManager.addReln(entity_id, rel_id, verb)) {
			showWarning("Database Error", "Could not save entity relationship.");
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void populateDropDown(String ref, Collection valuesObj){
		
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
					pairs.add(new NameValuePair(list.get(1), list.get(0)));
				}
			}
			
			ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
                    this.activity,
                    android.R.layout.simple_spinner_dropdown_item,
                    pairs);
            spinner.setAdapter(arrayAdapter);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void populateList(String ref, Collection valuesObj){
		
		Object obj = renderer.getViewByRef(ref);

		if (obj instanceof LinearLayout && valuesObj instanceof ArrayList){
			LinearLayout ll = (LinearLayout) obj;
			
			View child0 = ll.getChildAt(0);
			
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
					pairs.add(new NameValuePair(list.get(1), list.get(0)));
				}
			}
			
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
				
				int rbId = 0;
				for (NameValuePair pair : pairs) {
					CustomRadioButton radioButton = new CustomRadioButton(ll.getContext());
                    radioButton.setId(rbId++);
                    radioButton.setText(pair.getName());
                    radioButton.setValue(pair.getValue());
                    rg.addView(radioButton);
				}
			}
		}
	}
	
	public Object fetchArchEnt(String id){
		return databaseManager.fetchArchEnt(id);
	}

	public Object fetchRel(String id){
		return databaseManager.fetchRel(id);
	}

	public Object fetchOne(String query){
		return databaseManager.fetchOne(query);
	}

	@SuppressWarnings("rawtypes")
	public Collection fetchAll(String query){
		return databaseManager.fetchAll(query);
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
					Log.d("FAIMS","Map file " + filepath + " does not exist");
                    showWarning("Map Error", "Could not render map.");
					return;
				}
				
        		final GdalMapLayer gdalLayer;
                try {
                    gdalLayer = new GdalMapLayer(new EPSG3857(), 0, 18, CustomMapView.nextId(), filepath, mapView, true);
                    gdalLayer.setShowAlways(true);
                    mapView.getLayers().setBaseLayer(gdalLayer);
                    if(this.handlerThread == null){
                    	this.handlerThread = new HandlerThread("MapHandler");
                		this.handlerThread.start();
                    }
                    if(this.currentLocationHandler == null){
                    	this.currentLocationHandler = new Handler(this.handlerThread.getLooper());
                    }
                    if(this.currentLocationTask == null){
	                    this.currentLocationTask = new Runnable() {
							
							@Override
							public void run() {
								Object currentLocation = getGPSPosition();
								if(currentLocation != null){
									System.out.println("get location");
									GPSLocation location = (GPSLocation) currentLocation;
									previousLocation = location;
									Bitmap pointMarker = UnscaledBitmapLoader.decodeResource(
											activity.getResources(), R.drawable.blue_dot);
				                    MarkerStyle markerStyle = MarkerStyle.builder().setBitmap(pointMarker)
				                            .setSize(0.5f).build();
				                    MapPos markerLocation = gdalLayer.getProjection().fromWgs84(
				                            location.getLongitude(), location.getLatitude());
				                    if(currentPositionLayer != null){
				                    	mapView.getLayers().removeLayer(currentPositionLayer);
				                    }
				                    currentPositionLayer = new MarkerLayer(gdalLayer.getProjection());
				                    currentPositionLayer.add(new Marker(markerLocation, null, markerStyle, null));
				                    mapView.getLayers().addLayer(currentPositionLayer);
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
					                    	if(currentPositionLayer != null){
						                    	mapView.getLayers().removeLayer(currentPositionLayer);
						                    }
					                    	currentPositionLayer = new MarkerLayer(gdalLayer.getProjection());
						                    currentPositionLayer.add(new Marker(markerLocation, null, markerStyle, null));
						                    mapView.getLayers().addLayer(currentPositionLayer);
											previousLocation = null;
										}
									}
								}
								currentLocationHandler.postDelayed(this, getGpsUpdateInterval());
							}
						};
                    }
                    this.currentLocationHandler.postDelayed(currentLocationTask, getGpsUpdateInterval());
                } catch (IOException e) {
                	Log.e("FAIMS","Could not render raster layer",e);
                    showWarning("Map Error", "Could not render map.");
                }
                
			} else {
				Log.d("FAIMS","Could not find map view");
				showWarning("Logic Error", "Map does not exist.");
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception showing raster map",e);
		}
	}
	
	public void setMapFocusPoint(String ref, float latitude, float longitude) {
		
		if (latitude < -90.0f || latitude > 90.0f) {
			Log.d("FAIMS", "Latitude out of range " + latitude);
			showWarning("Logic Error", "Map data out of range.");
		}
		
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				mapView.setFocusPoint(new EPSG3857().fromWgs84(longitude, latitude));
			} else {
				Log.d("FAIMS","Could not find map view");
				showWarning("Logic Error", "Map does not exist.");
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception setting map focus point",e);
			showWarning("Logic Error", "Map is malformed");
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
				Log.d("FAIMS","Could not find map view");
				showWarning("Logic Error", "Map does not exist.");
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception setting map rotation",e);
			showWarning("Logic Error", "Map is malformed");
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
				Log.d("FAIMS","Could not find map view");
				showWarning("Logic Error", "Map does not exist.");
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception setting map zoom",e);
			showWarning("Logic Error", "Map is malformed");
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
				Log.d("FAIMS","Could not find map view");
				showWarning("Logic Error", "Map does not exist.");
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception setting map tilt",e);
			showWarning("Logic Error", "Map is malformed");
		}
	}
	
	public int showVectorLayer(String ref, String filename) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				int minZoom = 4;
				
				StyleSet<PointStyle> pointStyleSet = new StyleSet<PointStyle>();
		        Bitmap pointMarker = UnscaledBitmapLoader.decodeResource(activity.getResources(), R.drawable.point);
		        PointStyle pointStyle = PointStyle.builder().setBitmap(pointMarker).setSize(0.05f).setColor(Color.BLACK).build();
				pointStyleSet.setZoomStyle(minZoom, pointStyle);

				StyleSet<LineStyle> lineStyleSet = new StyleSet<LineStyle>();
		        lineStyleSet.setZoomStyle(minZoom, LineStyle.builder().setWidth(0.1f).setColor(Color.GREEN).build());
		        
		        PolygonStyle polygonStyle = PolygonStyle.builder().setColor(Color.BLUE).build();
		        StyleSet<PolygonStyle> polygonStyleSet = new StyleSet<PolygonStyle>(null);
				polygonStyleSet.setZoomStyle(minZoom, polygonStyle);
				
				int id = 0;
				try {
					WKBLayer layer = new WKBLayer(new EPSG3857(), baseDir + "/maps/" + filename,
							pointStyleSet, lineStyleSet, polygonStyleSet);
					id = mapView.addVectorLayer(layer);
				} catch (Exception e) {
					Log.e("FAIMS","Could not show vector layer", e);
                    showWarning("Map Error", "Could not show vector layer");
					return 0;
				}
				
				return id;
			} else {
				Log.d("FAIMS","Could not find map view");
				showWarning("Logic Error", "Map does not exist.");
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception showing vector layer",e);
		}
		return 0;
	}
	
	public void clearVectorLayer(String ref, int id) {
		try{
			Object obj = renderer.getViewByRef(ref);
			if (obj instanceof CustomMapView) {
				CustomMapView mapView = (CustomMapView) obj;
				
				try {
					mapView.removeVectorLayer(id);
				} catch (Exception e) {
					Log.e("FAIMS","Could not clear vector layer ", e);
                    showWarning("Map Error", "Could not clear vector layer");
				}
				
			} else {
				Log.d("FAIMS","Could not find map view");
				showWarning("Logic Error", "Map does not exist.");
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception showing vector layer",e);
		}
	}

	private String convertStreamToString(InputStream stream) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(stream));
		
			StringBuilder sb = new StringBuilder();
		
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			} 
	
			return sb.toString();
		} catch (IOException e) {
			FAIMSLog.log(e);
		} finally {
			try {
				if (br != null) br.close();
			} catch (IOException e) {
				FAIMSLog.log(e);
			}
		}
		return null;
	}

	public UIRenderer getUIRenderer(){
		return this.renderer;
	}

	public void setBaseDir(String dir) {
		this.baseDir = dir;
	}
}
