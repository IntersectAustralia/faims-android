package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import au.org.intersect.faims.android.exceptions.MapException;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.form.MapText;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.layers.Layer;

public abstract class BaseGeometryTool extends MapTool {
	
	protected LinearLayout layout;

	protected MapText selectedLayer;

	protected MapButton selectLayerButton;
	
	protected MapButton settingsButton;

	public BaseGeometryTool(Context context, CustomMapView mapView, String name) {
		super(context, mapView, name);
		
		layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		selectedLayer = new MapText(context);
		selectedLayer.setBackgroundColor(Color.WHITE);
		
		selectLayerButton = createSelectLayerButton(context);
		settingsButton = createSettingsButton(context);
		
		updateLayout();
	}
	
	protected void updateLayout() {
		layout.removeAllViews();
		layout.addView(settingsButton);
		layout.addView(selectLayerButton);
		layout.addView(selectedLayer);
	}
	
	@Override
	public void activate() {
		mapView.setSelectedGeometry(null);
		
		setSelectedLayer(mapView.getSelectedLayer());
	}
	
	@Override
	public void deactivate() {
		mapView.setSelectedGeometry(null);
	}

	@Override
	public View getUI() {
		return layout;
	}

	protected int parseColor(String value) throws Exception {
		Integer color = (int) Long.parseLong(value, 16);
		if (color == null) {
			throw new MapException("Invalid color specified");
		}
		return color;
	}
	
	protected float parseSize(int value) throws Exception {
		if (value < 0 || value > 100) {
			throw new MapException("Invalid size");
		}
		
		return ((float) value) / 100;
	}
	
	protected EditText addSetter(Context context, LinearLayout layout, String labelText, String defaultValue) {
		return addSetter(context, layout, labelText, defaultValue, -1);
	}
	
	protected EditText addSetter(Context context, LinearLayout layout, String labelText, String defaultValue, int type) {
		TextView label = new TextView(context);
		label.setText(labelText);
		
		EditText text = new EditText(context);
		text.setText(defaultValue.toUpperCase(Locale.ENGLISH));
		
		if (type >= 0) text.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		
		layout.addView(label);
		layout.addView(text);
		
		return text;
	}
	
	protected SeekBar addSlider(Context context, LinearLayout layout, final String labelText, float defaultValue) {
		final TextView label = new TextView(context);
		label.setText(labelText + " " + Float.toString(defaultValue));
		
		final SeekBar seekBar = new SeekBar(context);
		seekBar.setMax(100);
		seekBar.setProgress((int) (defaultValue * 100));
		
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				label.setText(labelText + " " + Float.toString(((float) seekBar.getProgress()) / 100));
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
		
		layout.addView(label);
		layout.addView(seekBar);
		
		return seekBar;
	}
	
	protected MapButton createSelectLayerButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Select Layer");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				List<Layer> layers = BaseGeometryTool.this.mapView.getLayers().getAllLayers();
				
				final ArrayList<Layer> filteredLayers = new ArrayList<Layer>();
				ArrayList<String> layerNames = new ArrayList<String>();
				for (Layer layer : layers) {
					if (layer instanceof CanvasLayer) {
						filteredLayers.add(layer);
						layerNames.add(BaseGeometryTool.this.mapView.getLayerName(layer));
					}
				}
				
				if (filteredLayers.isEmpty()) {
					showError(context, "No canvas layers found");
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setTitle("Select Layer");
				
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(BaseGeometryTool.this.context, android.R.layout.simple_list_item_1, layerNames);
					
					ListView listView = new ListView(context);
					listView.setAdapter(adapter);
					
					builder.setView(listView);
					
					final Dialog d = builder.create();
					
					listView.setOnItemClickListener(new OnItemClickListener() {
	
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1,
								int position, long arg3) {
							d.dismiss();
							setSelectedLayer(filteredLayers.get(position));
						}
	
					});
					
					d.show();
				}
			}
				
		});
		
		return button;
	}
	
	protected void setSelectedLayer(Layer layer) {
		mapView.setSelectedLayer(layer);
		
		if (layer == null) {
			selectedLayer.setText("No layer selected");
		} else {
			selectedLayer.setText("Current Layer: " + mapView.getLayerName(layer));
		}
	}
	
	protected abstract MapButton createSettingsButton(final Context context);

}
