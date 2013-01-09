package au.org.intersect.faims.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import au.org.intersect.faims.android.ui.form.CustomCheckBox;
import au.org.intersect.faims.android.ui.form.CustomRadioButton;
import au.org.intersect.faims.android.ui.form.EntityAttribute;
import au.org.intersect.faims.android.ui.form.NameValuePair;
import au.org.intersect.faims.android.ui.form.RelationshipAttribute;
import au.org.intersect.faims.android.ui.form.Tab;
import au.org.intersect.faims.android.ui.form.TabGroup;
import bsh.EvalError;
import bsh.Interpreter;

public class BeanShellLinker {
	
	private Interpreter interpreter;

	private UIRenderer renderer;

	private AssetManager assets;
	
	private FragmentActivity activity;
	
	private DatabaseManager databaseManager;

	public BeanShellLinker(FragmentActivity activity, AssetManager assets, UIRenderer renderer, DatabaseManager databaseManager) {
		this.activity = activity;
		this.assets = assets;
		this.renderer = renderer;
		this.databaseManager = databaseManager;
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
					view.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							execute(code);
						}
					});
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

	public void showTabGroup(String label){
		try{
			renderer.showTabGroup(this.activity, label);
		}
		catch(Exception e){
			Log.e("FAIMS", "Exception showing tab group",e);
		}
	}

	public void showTabGroup(String id, String uuid){
		Object archEntities = fetchArchEnt(uuid);
		if(archEntities instanceof Collection<?>){
			@SuppressWarnings("unchecked")
			List<EntityAttribute> entityAttributes = (List<EntityAttribute>) archEntities;
			try {
				TabGroup tabGroup = renderer.showTabGroup(activity, id);
				for (EntityAttribute entityAttribute : entityAttributes) {
			    	for(Tab tab : tabGroup.getTabs()){
			    		if(tab.hasView(entityAttribute.getName()) || tab.hasView(entityAttribute.getName() + "-freetext") || tab.hasView(entityAttribute.getName() + "-measure")
			    				|| tab.hasView(entityAttribute.getName() + "-certainty") || tab.hasView(entityAttribute.getName() + "-vocab")){
			    			if(entityAttribute.hasFreeText()){
			    				if(tab.getPath(entityAttribute.getName() + "-freetext") != null){
			    					setFieldValue(tab.getPath(entityAttribute.getName() + "-freetext"),entityAttribute.getText());
			    				}else{
			    					setFieldValue(tab.getPath(entityAttribute.getName()),entityAttribute.getText());
			    				}
			    			}
			    			if(entityAttribute.hasMeasure()){
			    				setFieldValue(tab.getPath(entityAttribute.getName() + "-measure"),entityAttribute.getMeasure());
			    			}
			    			if(entityAttribute.hasFreeText()){
			    				setFieldValue(tab.getPath(entityAttribute.getName() + "-certainty"),entityAttribute.getCertainty());
			    			}
			    			if(entityAttribute.hasFreeText()){
			    				setFieldValue(tab.getPath(entityAttribute.getName() + "-vocab"),entityAttribute.getVocab());
			    			}
			    		}
			    	}
			    }
			} catch (Exception e) {
				Log.e("FAIMS", "Exception showing tab group and load value",e);
			}
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
								if (cb.getValue().toString().equalsIgnoreCase(pair.getValue())){
									cb.setChecked(true);
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
								valueList.add(new NameValuePair(cb.getText().toString(), cb.getValue()));
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
}
