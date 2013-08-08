package au.org.intersect.faims.android.ui.map;

import java.util.HashMap;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import au.org.intersect.faims.android.exceptions.MapException;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.dialog.ColorPickerDialog;
import au.org.intersect.faims.android.ui.dialog.ErrorDialog;

public class ConfigDialog extends AlertDialog {

	private LinearLayout layout;
	private HashMap<String, View> fields = new HashMap<String, View>();
	
	protected ConfigDialog(Context context) {
		super(context);
		setTitle("Config Dialog");
	}

	public void init(final CustomMapView mapView){
		ScrollView scrollView = new ScrollView(getContext());
		layout = new LinearLayout(getContext());
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		addColorField("color", "Select Tool Color:", Integer.toHexString(mapView.getDrawViewColor()));
		addColorField("editColor", "Edit Tool Color:", Integer.toHexString(mapView.getEditViewColor()));
		addSlider("strokeSize", "Stroke Size:", mapView.getDrawViewStrokeStyle());
		addSlider("textSize", "Details Text Size:", mapView.getDrawViewTextSize());
		
		final boolean isEPSG4326 = GeometryUtil.EPSG4326.equals(mapView.getActivity().getProject().getSrid());
		if (isEPSG4326) {
			addCheckBox("showDegrees", "Show Degrees:", !mapView.showDecimal());
		}
		addCheckBox("showKm", "Display measurements in km:", mapView.showKm());
		addSlider("vertexSize", "Guide Point Size:", mapView.getVertexSize());
		addTextField("buffer", "Tracker Tool Buffer Size (m):", Float.toString(mapView.getPathBuffer()));
		addColorField("bufferColor", "Tracker Tool Buffer Color:", Integer.toHexString(mapView.getBufferColor()));
		addColorField("targetColor", "Tracker Tool Target Color:", Integer.toHexString(mapView.getTargetColor()));

		setView(scrollView);
		setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try{
					int color = parseColor("color");
					int editColor = parseColor("editColor");
					float strokeSize = parseSlider("strokeSize");
					float textSize = parseSlider("textSize");
					boolean showDecimal;
					if (isEPSG4326){
						showDecimal = !parseCheckBox("showDegrees");
					}else{
						showDecimal = true;
					}
					float vertexSize = parseSlider("vertexSize");
					boolean showKm = parseCheckBox("showKm");

					float buffer = Float.parseFloat(((EditText)getField("buffer")).getText().toString());
					int bufferColor = parseColor("bufferColor");
					int targetColor = parseColor("targetColor");

					mapView.setDrawViewColor(color);
					mapView.setEditViewColor(editColor);
					mapView.setDrawViewStrokeStyle(strokeSize);
					mapView.setDrawViewTextSize(textSize);
					mapView.setEditViewTextSize(textSize);
					mapView.setShowDecimal(showDecimal);
					mapView.setVertexSize(vertexSize);
					mapView.setShowKm(showKm);
					mapView.setPathBuffer(buffer);
					
					mapView.setBufferColor(bufferColor);
					mapView.setTargetColor(targetColor);
					
					if(mapView.getCurrentTool() != null){
						mapView.getCurrentTool().onConfigChanged();
						mapView.getCurrentTool().onMapChanged();
					}
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
					new ErrorDialog(getContext(), "Config Dialog Error", e.getMessage()).show();
				}
			}
		});
		
		setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// Do nothing
			}
			
		});
	}

	public void addTextField(String name, String label, String defaultValue) {
		addEditField(name, label, defaultValue, InputType.TYPE_CLASS_TEXT);
	}
	
	public void addColorField(String name, String label, String defaultValue) {
		TextView labelView = new TextView(getContext());
		labelView.setText(label);
		
		final EditText text = new EditText(getContext());
		text.setText(defaultValue.toUpperCase(Locale.ENGLISH));
		text.setInputType(EditorInfo.TYPE_NULL);
		text.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View arg0, MotionEvent action) {
				if(action.getAction() == MotionEvent.ACTION_UP){
					ColorPickerDialog colorPickerDialog = new ColorPickerDialog(getContext(), new ColorPickerDialog.OnColorChangedListener() {
						
						@Override
						public void colorChanged(int color) {
							text.setText(convertText(Integer.toHexString(color)));
							text.setBackgroundColor(Color.parseColor("#"+convertText(Integer.toHexString(color))));
						}

					}, Color.parseColor("#"+text.getText().toString()));
					colorPickerDialog.show();
				}
				return false;
			}
		});
		text.setBackgroundColor(Color.parseColor("#"+text.getText().toString()));
		
		layout.addView(labelView);
		layout.addView(text);
		
		fields.put(name, text);
		
	}
	
	private String convertText(String color) {
		return color.toUpperCase(Locale.ENGLISH);
	}

	public void addNumberField(String name, String label, String defaultValue) {
		addEditField(name, label, defaultValue, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
	}
	
	public void addEditField(String name, String label, String defaultValue, int type) {
		TextView labelView = new TextView(getContext());
		labelView.setText(label);
		
		EditText text = new EditText(getContext());
		text.setText(defaultValue.toUpperCase(Locale.ENGLISH));
		
		text.setInputType(type);
		
		layout.addView(labelView);
		layout.addView(text);
		
		fields.put(name, text);
	}
	
	public void addCheckBox(String name, String label, boolean defaultValue) {
		TextView labelView = new TextView(getContext());
		labelView.setText(label);
		
		CheckBox box = new CheckBox(getContext());
		box.setChecked(defaultValue);
		
		layout.addView(labelView);
		layout.addView(box);
		
		fields.put(name, box);
	}
	
	public void addSlider(String name, final String label, float defaultValue) {
		addSlider(name, label, defaultValue, 0, 1);
	}
	
	public void addSlider(String name, final String label, float defaultValue, final float minRange, final float maxRange) {
		final TextView labelView = new TextView(getContext());
		labelView.setText(label + " " + Float.toString(defaultValue));
		
		final SeekBar seekBar = new SeekBar(getContext());
		seekBar.setMax(100);
		seekBar.setProgress((int) (100 * (defaultValue / (maxRange - minRange))));
		
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				labelView.setText(label + " " + Float.toString(getSliderValue(seekBar.getProgress(), minRange, maxRange)));
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		layout.addView(labelView);
		layout.addView(seekBar);
		
		fields.put(name, seekBar);
	}
	
	public void addRange(String name, final String label, int defaultValue, final int minRange, final int maxRange) {
		final TextView labelView = new TextView(getContext());
		labelView.setText(label + " " + Integer.toString(defaultValue));
		
		final SeekBar seekBar = new SeekBar(getContext());
		seekBar.setMax(maxRange - minRange);
		seekBar.setProgress(defaultValue - minRange);
		
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				labelView.setText(label + " " + Integer.toString(getRangeValue(seekBar.getProgress(), minRange, maxRange)));
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		layout.addView(labelView);
		layout.addView(seekBar);
		
		fields.put(name, seekBar);
	}
	
	public View getField(String name) {
		return fields.get(name);
	}

	public int parseColor(String name) throws Exception {
		EditText text = (EditText) getField(name);
		if (text == null) {
			throw new MapException("Cannot find setting " + name);
		}
		
		Integer color = (int) Long.parseLong(text.getText().toString(), 16);
		if (color == null) {
			throw new MapException("Invalid color specified for " + name);
		}
		return color;
	}
	
	public float parseSlider(String name) throws Exception {
		return parseSlider(name, 0, 1);
	}
	
	public float parseSlider(String name, float minRange, float maxRange) throws Exception {
		SeekBar slider = (SeekBar) getField(name);
		if (slider == null) {
			throw new MapException("Cannot find setting " + name);
		}
		return getSliderValue(slider.getProgress(), minRange, maxRange);
	}
	
	public int parseRange(String name, int minRange, int maxRange) throws Exception {
		SeekBar slider = (SeekBar) getField(name);
		if (slider == null) {
			throw new MapException("Cannot find setting " + name);
		}
		return getRangeValue(slider.getProgress(), minRange, maxRange);
	}
	
	public boolean parseCheckBox(String name) throws Exception {
		CheckBox checkBox = (CheckBox) getField(name);
		if (checkBox == null) {
			throw new MapException("Cannot find setting " + name);
		}
		return checkBox.isChecked();
	}
	
	protected static float getSliderValue(int progress, float minRange, float maxRange) {
		return (((float) progress) / 100) * (maxRange - minRange);
	}
	
	protected static int getRangeValue(int progress, int minRange, int maxRange) {
		return progress + minRange;
	}
}
