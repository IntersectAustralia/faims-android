package au.org.intersect.faims.android.util;

import java.util.Arrays;

import jsqlite.Callback;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CustomPolygon;
import au.org.intersect.faims.android.nutiteq.WKTUtil;

public class SpatialiteUtil {
	
private static String dbname;
	
	public static void setDatabaseName(String name) {
		dbname = name;
	}
	
	public static double computeArea(CustomPolygon polygon) throws Exception {
		jsqlite.Database db = null;
		try {
			db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
			String sql = "select area(transform(GeomFromText(\"" + WKTUtil.geometryToWKT(polygon) + "\", 4326), 28356));";
			
			final StringBuilder result = new StringBuilder();
			db.exec(sql, new Callback() {
				@Override
				public void columns(String[] coldata) {
					FLog.d("Columns: " + Arrays.toString(coldata));
				}
	
				@Override
				public void types(String[] types) {
					FLog.d("Types: " + Arrays.toString(types));
				}
	
				@Override
				public boolean newrow(String[] rowdata) {
					FLog.d("Row: " + Arrays.toString(rowdata));
	
					result.append(rowdata[0]);
					return false;
				}
			});
			
			return Float.parseFloat(result.toString());
		} finally {
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
