package au.org.intersect.faims.android.util;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import jsqlite.Stmt;
import android.location.Location;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.nutiteq.WKBUtil;
import au.org.intersect.faims.android.nutiteq.WKTUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.style.LineStyle;
import com.nutiteq.utils.Utils;
import com.nutiteq.utils.WkbRead;

public class SpatialiteUtil {
	
private static String dbname;
	
	public static void setDatabaseName(String name) {
		dbname = name;
	}
	
	public static float computeLineDistance(List<MapPos> points, String srid) throws Exception {
		if (GeometryUtil.EPSG4326.equals(srid)) {
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
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				String sql = "select st_length(transform(LineFromText(?, 4326), ?));";
				st = db.prepare(sql);
				st.bind(1, WKTUtil.geometryToWKT(new Line(points, null, (LineStyle) null, null)));
				st.bind(2, Integer.parseInt(srid));
				st.step();
				return (float) st.column_double(0);
			} finally {
				if (st != null) {
					try {
						st.close();
					} catch (Exception e) {
						FLog.e("error closing statement", e);
					}
				}
				if (db != null) {
					try {
						db.close();
					} catch (Exception e) {
						FLog.e("error closing database", e);
					}
				}
			}
		}
	}
	
	public static double computePointDistance(MapPos p1, MapPos p2, String srid) throws Exception {
		if (GeometryUtil.EPSG4326.equals(srid)) {
			float[] results = new float[3];
			Location.distanceBetween(p1.y, p1.x, p2.y, p2.x, results);
			return results[0];
		} else {
			jsqlite.Database db = null;
			Stmt st = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
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
				if (st != null) {
					try {
						st.close();
					} catch (Exception e) {
						FLog.e("error closing statement", e);
					}
				}
				if (db != null) {
					try {
						db.close();
					} catch (Exception e) {
						FLog.e("error closing database", e);
					}
				}
			}
		}
	}
	
	public static float computeAngleBetween(MapPos v1, MapPos v2) {
		float angle = (float) (Math.acos(dot(v1, v2) / (length(v1) * length(v2))) * 180 / Math.PI);
		if (v2.x < 0) return -angle;
		return angle;
	}
	
	public static  float dot(MapPos p1, MapPos p2) {
		return (float) (p1.x * p2.x + p1.y * p2.y);
	}
	
	public static float length(MapPos p) {
		return (float) Math.sqrt(p.x * p.x + p.y * p.y);
	}
	
	public static float computeAzimuth(MapPos p1, MapPos p2) {
		Location l1 = new Location("");
		l1.setLatitude(p1.y);
		l1.setLongitude(p1.x);
		
		Location l2 = new Location("");
		l2.setLatitude(p2.y);
		l2.setLongitude(p2.x);
		
		return (l1.bearingTo(l2) + 360) % 360;
	}
	
	public static MapPos computeCentroid(Polygon polygon) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
			String sql = "select X(pt), Y(pt) from (select centroid(GeomFromText(?, 4326)) as pt);";
			st = db.prepare(sql);
			st.bind(1, WKTUtil.geometryToWKT(polygon));
			st.step();
			return new MapPos(st.column_double(0), st.column_double(1));
		} finally {
			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {
					FLog.e("error closing statement", e);
				}
			}
			if (db != null) {
				try {
					db.close();
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	public static double computePolygonArea(Polygon polygon, String srid) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
			String sql = "select area(transform(GeomFromText(?, 4326), ?));";
			st = db.prepare(sql);
			st.bind(1, WKTUtil.geometryToWKT(polygon));
			st.bind(2, Integer.parseInt(srid));
			st.step();
			return st.column_double(0);
		} finally {
			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {
					FLog.e("error closing statement", e);
				}
			}
			if (db != null) {
				try {
					db.close();
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	public static Geometry geometryBuffer(Geometry geom, float buffer, String srid) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
			String sql = "select Hex(AsBinary(transform(buffer(transform(GeomFromText(?, 4326), ?), ?), 4326)));";
			st = db.prepare(sql);
			st.bind(1, WKTUtil.geometryToWKT(geom));
			st.bind(2, Integer.parseInt(srid));
			st.bind(3, buffer);
			st.step();
			Geometry[] gs = WKBUtil.cleanGeometry(WkbRead.readWkb(
                    new ByteArrayInputStream(Utils
                            .hexStringToByteArray(st.column_string(0))), null));
			if (gs != null) {
	            return (Polygon) gs[0];
			}
			return null;
		} finally {
			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {
					FLog.e("error closing statement", e);
				}
			}
			if (db != null) {
				try {
					db.close();
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public static boolean isPointOnPath(Point point, Line path, float buffer, String srid) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
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
			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {
					FLog.e("error closing statement", e);
				}
			}
			if (db != null) {
				try {
					db.close();
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public static double distanceBetween(MapPos p1, MapPos p2, String srid) throws Exception {
		return computePointDistance(p1, p2, srid);
	}

	/*
	public static Point nearestPointOnPath(Point point, Line path) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
			String sql = "select Hex(AsBinary(transform(closestPoint(transform(GeomFromText(?, 4326), ?), transform(GeomFromText(?, 4326), ?)), 4326)));";
			st = db.prepare(sql);
			st.bind(1, WKTUtil.geometryToWKT(path));
			st.bind(2, WKTUtil.geometryToWKT(point));
			st.step();
			Geometry[] gs = WkbRead.readWkb(
                    new ByteArrayInputStream(Utils
                            .hexStringToByteArray(st.column_string(0))), null);
			if (gs != null) {
	            return (Point) gs[0];
			}
			return null;
		} finally {
			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {
					FLog.e("error closing statement", e);
				}
			}
			if (db != null) {
				try {
					db.close();
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	*/
	
	public static Geometry convertFromProjToProj(String fromSrid, String toSrid, Geometry geom) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
			String sql = "select Hex(AsBinary(transform(GeomFromText(?, ?), ?)));";
			st = db.prepare(sql);
			st.bind(1, WKTUtil.geometryToWKT(geom));
			st.bind(2, Integer.parseInt(fromSrid));
			st.bind(3, Integer.parseInt(toSrid));
			st.step();
			Geometry[] gs = WKBUtil.cleanGeometry(WkbRead.readWkb(
                    new ByteArrayInputStream(Utils
                            .hexStringToByteArray(st.column_string(0))), null));
			if (gs != null) {
	            return (Geometry) gs[0];
			}
			return null;
		} finally {
			if (st != null) {
				try {
					st.close();
				} catch (Exception e) {
					FLog.e("error closing statement", e);
				}
			}
			if (db != null) {
				try {
					db.close();
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
}
