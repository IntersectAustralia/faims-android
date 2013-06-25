package au.org.intersect.faims.android.ui.map.tools;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

import android.content.Context;
import au.org.intersect.faims.android.nutiteq.GeometryData;
import au.org.intersect.faims.android.ui.map.CustomMapView;

public class LoadTool extends MapTool {
	
	public static final String NAME = "Load Data";

	public LoadTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
	}
	
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (element instanceof Geometry) {
			Geometry geom = (Geometry) element;
			if (geom.userData instanceof GeometryData) {
				GeometryData geomData = (GeometryData) geom.userData;
				if (geomData.id != null) {
					mapView.notifyGeometryLoaded(geom);
				}
			} 
		}
	}

}
