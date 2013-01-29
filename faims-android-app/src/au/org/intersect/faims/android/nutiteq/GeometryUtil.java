package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;

public class GeometryUtil {

	@SuppressWarnings("unchecked")
	public static Geometry fromGeometry(Geometry geom) {
		if (geom instanceof Point) {
			Point p = (Point) geom;
			return new Point(p.getMapPos(), null, (StyleSet<PointStyle>) p.getStyleSet(), null);
		} else if (geom instanceof Line) {
			Line l = (Line) geom;
			return new Line(l.getVertexList(), null, (StyleSet<LineStyle>) l.getStyleSet(), null);
		} else if (geom instanceof Polygon) {
			Polygon p = (Polygon) geom;
			return new Polygon(p.getVertexList(), new ArrayList<List<MapPos>>(), null, (StyleSet<PolygonStyle>) p.getStyleSet(), null);
		}
		return null;
	}
	
}
