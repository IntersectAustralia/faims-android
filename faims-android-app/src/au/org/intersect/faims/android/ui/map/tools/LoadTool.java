package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.nutiteq.GeometryData;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.ToolBarButton;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

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
	
	public ToolBarButton getButton(Context context) {
		ToolBarButton button = new ToolBarButton(context);
		button.setLabel("Load");
		button.setSelectedState(R.drawable.tools_select_s);
		button.setNormalState(R.drawable.tools_select);
		return button;
	}

}
