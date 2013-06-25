package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.form.MapText;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.layers.Layer;

public abstract class BaseGeometryTool extends SettingsTool {
	
	protected MapText selectedLayer;

	protected MapButton selectLayerButton;

	protected Layer lastLayerSelected;

	public BaseGeometryTool(Context context, CustomMapView mapView, String name) {
		super(context, mapView, name);
		
		selectedLayer = new MapText(context);
		selectedLayer.setBackgroundColor(Color.WHITE);
		
		selectLayerButton = createSelectLayerButton(context);
		
		updateLayout();
	}
	
	protected void updateLayout() {
		super.updateLayout();
		if (selectLayerButton != null) layout.addView(selectLayerButton);
		if (selectedLayer != null) layout.addView(selectedLayer);
	}
	
	@Override
	public void activate() {		
		setSelectedLayer(mapView.getSelectedLayer());
	}
	
	@Override
	public void deactivate() {
	}
	
	@Override 
	public void onLayersChanged() {
		setSelectedLayer(mapView.getSelectedLayer());
	}
	
	protected MapButton createSelectLayerButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Select Layer");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				List<Layer> layers = BaseGeometryTool.this.mapView.getAllLayers();
				
				final ArrayList<Layer> filteredLayers = new ArrayList<Layer>();
				ArrayList<String> layerNames = new ArrayList<String>();
				for (Layer layer : layers) {
					if (layer.isVisible() && (layer instanceof CanvasLayer)) {
						filteredLayers.add(layer);
						layerNames.add(BaseGeometryTool.this.mapView.getLayerName(layer));
					}
				}
				
				if (filteredLayers.isEmpty()) {
					showError("No canvas layers found");
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setTitle("Select Layer");
				
					Collections.reverse(layerNames);
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
							int realPosition = filteredLayers.size() - 1 - position;
							setSelectedLayer(filteredLayers.get(realPosition));
						}
	
					});
					
					d.show();
				}
			}
				
		});
		
		return button;
	}
	
	protected void setSelectedLayer(Layer layer) {
		if (layer == null || !layer.isVisible()) {
			selectedLayer.setText("No layer selected");
			mapView.setSelectedLayer(null);
		} else {
			mapView.setSelectedLayer(layer);
			selectedLayer.setText("Current Layer: " + mapView.getLayerName(layer));
		}
		lastLayerSelected = layer;
	}

}
