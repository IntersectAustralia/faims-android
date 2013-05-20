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

public class GeometryUtil {

	public static Geometry fromGeometry(Geometry geom) {
		if (geom instanceof Point) {
			Point p = (Point) geom;
			return new Point(p.getMapPos(), p.getLabel(), p.getStyleSet(), p.userData);
		} else if (geom instanceof Line) {
			Line l = (Line) geom;
			return new Line(l.getVertexList(), l.getLabel(), l.getStyleSet(), l.userData);
		} else if (geom instanceof Polygon) {
			Polygon p = (Polygon) geom;
			return new Polygon(p.getVertexList(), new ArrayList<List<MapPos>>(), p.getLabel(), p.getStyleSet(), p.userData);
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
			return new CustomPoint(p.getGeomId(), p.getStyle(), transformVertex(p.getMapPos(), mapView, worldToScreen), (String) p.userData);
		} else if (geom instanceof CustomLine) {
			CustomLine l = (CustomLine) geom;
			return new CustomLine(l.getGeomId(), l.getStyle(), transformVertices(l.getVertexList(), mapView, worldToScreen), (String) l.userData);
		} else if (geom instanceof CustomPolygon) {
			CustomPolygon p = (CustomPolygon) geom;
			return new CustomPolygon(p.getGeomId(), p.getStyle(), transformVertices(p.getVertexList(), mapView, worldToScreen), (String) p.userData);
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
	
	public static Geometry convertGeometryFromWgs84(Geometry geom) {
		if (geom instanceof CustomPoint) {
			CustomPoint p = (CustomPoint) geom;
			return new CustomPoint(p.getGeomId(), p.getStyle(), convertFromWgs84(p.getMapPos()), (String) p.userData);
		} else if (geom instanceof CustomLine) {
			CustomLine l = (CustomLine) geom;
			return new CustomLine(l.getGeomId(), l.getStyle(), convertFromWgs84(l.getVertexList()), (String) l.userData);
		} else if (geom instanceof CustomPolygon) {
			CustomPolygon p = (CustomPolygon) geom;
			return new CustomPolygon(p.getGeomId(), p.getStyle(), convertFromWgs84(p.getVertexList()), (String) p.userData);
		}
		return null;
	}
	
	
	public static ArrayList<Geometry> convertGeometryListFromWgs84(List<Geometry> geomList) {
		ArrayList<Geometry> newGeomList = new ArrayList<Geometry>();
		for (Geometry geom : geomList) {
			newGeomList.add(convertGeometryFromWgs84(geom));
		}
		return newGeomList;
	}
	
	public static Geometry convertGeometryToWgs84(Geometry geom) {
		if (geom instanceof CustomPoint) {
			CustomPoint p = (CustomPoint) geom;
			return new CustomPoint(p.getGeomId(), p.getStyle(), convertToWgs84(p.getMapPos()), (String) p.userData);
		} else if (geom instanceof CustomLine) {
			CustomLine l = (CustomLine) geom;
			return new CustomLine(l.getGeomId(), l.getStyle(), convertToWgs84(l.getVertexList()), (String) l.userData);
		} else if (geom instanceof CustomPolygon) {
			CustomPolygon p = (CustomPolygon) geom;
			return new CustomPolygon(p.getGeomId(), p.getStyle(), convertToWgs84(p.getVertexList()), (String) p.userData);
		}
		return null;
	}
	
	
	public static ArrayList<Geometry> convertGeometryListToWgs84(List<Geometry> geomList) {
		ArrayList<Geometry> newGeomList = new ArrayList<Geometry>();
		for (Geometry geom : geomList) {
			newGeomList.add(convertGeometryToWgs84(geom));
		}
		return newGeomList;
	}
	
	public static double distance(MapPos p1, MapPos p2) {
		float[] results = new float[3];
		Location.distanceBetween(p1.y, p1.x, p2.y, p2.x, results);
		return results[0] / 1000;
	}
	
	public static List<MapPos> convertFromWgs84(List<MapPos> pts) {
		ArrayList<MapPos> list = new ArrayList<MapPos>();
		for (MapPos p : pts) {
			list.add(convertToWgs84(p));
		}
		return list;
	}
	
	public static MapPos convertFromWgs84(MapPos p) {
		return (new EPSG3857()).fromWgs84(p.x, p.y);
	}
	
	public static List<MapPos> convertToWgs84(List<MapPos> pts) {
		ArrayList<MapPos> list = new ArrayList<MapPos>();
		for (MapPos p : pts) {
			list.add(convertToWgs84(p));
		}
		return list;
	}
	
	public static MapPos convertToWgs84(MapPos p) {
		return (new EPSG3857()).toWgs84(p.x, p.y);
	}
	
}
