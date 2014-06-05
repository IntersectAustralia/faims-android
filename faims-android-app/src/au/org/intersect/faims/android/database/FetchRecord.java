package au.org.intersect.faims.android.database;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import jsqlite.Stmt;
import au.org.intersect.faims.android.data.User;
import au.org.intersect.faims.android.nutiteq.WKBUtil;
import au.org.intersect.faims.android.util.GeometryUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.utils.Utils;
import com.nutiteq.utils.WkbRead;

public class FetchRecord extends Database {

	public FetchRecord(File dbFile) {
		super(dbFile);
	}
	
	public Object fetchOne(String query) throws Exception {
		jsqlite.Database db = null;
		Stmt stmt = null;
		try {
			db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
			beginTransaction(db);
			
			stmt = db.prepare(query);
			Collection<String> results = new ArrayList<String>();
			if(stmt.step()){
				for(int i = 0; i < stmt.column_count(); i++){
					results.add(stmt.column_string(i));
				}
			}
			stmt.close();
			stmt = null;
			
			commitTransaction(db);
			return results;
		} finally {
			closeStmt(stmt);
			closeDB(db);
		}
	}

	public Collection<List<String>> fetchAll(String query) throws Exception {
		jsqlite.Database db = null;
		Stmt stmt = null;
		try {
			db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
			beginTransaction(db);
			
			stmt = db.prepare(query);
			Collection<List<String>> results = new ArrayList<List<String>>();
			while(stmt.step()){
				List<String> result = new ArrayList<String>();
				for(int i = 0; i < stmt.column_count(); i++){
					result.add(stmt.column_string(i));
				}
				results.add(result);
			}
			stmt.close();
			stmt = null;

			commitTransaction(db);
			return results;
		} finally {
			closeStmt(stmt);
			closeDB(db);
		}
	}

	public List<User> fetchAllUser() throws Exception{
		Collection<List<String>> users = fetchAll("select userid, fname, lname, email from user");
		List<User> userList = new ArrayList<User>();
		for(List<String> userData : users){
			User user = new User(userData.get(0), userData.get(1), userData.get(2), userData.get(3));
			userList.add(user);
		}
		return userList;
	}
	
	public Collection<List<String>> fetchEntityList(String type) throws Exception {
		String query = DatabaseQueries.FETCH_ENTITY_LIST(type);
		return fetchAll(query);
	}
	
	public Vector<Geometry> fetchVisibleGPSTrackingForUser(List<MapPos> list, int maxObjects, String querySql, String userid) throws Exception{
		if(querySql == null){
			return null;
		}

		String s = userid;
		while (s.length() < 5) {
			s = "0" + s;
		}
		String uuidForUser = "1" + s;
		
		Vector<Geometry> geometries = fetchAllVisibleEntityGeometry(list, querySql, maxObjects);
		Vector<Geometry> userGeometries = new Vector<Geometry>();
		for (Geometry geometry : geometries) {
			String[] userData = (String[]) geometry.userData;
			if(userData[0].startsWith(uuidForUser)){
				userGeometries.add(geometry);
			}
		}
		return userGeometries;
	}
	
	public Vector<Geometry> fetchAllVisibleEntityGeometry(List<MapPos> list, String userQuery, int maxObjects) throws Exception {
		jsqlite.Database db = null;
		Stmt stmt = null;
		try {
			db = openDB();

			if (userQuery == null) {
				userQuery = "";
			} else {
				userQuery =	"JOIN (" + userQuery + ") USING (uuid, aenttimestamp)\n";
			}
			
			String query = DatabaseQueries.FETCH_ALL_VISIBLE_ENTITY_GEOMETRY(userQuery);
			stmt = db.prepare(query);
			stmt.bind(1, list.get(0).x);
			stmt.bind(2, list.get(0).y);
			stmt.bind(3, list.get(2).x);
			stmt.bind(4, list.get(2).y);
			stmt.bind(5, maxObjects);
			Vector<Geometry> results = new Vector<Geometry>();
			while(stmt.step()){
				String uuid = stmt.column_string(0);
				String response = stmt.column_string(1);
				Geometry[] gs = WKBUtil.cleanGeometry(WkbRead.readWkb(
	                    new ByteArrayInputStream(Utils
	                            .hexStringToByteArray(stmt.column_string(2))), (Object) null));
				if (gs != null) {
		            for (int i = 0; i < gs.length; i++) {
		            	Geometry g = gs[i];
		            	g.userData = new String[] { uuid, response };
		                results.add(GeometryUtil.fromGeometry(g));
		            }
				}
			}
			stmt.close();
			stmt = null;

			return results;
		} finally {
			closeStmt(stmt);
			closeDB(db);
		}
	}
	
	public Collection<List<String>> fetchRelationshipList(String type) throws Exception {
		String query = DatabaseQueries.FETCH_RELN_LIST(type);
		return fetchAll(query);
	}
	
	public Vector<Geometry> fetchAllVisibleRelationshipGeometry(List<MapPos> list, String userQuery, int maxObjects) throws Exception {
		jsqlite.Database db = null;
		Stmt stmt = null;
		try {
			db = openDB();
			
			if (userQuery == null) {
				userQuery = "";
			} else {
				userQuery =	"JOIN (" + userQuery + ") USING (relationshipid, relntimestamp)\n";
			}
			
			String query = DatabaseQueries.FETCH_ALL_VISIBLE_RELN_GEOMETRY(userQuery);
			stmt = db.prepare(query);
			stmt.bind(1, list.get(0).x);
			stmt.bind(2, list.get(0).y);
			stmt.bind(3, list.get(2).x);
			stmt.bind(4, list.get(2).y);
			stmt.bind(5, maxObjects);
			Vector<Geometry> results = new Vector<Geometry>();
			while(stmt.step()){
				String uuid = stmt.column_string(0);
				String response = stmt.column_string(1);
				Geometry[] gs = WKBUtil.cleanGeometry(WkbRead.readWkb(
	                    new ByteArrayInputStream(Utils
	                            .hexStringToByteArray(stmt.column_string(2))), (Object) null));
				if (gs != null) {
		            for (int i = 0; i < gs.length; i++) {
		            	Geometry g = gs[i];
		            	g.userData = new String[] { uuid, response };
		                results.add(GeometryUtil.fromGeometry(g));
		            }
				}
			}
			stmt.close();
			stmt = null;

			return results;
		} finally {
			closeStmt(stmt);
			closeDB(db);
		}
	}
	
	public boolean hasRecordsFrom(String timestamp) throws Exception {
		jsqlite.Database db = null;
		Stmt stmt = null;
		try {
			db = openDB();
			
			int count = 0;
			
			String query = DatabaseQueries.COUNT_AENT_RECORDS(timestamp);
			stmt = db.prepare(query);
			if(stmt.step()){
				count = stmt.column_int(0);
			}
			stmt.close();
			stmt = null;
			
			if(count > 0){
				return true;
			}
			
			query = DatabaseQueries.COUNT_RELN_RECORDS(timestamp);
			stmt = db.prepare(query);
			if(stmt.step()){
				count = stmt.column_int(0);
			}
			stmt.close();
			stmt = null;
			
			if(count > 0){
				return true;
			}
			
			query = DatabaseQueries.COUNT_AENT_RELN_RECORDS(timestamp);
			stmt = db.prepare(query);
			if(stmt.step()){
				count = stmt.column_int(0);
			}
			stmt.close();
			stmt = null;
			
			if(count > 0){
				return true;
			}
			
			return false;
		} finally {
			closeStmt(stmt);
			closeDB(db);
		}
	}

}
