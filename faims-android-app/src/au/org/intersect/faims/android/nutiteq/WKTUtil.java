package au.org.intersect.faims.android.nutiteq;

import java.util.List;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;

public class WKTUtil {
	
	public static String geometryToWKT(Geometry geometry) {
		if (geometry == null) return "";
		
		if (geometry instanceof Point) {
			Point point = (Point) geometry;
			return "POINT (" + String.valueOf(point.getMapPos().x) + " " + String.valueOf(point.getMapPos().y) + ")";
		} else if (geometry instanceof Line) {
			Line line = (Line) geometry;
			StringBuilder sb = new StringBuilder();
			sb.append("LINESTRING ");
			sb.append(verticesToWKT(line.getVertexList()));
			return sb.toString();
		} else if (geometry instanceof Polygon) {
			Polygon polygon = (Polygon) geometry;
			StringBuilder sb = new StringBuilder();
			sb.append("POLYGON (");
			sb.append(verticesToWKT(polygon.getVertexList()));
			sb.append(")");
			return sb.toString();
		}
		
		return "";
	}
	
	/*
	public static String geometryToWKT(List<? extends Geometry> geometryList) {
		if (geometryList.size() == 0) return "";
		
		Geometry geometry = geometryList.get(0);
		if (geometry instanceof Point) {
			List<Point> pointList = (List<Point>) geometryList;
		} else if (geometry instanceof Line) {
			List<Line> lineList = (List<Line>) geometryList;
		} else if (geometry instanceof Polygon) {
			List<Polygon> polygonList = (List<Polygon>) geometryList;
		}
		
		return "";
	}
	*/
	
	public static String collectionToWKT(List<Geometry> geometryList) {
		if (geometryList == null || geometryList.size() == 0) return null;
		
		StringBuilder sb = new StringBuilder();
		sb.append("GEOMETRYCOLLECTION (");
		boolean first = true;
		for (Geometry geometry : geometryList) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(geometryToWKT(geometry));
		}
		sb.append(")");
		return sb.toString();
	}
	
	private static String verticesToWKT(List<MapPos> vertices) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		boolean first = true;
		for (MapPos vertex : vertices) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(String.valueOf(vertex.x) + " " + String.valueOf(vertex.y));
		}
		sb.append(")");
		return sb.toString();
	}

}
