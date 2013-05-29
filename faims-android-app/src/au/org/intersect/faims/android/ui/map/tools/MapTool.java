package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import au.org.intersect.faims.android.ui.dialog.ErrorDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;

public abstract class MapTool extends CustomMapView.CustomMapListener {
	
	protected String name;
	protected CustomMapView mapView;
	protected Context context;
	
	protected RelativeLayout container;
	
	public MapTool(Context context, CustomMapView mapView, String name) {
		this.name = name;
		this.mapView = mapView;
		this.context = context;
		
		container = new RelativeLayout(context);
		container.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public void activate() {
		
	}
	
	public void deactivate() {
		
	}
	
	public void onLayersChanged() {
	}
	
	public void onSelectionChanged() {
	}
	
	public void onMapChanged() {
		
	}
	
	public View getUI() {
		return container;
	}
	
	protected void showError(String message) {
		new ErrorDialog(context, "Tool Error", message).show();
	}
	
}
