package au.org.intersect.faims.android.database;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jsqlite.Stmt;
import android.location.Location;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.WKBUtil;
import au.org.intersect.faims.android.nutiteq.WKTUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.utils.Utils;
import com.nutiteq.utils.WkbRead;

public class SpatialRecord extends Database {
	
	public SpatialRecord(File dbFile) {
		super(dbFile);
	}

	public double getDistanceBetween(String prevLong, String prevLat, String curLong, String curLat) throws jsqlite.Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		double distance = 0;
		try {
			db = openDB();
			String query = "select geodesiclength(linefromtext('LINESTRING("+ prevLong+" " + prevLat + ", " + curLong + " " + curLat + ")', 4326));";
			st = db.prepare(query);
			if(st.step()){
				distance = st.column_double(0);
			}
			st.close();
			st = null;
			return distance;
		} finally {
			closeStmt(st);
			closeDB(db);
		}
	}
	
	public float computeLineDistance(List<MapPos> points, String srid) throws Exception {
		if (!isProperProjection(srid)) {
			float totalDistance = 0;
			MapPos lp = null;
			for (MapPos p : points) {
				if (lp != null) {
					float[] results = new float[3];
					Location.distanceBetween(lp.y, lp.x, p.y, p.x, results);
					totalDistance += results[0];
				}
				lp = p;
			}
			return totalDistance;
		} else {
			jsqlite.Database db = null;
			Stmt st = null;
			try {
				db = openDB();
				String sql = "select st_length(transform(LineFromText(?, 4326), ?));";
				st = db.prepare(sql);
				st.bind(1, WKTUtil.geometryToWKT(new Line(points, null, (LineStyle) null, null)));
				st.bind(2, Integer.parseInt(srid));
				st.step();
				return (float) st.column_double(0);
			} finally {
				closeStmt(st);
				closeDB(db);
			}
		}
	}
	
	public double computePointDistance(MapPos p1, MapPos p2, String srid) throws Exception {
		if (!isProperProjection(srid)) {
			float[] results = new float[3];
			Location.distanceBetween(p1.y, p1.x, p2.y, p2.x, results);
			return results[0];
		} else {
			jsqlite.Database db = null;
			Stmt st = null;
			try {
				db = openDB();
				String sql = "select st_length(transform(LineFromText(?, 4326), ?));";
				st = db.prepare(sql);
				ArrayList<MapPos> list = new ArrayList<MapPos>();
				list.add(p1);
				list.add(p2);
				st.bind(1, WKTUtil.geometryToWKT(new Line(list, null, (LineStyle) null, null)));
				st.bind(2, Integer.parseInt(srid));
				st.step();
				return st.column_double(0);
			} finally {
				closeStmt(st);
				closeDB(db);
			}
		}
	}
	
	public MapPos computeCentroid(Polygon polygon) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = openDB();
			String sql = "select X(pt), Y(pt) from (select centroid(GeomFromText(?, 4326)) as pt);";
			st = db.prepare(sql);
			st.bind(1, WKTUtil.geometryToWKT(polygon));
			st.step();
			return new MapPos(st.column_double(0), st.column_double(1));
		} finally {
			closeStmt(st);
			closeDB(db);
		}
	}
	
	public double computePolygonArea(Polygon polygon, String srid) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = openDB();
			String sql = "select area(transform(GeomFromText(?, 4326), ?));";
			st = db.prepare(sql);
			st.bind(1, WKTUtil.geometryToWKT(polygon));
			st.bind(2, Integer.parseInt(srid));
			st.step();
			return st.column_double(0);
		} finally {
			closeStmt(st);
			closeDB(db);
		}
	}
	
	public Geometry geometryBuffer(Geometry geom, float buffer, String srid) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = openDB();
			String sql = "select Hex(AsBinary(transform(buffer(transform(GeomFromText(?, 4326), ?), ?), 4326)));";
			st = db.prepare(sql);
			st.bind(1, WKTUtil.geometryToWKT(geom));
			st.bind(2, Integer.parseInt(srid));
			st.bind(3, buffer);
			st.step();
			Geometry[] gs = WKBUtil.cleanGeometry(WkbRead.readWkb(
                    new ByteArrayInputStream(Utils
                            .hexStringToByteArray(st.column_string(0))), (Object) null));
			if (gs != null) {
	            return (Polygon) gs[0];
			}
			return null;
		} finally {
			closeStmt(st);
			closeDB(db);
		}
	}

	public boolean isPointOnPath(Point point, Line path, float buffer, String srid) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = openDB();
			String sql = "select st_intersects(buffer(transform(GeomFromText(?, 4326), ?), ?), transform(GeomFromText(?, 4326), ?));";
			st = db.prepare(sql);
			st.bind(1, WKTUtil.geometryToWKT(path));
			st.bind(2, Integer.parseInt(srid));
			st.bind(3, buffer);
			st.bind(4, WKTUtil.geometryToWKT(point));
			st.bind(5, Integer.parseInt(srid));
			st.step();
			return st.column_int(0) == 1;
		} finally {
			closeStmt(st);
			closeDB(db);
		}
	}

	public double distanceBetween(MapPos p1, MapPos p2, String srid) throws Exception {
		return computePointDistance(p1, p2, srid);
	}

	public Geometry convertFromProjToProj(String fromSrid, String toSrid, Geometry geom) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = openDB();
			String sql = "select Hex(AsBinary(transform(GeomFromText(?, ?), ?)));";
			st = db.prepare(sql);
			st.bind(1, WKTUtil.geometryToWKT(geom));
			st.bind(2, Integer.parseInt(fromSrid));
			st.bind(3, Integer.parseInt(toSrid));
			st.step();
			Geometry[] gs = WKBUtil.cleanGeometry(WkbRead.readWkb(
                    new ByteArrayInputStream(Utils
                            .hexStringToByteArray(st.column_string(0))), (Object) null));
			if (gs != null) {
	            return (Geometry) gs[0];
			}
			return null;
		} finally {
			closeStmt(st);
			closeDB(db);
		}
	}
	
	public boolean isProperProjection(String srid) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = openDB();
			String sql = "select count(srid) from spatial_ref_sys where proj4text like '%+units=m%' and srid = ?;";
			st = db.prepare(sql);
			st.bind(1, Integer.parseInt(srid));
			st.step();
			return st.column_int(0) == 1;
		} finally {
			closeStmt(st);
			closeDB(db);
		}
	}
	
	public MapPos convertFromProjToProj(String fromSrid, String toSrid, MapPos p) {
		try {
			Point point = (Point) databaseManager.spatialRecord().convertFromProjToProj(fromSrid, toSrid, new Point(p, null, (PointStyle) null, null));
			return point.getMapPos();
		} catch (Exception e) {
			FLog.e("error converting from proj " + fromSrid + " to " + toSrid, e);
			return null;
		}
	}
	
	public List<MapPos> convertFromProjToProj(String fromSrid, String toSrid, List<MapPos> list) {
		try {
			ArrayList<MapPos> newList = new ArrayList<MapPos>();
			for (MapPos p : list) {
				Point point = (Point) databaseManager.spatialRecord().convertFromProjToProj(fromSrid, toSrid, new Point(p, null, (PointStyle) null, null));
				newList.add(point.getMapPos());
			}
			return newList;
		} catch (Exception e) {
			FLog.e("error converting from proj " + fromSrid + " to " + toSrid, e);
			return null;
		}
	}
	
	public Geometry convertGeometryFromProjToProj(String fromSrid, String toSrid, Geometry geom) {
		try {
			Geometry g = databaseManager.spatialRecord().convertFromProjToProj(fromSrid, toSrid, geom);
			if (geom instanceof Point) {
				Point p = (Point) geom;
				return new Point(((Point)g).getMapPos(), p.getLabel(), p.getStyleSet(), p.userData);
			} else if (geom instanceof Line) {
				Line l = (Line) geom;
				return new Line(((Line)g).getVertexList(), l.getLabel(), l.getStyleSet(), l.userData);
			} else if (geom instanceof Polygon) {
				Polygon p = (Polygon) geom;
				return new Polygon(((Polygon)g).getVertexList(), new ArrayList<List<MapPos>>(), p.getLabel(), p.getStyleSet(), p.userData);
			}
			return null;
		} catch (Exception e) {
			FLog.e("error converting from proj " + fromSrid + " to " + toSrid, e);
			return null;
		}
	}
	
	public List<Geometry> convertGeometryFromProjToProj(String fromSrid, String toSrid, List<Geometry> geomList) {
		try {
			ArrayList<Geometry> newList = new ArrayList<Geometry>();
			if(geomList == null) return null;
			for (Geometry geom : geomList) {
				Geometry g = convertFromProjToProj(fromSrid, toSrid, geom);
				if (geom instanceof Point) {
					Point p = (Point) geom;
					newList.add(new Point(((Point)g).getMapPos(), p.getLabel(), p.getStyleSet(), p.userData));
				} else if (geom instanceof Line) {
					Line l = (Line) geom;
					newList.add(new Line(((Line)g).getVertexList(), l.getLabel(), l.getStyleSet(), l.userData));
				} else if (geom instanceof Polygon) {
					Polygon p = (Polygon) geom;
					newList.add(new Polygon(((Polygon)g).getVertexList(), new ArrayList<List<MapPos>>(), p.getLabel(), p.getStyleSet(), p.userData));
				}
			}
			return newList;
		} catch (Exception e) {
			FLog.e("error converting from proj " + fromSrid + " to " + toSrid, e);
			return null;
		}
	}
	
}
