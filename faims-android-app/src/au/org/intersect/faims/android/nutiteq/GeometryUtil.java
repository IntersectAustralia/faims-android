package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;

import com.nutiteq.MapView;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.projections.Projection;
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
	
	public static ArrayList<Geometry> transformGeometryList(List<Geometry> geomList, MapView mapView, boolean worldToScreen) {
		ArrayList<Geometry> newGeomList = new ArrayList<Geometry>();
		for (Geometry geom : geomList) {
			newGeomList.add(transformGeometry(geom, mapView, worldToScreen));
		}
		return newGeomList;
	}
	
	public static Geometry transformGeometry(Geometry geom, MapView mapView, boolean worldToScreen) {
		if (geom instanceof Point) {
			Point p = (Point) geom;
			return new Point(transformVertex(p.getMapPos(), mapView, worldToScreen), null, (StyleSet<PointStyle>) p.getStyleSet(), null);
		} else if (geom instanceof Line) {
			Line l = (Line) geom;
			return new Line(transformVertices(l.getVertexList(), mapView, worldToScreen), null, (StyleSet<LineStyle>) l.getStyleSet(), null);
		} else if (geom instanceof Polygon) {
			Polygon p = (Polygon) geom;
			return new Polygon(transformVertices(p.getVertexList(), mapView, worldToScreen), new ArrayList<List<MapPos>>(), null, (StyleSet<PolygonStyle>) p.getStyleSet(), null);
		}
		return null;
	}
	
	public static MapPos transformVertex(MapPos vertex, MapView mapView, boolean worldToScreen) {
		if (worldToScreen)
			return mapView.worldToScreen(vertex.x, vertex.y, vertex.z);
		return mapView.screenToWorld(vertex.x, vertex.y);
	}
	
	public static List<MapPos> transformVertices(List<MapPos> vertices, MapView mapView, boolean worldToScreen) {
		List<MapPos> newVertices = new ArrayList<MapPos>();
		for (MapPos vertex : vertices) {
			newVertices.add(transformVertex(vertex, mapView, worldToScreen));
		}
		return newVertices;
	}
	
	public static Geometry projectGeometry(Projection proj, Geometry geom) {
		if (geom instanceof Point) {
			Point p = (Point) geom;
			return new Point(projectVertex(proj, p.getMapPos()), null, (StyleSet<PointStyle>) p.getStyleSet(), null);
		} else if (geom instanceof Line) {
			Line l = (Line) geom;
			return new Line(projectVertices(proj, l.getVertexList()), null, (StyleSet<LineStyle>) l.getStyleSet(), null);
		} else if (geom instanceof Polygon) {
			Polygon p = (Polygon) geom;
			return new Polygon(projectVertices(proj, p.getVertexList()), new ArrayList<List<MapPos>>(), null, (StyleSet<PolygonStyle>) p.getStyleSet(), null);
		}
		return null;
	}
	public static ArrayList<Geometry> projectGeometryList(Projection proj, List<Geometry> geomList) {
		ArrayList<Geometry> newGeomList = new ArrayList<Geometry>();
		for (Geometry geom : geomList) {
			newGeomList.add(projectGeometry(proj, geom));
		}
		return newGeomList;
	}
	
	public static MapPos projectVertex(Projection proj, MapPos v) {
		return proj.toWgs84(v.x, v.y);
	}
	
	public static List<MapPos> projectVertices(Projection proj, List<MapPos> vertices) {
		List<MapPos> newVertices = new ArrayList<MapPos>();
		for (MapPos v : vertices) {
			newVertices.add(projectVertex(proj, v));
		}
		return newVertices;
	}
	
	public static double distance(MapPos p1, MapPos p2) {
		float[] results = new float[3];
		Location.distanceBetween(p1.y, p1.x, p2.y, p2.x, results);
		return results[0] / 1000;
	}
	
	public static MapPos convertToWgs84(MapPos p) {
		return (new EPSG3857()).toWgs84(p.x, p.y);
	}
	
}
