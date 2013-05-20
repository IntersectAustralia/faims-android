package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Polygon;

public class CustomPolygon extends Polygon {

	private int geomId;
	private GeometryStyle style;

	public CustomPolygon(int geomId, GeometryStyle style, List<MapPos> vertices, String uuid) {
		super(vertices, new ArrayList<List<MapPos>>(), null, style.toPolygonStyleSet(), uuid);
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
