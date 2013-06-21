package au.org.intersect.faims.android.nutiteq;

import java.math.BigDecimal;
import java.util.ArrayList;
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
			return "POINT (" + doubleString(point.getMapPos().x) + " " + doubleString(point.getMapPos().y) + ")";
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
			ArrayList<MapPos> list = new ArrayList<MapPos>();
			for (MapPos p : polygon.getVertexList()) {
				list.add(p);
			}
			list.add(polygon.getVertexList().get(0));
			sb.append(verticesToWKT(list));
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
	
	private static String doubleString(double d) {
		return BigDecimal.valueOf(d).toPlainString();
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
			sb.append(doubleString(vertex.x) + " " + doubleString(vertex.y));
		}
		sb.append(")");
		return sb.toString();
	}

}
