package au.org.intersect.faims.android.util;

import jsqlite.Stmt;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.WKTUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Polygon;

public class SpatialiteUtil {
	
private static String dbname;
	
	public static void setDatabaseName(String name) {
		dbname = name;
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
				st.close();
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
	
	public static double computeArea(Polygon polygon) throws Exception {
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
			String sql = "select area(transform(GeomFromText(?, 4326), 28356));";
			st = db.prepare(sql);
			st.bind(1, WKTUtil.geometryToWKT(polygon));
			st.step();
			return st.column_double(0);
		} finally {
			if (st != null) {
				st.close();
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
