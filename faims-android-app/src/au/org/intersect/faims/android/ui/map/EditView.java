package au.org.intersect.faims.android.ui.map;

import com.nutiteq.components.MapPos;

import android.content.Context;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;

public class EditView extends DrawView {

	public EditView(Context context) {
		super(context);
	}
	
	protected MapPos transformPoint(MapPos p) {
		return p;
	}
	
	protected MapPos projectPoint(MapPos p) {
		p = GeometryUtil.transformVertex(p, mapView, false);
		return GeometryUtil.convertToWgs84(p);
	}

}
