package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.ui.Label;

public class CustomPolygon extends Polygon {

	private int geomId;

	public CustomPolygon(int geomId, List<MapPos> vertices, Label label,
			PolygonStyle polygonStyle, Object userData) {
		super(vertices, label, polygonStyle, userData);
		this.geomId = geomId;
	}
	
	public CustomPolygon(int geomId, List<MapPos> vertices,
			ArrayList<List<MapPos>> invertices, Label label,
			StyleSet<PolygonStyle> styleSet, Object userData) {
		super(vertices, invertices, label, styleSet, userData);
		this.geomId = geomId;
	}

	public int getGeomId() {
		return geomId;
	}

}
