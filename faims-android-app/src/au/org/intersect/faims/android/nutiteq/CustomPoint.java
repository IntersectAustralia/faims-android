package au.org.intersect.faims.android.nutiteq;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Point;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.ui.Label;

public class CustomPoint extends Point {

	private int geomId;

	public CustomPoint(int geomId, MapPos mapPos, Label label, PointStyle pointStyle,
			Object userData) {
		super(mapPos, label, pointStyle, userData);
		this.geomId = geomId;
	}
	
	public CustomPoint(int geomId, MapPos mapPos, Label label,
			StyleSet<PointStyle> styleSet, Object userData) {
		super(mapPos, label, styleSet, userData);
		this.geomId = geomId;
	}

	public int getGeomId() {
		return geomId;
	}

}
