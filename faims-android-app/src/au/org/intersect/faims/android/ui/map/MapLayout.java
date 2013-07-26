package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.activity.ShowProjectActivity;

public class MapLayout extends LinearLayout {

	private DrawView drawView;
	private CustomMapView mapView;
	private RelativeLayout toolsView;
	private RelativeLayout layersView;
	private EditView editView;
	private RelativeLayout container;
	
	private LayerBarView layerBarView;
	private ToolsBarView toolsBarView;

	public MapLayout(Context context) {
		super(context);
		
		ShowProjectActivity activity = (ShowProjectActivity) context;
		
		this.setOrientation(LinearLayout.VERTICAL);
		this.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));
		
		container = new RelativeLayout(activity);
		drawView = new DrawView(activity);
		editView = new EditView(activity);
		toolsView = new RelativeLayout(activity);
		layersView = new RelativeLayout(activity);
		
		mapView = new CustomMapView(activity, this);
		mapView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mapView.startMapping();
		
		layerBarView = new LayerBarView(activity);
		layerBarView.setMapView(mapView);
		
		toolsBarView = new ToolsBarView(activity);
		toolsBarView.setMapView(mapView);
		
		indexViews();
		
		addView(container);
	}

	public DrawView getDrawView() {
		return drawView;
	}

	public EditView getEditView() {
		return editView;
	}

	public CustomMapView getMapView() {
		return mapView;
	}
	
	public RelativeLayout getToolsView() {
		return toolsView;
	}
	
	public RelativeLayout getLayersView() {
		return layersView;
	}
	
	public LayerBarView getLayerBarView() {
		return layerBarView;
	}
	
	public ToolsBarView getToolsBarView() {
		return toolsBarView;
	}
	
	private void indexViews() {
		while(container.getChildCount() > 0) {
			container.removeViewAt(0);
		}
		if (mapView != null) container.addView(mapView);
		if (drawView != null) container.addView(drawView);
		if (editView != null) container.addView(editView);
		if (toolsView != null) container.addView(toolsView);
		if (layersView != null) container.addView(layersView);
		if (layerBarView != null) container.addView(layerBarView);
		if (toolsBarView != null) container.addView(toolsBarView);
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		Bitmap logo = BitmapFactory.decodeResource(getContext().getResources(),
				R.drawable.ic_launcher);
		if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			CustomMapView.setWatermark(logo, -1.0f, -0.9f, 0.1f);
		}else{
			CustomMapView.setWatermark(logo, -1.0f, -0.7f, 0.1f);
		}
		super.onConfigurationChanged(newConfig);
	}
}
