package au.org.intersect.faims.android.ui.map.tools;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

import android.content.Context;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.map.CustomMapView;

public class HighlightSelectionTool extends SelectionTool {

	public HighlightSelectionTool(Context context, CustomMapView mapView,
			String name) {
		super(context, mapView, name);
	}
	
	@Override
	public void activate() {
		super.activate();
		clearSelection();
	}
	
	@Override
	public void deactivate() {
		super.deactivate();
		clearSelection();
	}
	
	protected void clearSelection() {
		try {
			mapView.clearHighlights();
		} catch (Exception e) {
			FLog.e("error clearing selection", e);
			showError(e.getMessage());
		}
	}
	
	@Override
	public void onLayersChanged() {
		super.onLayersChanged();
		try {
			mapView.updateHighlights();
		} catch (Exception e) {
			FLog.e("error updating selection", e);
			showError(e.getMessage());
		}
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (element instanceof Geometry) {
			try {
				Geometry geom = (Geometry) element;
				
				if (mapView.hasHighlight(geom)) {
					mapView.removeHighlight(geom);
				} else {
					mapView.addHighlight(geom);
				}
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}

}
