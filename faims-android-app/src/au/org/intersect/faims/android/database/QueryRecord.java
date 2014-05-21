package au.org.intersect.faims.android.database;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jsqlite.Stmt;
import au.org.intersect.faims.android.nutiteq.WKTUtil;

import com.nutiteq.geometry.Geometry;

public class QueryRecord extends Database {

	public QueryRecord(File dbFile) {
		super(dbFile);
	}

	public List<String> runSelectionQuery(String sql, ArrayList<String> values) throws Exception {
		Stmt stmt = null;
		try {
			db = openDB();
			
			stmt = db.prepare(sql);
			for (int i = 0; i < values.size(); i++) {
				stmt.bind(i+1, "".equals(values.get(i)) ? null : values.get(i));
			}
			ArrayList<String> result = new ArrayList<String>();
			while(stmt.step()) {
				result.add(stmt.column_string(0));
			}
			return result;
		} finally {
			closeStmt(stmt);
			closeDB();
		}
	}

	public List<String> runLegacySelectionQuery(String dbPath,
			String tableName, String sql, ArrayList<String> values) throws Exception {
		Stmt stmt = null;
		try {
			db = openDB();
			
			stmt = db.prepare(sql);
			for (int i = 0; i < values.size(); i++) {
				stmt.bind(i+1, "".equals(values.get(i)) ? null : values.get(i));
			}
			ArrayList<String> result = new ArrayList<String>();
			while(stmt.step()) {
				result.add(dbPath + ":" + tableName + ":" + stmt.column_string(0));
			}
			return result;
		} finally {
			closeStmt(stmt);
			closeDB();
		}
	}

	public List<String> runDistanceEntityQuery(Geometry geometry, float distance, String srid) throws Exception {
		Stmt stmt = null;
		try {
			db = openDB();
			
			stmt = db.prepare(DatabaseQueries.RUN_DISTANCE_ENTITY);
			stmt.bind(1, WKTUtil.geometryToWKT(geometry));
			stmt.bind(2, Integer.parseInt(srid));
			stmt.bind(3, distance);
			stmt.bind(4, Integer.parseInt(srid));
			ArrayList<String> result = new ArrayList<String>();
			while(stmt.step()) {
				result.add(stmt.column_string(0));
			}
			return result;
		} finally {
			closeStmt(stmt);
			closeDB();
		}
	}
	
	public List<String> runDistanceRelationshipQuery(Geometry geometry, float distance, String srid) throws Exception {
		Stmt stmt = null;
		try {
			db = openDB();
			
			stmt = db.prepare(DatabaseQueries.RUN_DISTANCE_RELATIONSHIP);
			stmt.bind(1, WKTUtil.geometryToWKT(geometry));
			stmt.bind(2, Integer.parseInt(srid));
			stmt.bind(3, distance);
			stmt.bind(4, Integer.parseInt(srid));
			ArrayList<String> result = new ArrayList<String>();
			while(stmt.step()) {
				result.add(stmt.column_string(0));
			}
			return result;
		} finally {
			closeStmt(stmt);
			closeDB();
		}
	}

	public Collection<? extends String> runDistanceLegacyQuery(
			String dbPath, String tableName, String idColumn, String geometryColumn, Geometry geometry, float distance, String srid) throws Exception {
		Stmt stmt = null;
		try {
			db = openDB();
			
			stmt = db.prepare("select " + idColumn + " from " + tableName + " where "+ geometryColumn + " is not null and st_intersects(buffer(transform(GeomFromText(?, 4326), ?), ?), transform("+ geometryColumn + ", ?))");
			stmt.bind(1, WKTUtil.geometryToWKT(geometry));
			stmt.bind(2, Integer.parseInt(srid));
			stmt.bind(3, distance);
			stmt.bind(4, Integer.parseInt(srid));
			ArrayList<String> result = new ArrayList<String>();
			while(stmt.step()) {
				result.add(dbPath + ":" + tableName + ":" + stmt.column_string(0));
			}
			return result;
		} finally {
			closeStmt(stmt);
			closeDB();
		}
	}
	
	public List<String> runIntersectEntityQuery(Geometry geometry) throws Exception {
		Stmt stmt = null;
		try {
			db = openDB();
			
			stmt = db.prepare(DatabaseQueries.RUN_INTERSECT_ENTITY);
			stmt.bind(1, WKTUtil.geometryToWKT(geometry));
			ArrayList<String> result = new ArrayList<String>();
			while(stmt.step()) {
				result.add(stmt.column_string(0));
			}
			return result;
		} finally {
			closeStmt(stmt);
			closeDB();
		}
	}
	
	public List<String> runIntersectRelationshipQuery(Geometry geometry) throws Exception {
		Stmt stmt = null;
		try {
			db = openDB();
			
			stmt = db.prepare(DatabaseQueries.RUN_INTERSECT_RELATIONSHIP);
			stmt.bind(1, WKTUtil.geometryToWKT(geometry));
			ArrayList<String> result = new ArrayList<String>();
			while(stmt.step()) {
				result.add(stmt.column_string(0));
			}
			return result;
		} finally {
			closeStmt(stmt);
			closeDB();
		}
	}
	
	public Collection<? extends String> runIntersectLegacyQuery(
			String dbPath, String tableName, String idColumn, String geometryColumn, Geometry geometry) throws Exception {
		Stmt stmt = null;
		try {
			db = openDB();
			
			stmt = db.prepare("select " + idColumn + " from " + tableName + " where "+ geometryColumn + " is not null and st_intersects(GeomFromText(?, 4326), transform("+ geometryColumn + ", 4326))");
			stmt.bind(1, WKTUtil.geometryToWKT(geometry));
			ArrayList<String> result = new ArrayList<String>();
			while(stmt.step()) {
				result.add(dbPath + ":" + tableName + ":" + stmt.column_string(0));
			}
			return result;
		} finally {
			closeStmt(stmt);
			closeDB();
		}
	}
	
}
