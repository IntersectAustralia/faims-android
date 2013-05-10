package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.view.View;
import au.org.intersect.faims.android.ui.dialog.ErrorDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;

public abstract class MapTool extends CustomMapView.CustomMapListener {
	
	protected String name;
	protected CustomMapView mapView;
	protected Context context;

	public MapTool(Context context, CustomMapView mapView, String name) {
		this.name = name;
		this.mapView = mapView;
		this.context = context;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public void activate() {
		
	}
	
	public void deactivate() {
		
	}
	
	public void update() {
		
	}
	
	// define tool context ui
	public abstract View getUI();
	
	public void showError(Context context, String message) {
		new ErrorDialog(context, "Tool Error", message).show();
	}
	
}
