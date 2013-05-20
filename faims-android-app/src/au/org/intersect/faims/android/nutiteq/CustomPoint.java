package au.org.intersect.faims.android.nutiteq;


import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Point;

public class CustomPoint extends Point {

	private int geomId;
	private GeometryStyle style;

	public CustomPoint(int geomId, GeometryStyle style, MapPos mapPos, String uuid) {
		super(mapPos, null, style.toPointStyleSet(), uuid);
		this.geomId = geomId;
		this.style = style;
	}

	public int getGeomId() {
		return geomId;
	}
	
	public GeometryStyle getStyle() {
		return style;
	}
	
}
