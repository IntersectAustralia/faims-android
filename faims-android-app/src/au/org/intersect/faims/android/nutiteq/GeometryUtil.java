package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;

import com.nutiteq.MapView;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.projections.Projection;

public class GeometryUtil {

	public static Geometry fromGeometry(Geometry geom) {
		if (geom instanceof CustomPoint) {
			CustomPoint p = (CustomPoint) geom;
			return new CustomPoint(p.getGeomId(), p.getStyle(), p.getMapPos());
		} else if (geom instanceof CustomLine) {
			CustomLine l = (CustomLine) geom;
			return new CustomLine(l.getGeomId(), l.getStyle(), l.getVertexList());
		} else if (geom instanceof CustomPolygon) {
			CustomPolygon p = (CustomPolygon) geom;
			return new CustomPolygon(p.getGeomId(), p.getStyle(), p.getVertexList());
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
		if (geom instanceof CustomPoint) {
			CustomPoint p = (CustomPoint) geom;
			return new CustomPoint(p.getGeomId(), p.getStyle(), transformVertex(p.getMapPos(), mapView, worldToScreen));
		} else if (geom instanceof CustomLine) {
			CustomLine l = (CustomLine) geom;
			return new CustomLine(l.getGeomId(), l.getStyle(), transformVertices(l.getVertexList(), mapView, worldToScreen));
		} else if (geom instanceof CustomPolygon) {
			CustomPolygon p = (CustomPolygon) geom;
			return new CustomPolygon(p.getGeomId(), p.getStyle(), transformVertices(p.getVertexList(), mapView, worldToScreen));
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
		if (geom instanceof CustomPoint) {
			CustomPoint p = (CustomPoint) geom;
			return new CustomPoint(p.getGeomId(), p.getStyle(), projectVertex(proj, p.getMapPos()));
		} else if (geom instanceof CustomLine) {
			CustomLine l = (CustomLine) geom;
			return new CustomLine(l.getGeomId(), l.getStyle(), projectVertices(proj, l.getVertexList()));
		} else if (geom instanceof CustomPolygon) {
			CustomPolygon p = (CustomPolygon) geom;
			return new CustomPolygon(p.getGeomId(), p.getStyle(), projectVertices(proj, p.getVertexList()));
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
	
	public static String convertToDegrees(double value) {
		double degrees = value < 0 ? Math.ceil(value) : Math.floor(value);
		double totalSeconds = Math.floor(Math.abs(value - degrees) * 3600);
		double minutes = Math.floor((totalSeconds / 60));
		double seconds = totalSeconds - (minutes * 60);
		return Integer.toString((int) degrees) + ":" + toFixed((int) minutes, 2) + ":" + toFixed((int) seconds, 2);  
	}
	
	private static String toFixed(int value, int n) {
		String s = Integer.toString(value);
		while(s.length() < n) {
			s = "0" + s;
		}
		return s;
	}
	
}
