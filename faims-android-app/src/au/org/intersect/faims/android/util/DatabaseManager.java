package au.org.intersect.faims.android.util;

import java.util.Arrays;
import java.util.List;

import jsqlite.Callback;
import jsqlite.Stmt;

public class DatabaseManager {

	private String dbname;

	public DatabaseManager(String filename) {
		this.dbname = filename;
	}

	public void saveArchEnt(String entity_id, String entity_type,
			String geo_data, List<?> attributes) {
		// TODO Auto-generated method stub
		try {
			jsqlite.Database db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
	
			// Callback used to display query results in Android LogCat
			Callback cb = new Callback() {
				@Override
				public void columns(String[] coldata) {
					FAIMSLog.log("Columns: " + Arrays.toString(coldata));
				}
	
				@Override
				public void types(String[] types) {
					FAIMSLog.log("Types: " + Arrays.toString(types));
				}
	
				@Override
				public boolean newrow(String[] rowdata) {
					FAIMSLog.log("Row: " + Arrays.toString(rowdata));
	
					return false;
				}
			};
	
			// Test prepare statements
			String query = "SELECT name, peoples, AsText(Geometry) from Towns where peoples > 350000";
			Stmt st = db.prepare(query);
			st.step();
			st.close();
	
			// Test various queries
			db.exec("select Distance(PointFromText('point(-77.35368 39.04106)', 4326), PointFromText('point(-77.35581 39.01725)', 4326));",
					cb);
			db.exec("SELECT name, peoples, AsText(Geometry), GeometryType(Geometry), NumPoints(Geometry), SRID(Geometry), IsValid(Geometry) from Towns where peoples > 350000;",
					cb);
			db.exec("SELECT Distance( Transform(MakePoint(4.430174797, 51.01047063, 4326), 32631), Transform(MakePoint(4.43001276, 51.01041585, 4326),32631));",
					cb);
	
			// Close the database
			db.close();
		} catch (Exception e) {
			FAIMSLog.log(e);
		}
	}
	
}
