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

	@SuppressWarnings("unchecked")
	public static Geometry translateGeometry(Geometry geom, double x, double y) {
		if (geom instanceof Point) {
			Point p = (Point) geom;
			return new Point(translateVertex(p.getMapPos(), x, y), null, (StyleSet<PointStyle>) p.getStyleSet(), null);
		} else if (geom instanceof Line) {
			Line l = (Line) geom;
			return new Line(translateVertices(l.getVertexList(), x, y), null, (StyleSet<LineStyle>) l.getStyleSet(), null);
		} else if (geom instanceof Polygon) {
			Polygon p = (Polygon) geom;
			return new Polygon(translateVertices(p.getVertexList(), x, y), new ArrayList<List<MapPos>>(), null, (StyleSet<PolygonStyle>) p.getStyleSet(), null);
		}
		return null;
	}
	
	public static MapPos translateVertex(MapPos vertex, double x, double y) {
		return new MapPos(vertex.x - x, vertex.y - y);
	}
	
	public static List<MapPos> translateVertices(List<MapPos> vertices, double x, double y) {
		List<MapPos> newVertices = new ArrayList<MapPos>();
		for (MapPos vertex : vertices) {
			newVertices.add(translateVertex(vertex, x, y));
		}
		return newVertices;
	}
	
}
