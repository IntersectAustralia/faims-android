package au.org.intersect.faims.android.nutiteq;

import java.util.List;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Line;

public class CustomLine extends Line {

	private int geomId;
	private GeometryStyle style;

	public CustomLine(int geomId, GeometryStyle style, List<MapPos> vertices) {
		super(vertices, null, style.toLineStyleSet(), null);
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
