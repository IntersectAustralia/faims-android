package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;

import com.nutiteq.MapView;
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
	
	public static Geometry cloneGeometry(Geometry geom1, Geometry geom2) {
		if (geom1 instanceof Point && geom2 instanceof Point) {
			Point p1 = (Point) geom1;
			Point p2 = (Point) geom2;
			return new Point(p2.getMapPos(), null, (StyleSet<PointStyle>) p1.getStyleSet(), null);
		} else if (geom1 instanceof Line && geom2 instanceof Line) {
			Line l1 = (Line) geom1;
			Line l2 = (Line) geom2;
			return new Line(l2.getVertexList(), null, (StyleSet<LineStyle>) l1.getStyleSet(), null);
		} else if (geom1 instanceof Polygon && geom2 instanceof Polygon) {
			Polygon p1 = (Polygon) geom1;
			Polygon p2 = (Polygon) geom2;
			return new Polygon(p2.getVertexList(), new ArrayList<List<MapPos>>(), null, (StyleSet<PolygonStyle>) p1.getStyleSet(), null);
		}
		return null;
	}

	public static Geometry worldToScreen(Geometry geom, MapView mapView) {
		return transformGeometry(geom, mapView, true);
	}
	
	public static Geometry screenToWorld(Geometry geom, MapView mapView) {
		return transformGeometry(geom, mapView, false);
	}
	
	public static Geometry transformGeometry(Geometry geom, MapView mapView, boolean worldToScreen) {
		if (geom instanceof Point) {
			Point p = (Point) geom;
			return new Point(translateVertex(p.getMapPos(), mapView, worldToScreen), null, (StyleSet<PointStyle>) p.getStyleSet(), null);
		} else if (geom instanceof Line) {
			Line l = (Line) geom;
			return new Line(translateVertices(l.getVertexList(), mapView, worldToScreen), null, (StyleSet<LineStyle>) l.getStyleSet(), null);
		} else if (geom instanceof Polygon) {
			Polygon p = (Polygon) geom;
			return new Polygon(translateVertices(p.getVertexList(), mapView, worldToScreen), new ArrayList<List<MapPos>>(), null, (StyleSet<PolygonStyle>) p.getStyleSet(), null);
		}
		return null;
	}
	
	public static MapPos translateVertex(MapPos vertex, MapView mapView, boolean worldToScreen) {
		if (worldToScreen)
			return mapView.worldToScreen(vertex.x, vertex.y, vertex.z);
		return mapView.screenToWorld(vertex.x, vertex.y);
	}
	
	public static List<MapPos> translateVertices(List<MapPos> vertices, MapView mapView, boolean worldToScreen) {
		List<MapPos> newVertices = new ArrayList<MapPos>();
		for (MapPos vertex : vertices) {
			newVertices.add(translateVertex(vertex, mapView, worldToScreen));
		}
		return newVertices;
	}
	
}
