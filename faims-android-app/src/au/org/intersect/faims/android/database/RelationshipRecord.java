package au.org.intersect.faims.android.database;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import jsqlite.Stmt;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.WKBUtil;
import au.org.intersect.faims.android.ui.view.Relationship;
import au.org.intersect.faims.android.ui.view.RelationshipAttribute;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.GeometryUtil;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.utils.Utils;
import com.nutiteq.utils.WkbRead;

public class RelationshipRecord extends SharedRecord {

	public RelationshipRecord(File dbFile) {
		super(dbFile);
	}

	public String updateRel(String uuid, String geometry) throws Exception {
		FLog.d("uuid:" + uuid);
		FLog.d("geometry:" + geometry);
		
		Stmt st = null;
		try {
			db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
			beginTransaction();
			
			String parenttimestamp = null;
			
			String query = DatabaseQueries.GET_RELATIONSHIP_PARENT_TIMESTAMP;
			st = db.prepare(query);
			st.bind(1, uuid);
			if (st.step()) {
				parenttimestamp = st.column_string(0);
			}
			st.close();
			st = null;
			
			query = DatabaseQueries.INSERT_AND_UPDATE_INTO_RELATIONSHIP;
			st = db.prepare(query);
			st.bind(1, userId);
			st.bind(2, clean(geometry));
			st.bind(3, parenttimestamp);
			st.bind(4, uuid);
			st.step();
			st.close();
			st = null;
			
			commitTransaction();
			notifyListeners();
			return uuid;
		} finally {
			closeStmt(st);
			closeDB();
		}
	}
	
	public void deleteRel(String relationshipId) throws jsqlite.Exception {
		FLog.d("relationshipId:" + relationshipId);
		
		Stmt st = null;
		try {
			if (relationshipId != null) {
				db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
				beginTransaction();
				
				String parenttimestamp = null;
				
				String query = DatabaseQueries.GET_RELATIONSHIP_PARENT_TIMESTAMP;
				st = db.prepare(query);
				st.bind(1, relationshipId);
				if (st.step()) {
					parenttimestamp = st.column_string(0);
				}
				st.close();
				st = null;
				
				query = DatabaseQueries.DELETE_RELN;
				st = db.prepare(query);
				st.bind(1, userId);
				st.bind(2, parenttimestamp);
				st.bind(3, relationshipId);
				st.step();
				st.close();
				st = null;
				
				commitTransaction();
				notifyListeners();
			}
		} finally {
			closeStmt(st);
			closeDB();
		}
	}
	
	public String saveRel(String relationshipId, String relationshipType,
			String geometry, List<RelationshipAttribute> attributes) throws Exception {
		FLog.d("relationshipId:" + relationshipId);
		FLog.d("relationshipType:" + relationshipType);
		FLog.d("geometry:" + geometry);
		
		if (attributes != null) {
			for (RelationshipAttribute attribute : attributes) {
				FLog.d(attribute.toString());
			}
		}
		
		Stmt st = null;
		try {
			db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
			beginTransaction();
			
			if (!validRel(db, relationshipId, relationshipType, geometry, attributes)) {
				FLog.d("relationship not valid");
				return null;
			}
			
			String uuid;
			
			if (relationshipId == null) {
				// create new relationship
				uuid = generateUUID();
			} else {
				uuid = relationshipId;
			}
			
			String parenttimestamp = null;
			
			String query = DatabaseQueries.GET_RELATIONSHIP_PARENT_TIMESTAMP;
			st = db.prepare(query);
			st.bind(1, uuid);
			if (st.step()) {
				parenttimestamp = st.column_string(0);
			}
			st.close();
			st = null;
			
			String currentTimestamp = DateUtil.getCurrentTimestampGMT();

			query = DatabaseQueries.INSERT_INTO_RELATIONSHIP;
			st = db.prepare(query);
			st.bind(1, uuid);
			st.bind(2, userId);
			st.bind(3, clean(geometry));
			st.bind(4, currentTimestamp);
			st.bind(5, parenttimestamp);
			st.bind(6, relationshipType);
			st.step();
			st.close();
			st = null;
			
			HashMap<String, String> cacheTimestamps = new HashMap<String, String>();
			// save relationship attributes
			if (attributes != null) {
				for (RelationshipAttribute attribute : attributes) {
					parenttimestamp = null;
					if (cacheTimestamps.containsKey(attribute.getName())) {
						parenttimestamp = cacheTimestamps.get(attribute.getName());
					} else {
						query = DatabaseQueries.GET_RELNVALUE_PARENT_TIMESTAMP;
						st = db.prepare(query);
						st.bind(1, uuid);
						st.bind(2, attribute.getName());
						if (st.step()) {
							parenttimestamp = st.column_string(0);
						}
						st.close();
						st = null;
						
						cacheTimestamps.put(attribute.getName(), parenttimestamp);
					}
					
					query = DatabaseQueries.INSERT_INTO_RELNVALUE;
					st = db.prepare(query);
					st.bind(1, uuid);
					st.bind(2, userId);
					st.bind(3, clean(attribute.getVocab()));
					st.bind(4, clean(attribute.getText()));
					st.bind(5, clean(attribute.getCertainty()));
					st.bind(6, currentTimestamp);
					st.bind(7, attribute.isDeleted() ? "true" : null);
					st.bind(8, parenttimestamp);
					st.bind(9, attribute.getName());
					st.step();
					st.close();
					st = null;
				}
			}
			
			commitTransaction();
			notifyListeners();
			return uuid;
		} finally {
			closeStmt(st);
			closeDB();
		}
	}

	private boolean validRel(jsqlite.Database db, String relationshipId, String relationshipType, String geometry, List<RelationshipAttribute> attributes) throws Exception {
		Stmt st = null;
		try {
			if (relationshipId == null && !hasRelationshipType(db, relationshipType)) {
				return false;
			} else if (relationshipId != null && !hasRelationship(db, relationshipId)) {
				return false;
			}
			
			// check if attributes exist
			if (attributes != null) {
				for (RelationshipAttribute attribute : attributes) {
					String query = DatabaseQueries.CHECK_VALID_RELN;
					st = db.prepare(query);
					st.bind(1, relationshipType);
					st.bind(2, attribute.getName());
					st.step();
					if (st.column_int(0) == 0) {
						return false;
					}
					st.close();
					st = null;
				}
			}
		} finally {
			if (st != null) st.close();
		}
		
		return true;
	}
	
	public Relationship fetchRel(String id) throws Exception {
		Stmt stmt = null;
		try {
			db = openDB();
			
			if (!hasRelationship(db, id)) {
				return null;
			}
			
			String query = DatabaseQueries.FETCH_RELN_VALUE;
			stmt = db.prepare(query);
			stmt.bind(1, id);
			Collection<RelationshipAttribute> attributes = new ArrayList<RelationshipAttribute>();
			while(stmt.step()){
				if (stmt.column_string(6) != null) continue; // don't return deleted attributes
				RelationshipAttribute relAttribute = new RelationshipAttribute();
				relAttribute.setName(stmt.column_string(1));
				relAttribute.setVocab(stmt.column_string(2));
				relAttribute.setText(stmt.column_string(3));
				relAttribute.setCertainty(stmt.column_string(4));
				relAttribute.setType(stmt.column_string(5));
				//relAttribute.setDeleted(stmt.column_string(6) != null ? true : false);
				relAttribute.setDirty(stmt.column_string(7) != null ? true : false);
				relAttribute.setDirtyReason(stmt.column_string(8));
				attributes.add(relAttribute);
			}
			stmt.close();
			stmt = null;
			
			for (RelationshipAttribute attribute : attributes) {
				FLog.d(attribute.toString());
			}
			
			// get vector geometry
			stmt = db.prepare(DatabaseQueries.FETCH_RELN_GEOSPATIALCOLUMN);
			stmt.bind(1, id);
			List<Geometry> geomList = new ArrayList<Geometry>();
			if(stmt.step()){
				Geometry[] gs = WKBUtil.cleanGeometry(WkbRead.readWkb(
	                    new ByteArrayInputStream(Utils
	                            .hexStringToByteArray(stmt.column_string(1))), (Object) null));
				if (gs != null) {
		            for (int i = 0; i < gs.length; i++) {
		            	Geometry g = gs[i];
		                geomList.add(GeometryUtil.fromGeometry(g));
		            }
				}
			}
			stmt.close();
			stmt = null;
			
			// check if forked
			stmt = db.prepare(DatabaseQueries.IS_RELATIONSHIP_FORKED);
			stmt.bind(1, id);
			stmt.step();
			boolean isforked = stmt.column_string(0) != null && Integer.parseInt(stmt.column_string(0)) > 0;
			stmt.close();
			stmt = null;
			if (!isforked) {
				stmt = db.prepare(DatabaseQueries.IS_RELNVALUE_FORKED);
				stmt.bind(1, id);
				stmt.step();
				isforked = stmt.column_string(0) != null && Integer.parseInt(stmt.column_string(0)) > 0;
				stmt.close();
				stmt = null;
			}
			
			Relationship relationship = new Relationship(id, null, attributes, geomList, isforked);

			return relationship;
		} finally {
			closeStmt(stmt);
			closeDB();
		}
	}
	
	public Geometry getBoundaryForVisibleRelnGeometry(String userQuery) throws Exception {
		Stmt stmt = null;
		Geometry result = null;
		try {
			db = openDB();
			
			if (userQuery == null) {
				userQuery = "";
			} else {
				userQuery =	"JOIN (" + userQuery + ") USING (relationshipid, relntimestamp)\n";
			}
			
			String query = DatabaseQueries.GET_BOUNDARY_OF_ALL_VISIBLE_RELN_GEOMETRY(userQuery);
			stmt = db.prepare(query);
			if(stmt.step()){
				Geometry[] gs = WKBUtil.cleanGeometry(WkbRead.readWkb(
	                    new ByteArrayInputStream(Utils
	                            .hexStringToByteArray(stmt.column_string(0))), (Object) null));
				if (gs != null) {
					result = GeometryUtil.fromGeometry(gs[0]);
				}
			}
			stmt.close();
			stmt = null;

			return result;
		} finally {
			closeStmt(stmt);
			closeDB();
		}
	}
	
}
