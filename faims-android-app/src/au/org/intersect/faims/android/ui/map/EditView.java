package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import au.org.intersect.faims.android.util.GeometryUtil;

import com.nutiteq.components.MapPos;

public class EditView extends DrawView {

	public EditView(Context context) {
		super(context);
	}
	
	protected MapPos transformPoint(MapPos p) {
		return p;
	}
	
	protected MapPos projectPoint(MapPos p) {
		p = GeometryUtil.transformVertex(p, mapView, false);
		return databaseManager.spatialRecord().convertFromProjToProj(GeometryUtil.EPSG3857, mapView.getModuleSrid(), p);
	}

}
