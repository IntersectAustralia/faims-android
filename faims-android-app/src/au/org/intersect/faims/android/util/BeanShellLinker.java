package au.org.intersect.faims.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jsqlite.Callback;
import jsqlite.Stmt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import au.org.intersect.faims.android.ui.form.NameValuePair;
import au.org.intersect.faims.android.ui.form.TabGroup;
import bsh.EvalError;
import bsh.Interpreter;

public class BeanShellLinker {
	
	private Interpreter interpreter;

	private UIRenderer renderer;

	private AssetManager assets;
	
	private Activity activity;
	
	public BeanShellLinker(Activity activity, AssetManager assets, UIRenderer renderer) {
		this.activity = activity;
		this.assets = assets;
		this.renderer = renderer;
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
    	}
	}

	public void execute(String code) {
		try {
    		interpreter.eval(code);
    	} catch (EvalError e) {
    		FAIMSLog.log(e); 
    		FAIMSLog.log(code);
    	}
	}
	
	public void bindViewToEvent(String ref, String type, final String code) {
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
						if (value.equalsIgnoreCase(spinner.getItemAtPosition(i).toString())){
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
							if (view instanceof RadioButton){
								RadioButton rb = (RadioButton) view;
								if (rb.getText().toString().equalsIgnoreCase(value)){
									rb.setChecked(true);
									break;
								}
							}
						}
					}
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
							if (view instanceof CheckBox){
								CheckBox cb = (CheckBox) view;
								if (cb.getText().toString().equalsIgnoreCase(pair.getName())){
									cb.setChecked((pair.getValue().equals("true") ? true : false));
									break;
								}
							}
						}
					}
				}
			}
			else {
				Log.w("FAIMS","Couldn't set value for ref= " + ref + " obj= " + obj.toString());
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
				return spinner.getSelectedItem().toString();
			}
			else if (obj instanceof LinearLayout){
				LinearLayout ll = (LinearLayout) obj;
				
				View child0 = ll.getChildAt(0);
				
				if( child0 instanceof CheckBox){
					List<NameValuePair> valueList = new ArrayList<NameValuePair>();
					
					for(int i = 0; i < ll.getChildCount(); ++i){
						View view = ll.getChildAt(i);
						
						if (view instanceof CheckBox){
							CheckBox cb = (CheckBox) view;
							valueList.add(new NameValuePair(cb.getText().toString(), "" + cb.isChecked()));
						}
					}
					return valueList;
				}
				else if (child0 instanceof RadioGroup){
					RadioGroup rg = (RadioGroup) child0;
					String value = "";
					for(int i = 0; i < rg.getChildCount(); ++i){
						View view = rg.getChildAt(i);
						
						if (view instanceof RadioButton){
							RadioButton rb = (RadioButton) view;
							if (rb.isChecked()){
								value = rb.getText().toString();
								break;
							}
						}
					}
					return value;
				}
				else{
					Log.w("FAIMS","Couldn't get value for ref= " + ref + " obj= " + obj.toString());
					return "";
				}
			}
			else {
				Log.w("FAIMS","Couldn't get value for ref= " + ref + " obj= " + obj.toString());
				return "";
			}
		}
		catch(Exception e){
			Log.e("FAIMS","Exception getting field value",e);
			return "";
		}
	}
	
	public void saveArchEnt(String entity_id, String entity_type, String geo_data, List<?> attributes) {
		FAIMSLog.log();
		
		try {
			jsqlite.Database db = new jsqlite.Database();
			/*
			db.open(assets.open("test-2.3.sqlite").toString(), jsqlite.Constants.SQLITE_OPEN_READONLY);
	
			// Callback used to display query results in Android LogCat
			Callback cb = new Callback() {
				@Override
				public void columns(String[] coldata) {
					FAIMSLog.log("Columns: " + Arrays.toString(coldata));
				}
	
				@Override
				public void types(String[] types) {
					FAIMSLog.log("Types: " + Arrays.toString(types));
				}
	
				@Override
				public boolean newrow(String[] rowdata) {
					FAIMSLog.log("Row: " + Arrays.toString(rowdata));
	
					return false;
				}
			};
	
			// Test prepare statements
			String query = "SELECT name, peoples, AsText(Geometry) from Towns where peoples > 350000";
			Stmt st = db.prepare(query);
			st.step();
			st.close();
	
			// Test various queries
			db.exec("select Distance(PointFromText('point(-77.35368 39.04106)', 4326), PointFromText('point(-77.35581 39.01725)', 4326));",
					cb);
			db.exec("SELECT name, peoples, AsText(Geometry), GeometryType(Geometry), NumPoints(Geometry), SRID(Geometry), IsValid(Geometry) from Towns where peoples > 350000;",
					cb);
			db.exec("SELECT Distance( Transform(MakePoint(4.430174797, 51.01047063, 4326), 32631), Transform(MakePoint(4.43001276, 51.01041585, 4326),32631));",
					cb);
	
			// Close the database
			db.close();
			
			*/
		} catch (Exception e) {
			FAIMSLog.log(e);
		}
	}
	
	public void saveRel(String entity_id, String rel_type, String geo_data, List<?> attributes) {
		FAIMSLog.log();
	}
	
	private String convertStreamToString(InputStream stream) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(stream));
		
			StringBuilder sb = new StringBuilder();
		
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
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

	
}
