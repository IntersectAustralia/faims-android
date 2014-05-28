package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.CustomGdalMapLayer;
import au.org.intersect.faims.android.nutiteq.CustomOgrLayer;
import au.org.intersect.faims.android.nutiteq.CustomSpatialiteLayer;
import au.org.intersect.faims.android.nutiteq.DatabaseLayer;
import au.org.intersect.faims.android.ui.dialog.ErrorDialog;
import au.org.intersect.faims.android.ui.map.button.EyeButton;
import au.org.intersect.faims.android.ui.view.CustomDragDropListView;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nutiteq.layers.Layer;

public class LayerListItem extends LinearLayout {

	private TextView text;
	private Layer layer;
	private EyeButton visibleButton;

	public LayerListItem(Context context) {
		super(context);
		setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
		setOrientation(LinearLayout.HORIZONTAL);
		int size = (int) ScaleUtil.getDip(context, 10);
		setPadding(size, size, size, size);
	}
	
	public void init(final CustomDragDropListView listView, final Layer layer, final CustomMapView mapView) {
		this.layer = layer;
		text = new TextView(this.getContext());
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		text.setLayoutParams(params);
		int size = (int) ScaleUtil.getDip(this.getContext(), 5);
		text.setPadding(size, size, size, size);
		text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
		text.setSingleLine(false);
		text.setText(getLayerName(layer));
		
		visibleButton = new EyeButton(this.getContext());
		visibleButton.setChecked(layer.isVisible());
		visibleButton.setHighlight(mapView.getSelectedLayer() == layer);
		visibleButton.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent action) {
				if(action.getAction() == MotionEvent.ACTION_UP){
					try {
						mapView.setLayerVisible(layer, !visibleButton.isChecked());
						visibleButton.setChecked(!visibleButton.isChecked());
						mapView.updateLayers();
					} catch (Exception e) {
						FLog.e("error setting layer visibility", e);
						showErrorDialog("Error setting layer visibility");
					}
					((BaseAdapter)listView.getAdapter()).notifyDataSetChanged();
					v.setFocusable(false);
				}
				return true;
			}
		});
		
		addView(text);
		addView(visibleButton);
	}
	
	private String getLayerName(Layer layer) {
		String layerName = "N/A";
		if (layer instanceof CustomGdalMapLayer) {
			layerName = ((CustomGdalMapLayer) layer).getName();
		} else if (layer instanceof CustomOgrLayer) {
			layerName = ((CustomOgrLayer) layer).getName();
		} else if (layer instanceof CustomSpatialiteLayer) {
			layerName = ((CustomSpatialiteLayer) layer).getName();
		} else if (layer instanceof CanvasLayer) {
			layerName = ((CanvasLayer) layer).getName();
		} else if (layer instanceof DatabaseLayer) {
			layerName = ((DatabaseLayer) layer).getName();
		}
		return layerName;
	}

	public void setItemSelected(boolean selected){
		setBackgroundResource(selected? android.R.color.holo_blue_dark : 0);
		visibleButton.setHighlight(selected);
	}
	
	private void showErrorDialog(String message) {
		new ErrorDialog(this.getContext(), "Layer Manager Error", message).show();
	}

	public Layer getLayer() {
		return layer;
	}
}
