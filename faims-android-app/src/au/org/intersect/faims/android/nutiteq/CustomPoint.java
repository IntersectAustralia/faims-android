package au.org.intersect.faims.android.nutiteq;


import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Point;

public class CustomPoint extends Point {

	private int geomId;
	private GeometryStyle style;

	public CustomPoint(int geomId, GeometryStyle style, MapPos mapPos, String type, String uuid, String label) {
		this(geomId, style, mapPos, new String[] { type, uuid, label });
	}
	
	public CustomPoint(int geomId, GeometryStyle style, MapPos mapPos, String[] userData) {
		super(mapPos, null, style.toPointStyleSet(), userData);
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
