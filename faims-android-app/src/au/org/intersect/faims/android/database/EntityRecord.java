package au.org.intersect.faims.android.database;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import jsqlite.Stmt;
import au.org.intersect.faims.android.data.ArchEntity;
import au.org.intersect.faims.android.data.EntityAttribute;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.WKBUtil;
import au.org.intersect.faims.android.util.DateUtil;
import au.org.intersect.faims.android.util.GeometryUtil;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.utils.Utils;
import com.nutiteq.utils.WkbRead;

public class EntityRecord extends SharedRecord {

	public EntityRecord(File dbFile) {
		super(dbFile);
	}

	public void deleteArchEnt(String entityId) throws jsqlite.Exception {
		FLog.d("entityId:" + entityId);
		
		Stmt st = null;
		try {
			if (entityId != null) {
				db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);	
				beginTransaction();
				
				String parenttimestamp = null;
				
				String query = DatabaseQueries.GET_ARCH_ENT_PARENT_TIMESTAMP;
				st = db.prepare(query);
				st.bind(1, entityId);
				if (st.step()) {
					parenttimestamp = st.column_string(0);
				}
				st.close();
				st = null;
				
				query = DatabaseQueries.DELETE_ARCH_ENT;
				st = db.prepare(query);
				st.bind(1, userId);
				st.bind(2, parenttimestamp);
				st.bind(3, entityId);
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

	public String saveArchEnt(String entityId, String entityType,
			String geometry, List<EntityAttribute> attributes) throws Exception {
		FLog.d("entityId:" + entityId);
		FLog.d("entityType:" + entityType);
		FLog.d("geometry:" + geometry);
		
		if (attributes != null) {
			for (EntityAttribute attribute : attributes) {
				FLog.d(attribute.toString());
			}
		}
		
		Stmt st = null;
		try {
			db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
			beginTransaction();
			
			if (!validArchEnt(db, entityId, entityType, geometry, attributes)) {
				FLog.d("arch entity not valid");
				return null;
			}
			
			String uuid;
			
			if (entityId == null) {
				// create new entity
				uuid = generateUUID();
			} else {
				// update entity
				uuid = entityId;
			}
			
			String parenttimestamp = null;
			
			String query = DatabaseQueries.GET_ARCH_ENT_PARENT_TIMESTAMP;
			st = db.prepare(query);
			st.bind(1, uuid);
			if (st.step()) {
				parenttimestamp = st.column_string(0);
			}
			st.close();
			st = null;
			
			String currentTimestamp = DateUtil.getCurrentTimestampGMT();
			
			query = DatabaseQueries.INSERT_INTO_ARCHENTITY;
			st = db.prepare(query);
			st.bind(1, uuid);
			st.bind(2, userId);
			st.bind(3, clean(geometry));
			st.bind(4, currentTimestamp);
			st.bind(5, parenttimestamp);
			st.bind(6, entityType);
			st.step();
			st.close();
			st = null;
			
			HashMap<String, String> cacheTimestamps = new HashMap<String, String>();
			// save entity attributes
			if (attributes != null) {
				for (EntityAttribute attribute : attributes) {
					parenttimestamp = null;
					if (cacheTimestamps.containsKey(attribute.getName())) {
						parenttimestamp = cacheTimestamps.get(attribute.getName());
					} else {
						query = DatabaseQueries.GET_AENTVALUE_PARENT_TIMESTAMP;
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
					
					query = DatabaseQueries.INSERT_INTO_AENTVALUE;
					st = db.prepare(query);
					st.bind(1, uuid);
					st.bind(2, userId);
					st.bind(3, clean(attribute.getVocab()));
					st.bind(4, clean(attribute.getMeasure()));
					st.bind(5, clean(attribute.getText()));
					st.bind(6, clean(attribute.getCertainty()));
					st.bind(7, currentTimestamp);
					st.bind(8, attribute.isDeleted() ? "true" : null);
					st.bind(9, parenttimestamp);
					st.bind(10, attribute.getName());
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
	
	public String updateArchEnt(String uuid, String geometry) throws Exception {
		FLog.d("uuid:" + uuid);
		FLog.d("geometry:" + geometry);
		
		Stmt st = null;
		try {
			db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
			beginTransaction();
			
			String parenttimestamp = null;
			
			String query = DatabaseQueries.GET_ARCH_ENT_PARENT_TIMESTAMP;
			st = db.prepare(query);
			st.bind(1, uuid);
			if (st.step()) {
				parenttimestamp = st.column_string(0);
			}
			st.close();
			st = null;
			
			query = DatabaseQueries.INSERT_AND_UPDATE_INTO_ARCHENTITY;
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
	
	private boolean validArchEnt(jsqlite.Database db, String entityId, String entityType, String geometry, List<EntityAttribute> attributes) throws Exception {
		Stmt st = null;
		try {
			if (entityId == null && !hasEntityType(db, entityType)) {
				return false;
			} else if (entityId != null && !hasEntity(db, entityId)) {
				return false;
			}
			
			// check if attributes exist
			if (attributes != null) {
				for (EntityAttribute attribute : attributes) {
					String query = DatabaseQueries.CHECK_VALID_AENT;
					
					st = db.prepare(query);
					st.bind(1, entityType);
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

	public ArchEntity fetchArchEnt(String id) throws Exception {
		Stmt stmt = null;
		try {
			db = openDB();
			
			if (!hasEntity(db, id)) {
				return null;
			}

			String query = DatabaseQueries.FETCH_AENT_VALUE;
			stmt = db.prepare(query);
			stmt.bind(1, id);
			Collection<EntityAttribute> attributes = new ArrayList<EntityAttribute>();
			while(stmt.step()){
				if (stmt.column_string(7) != null) continue; // don't return deleted attributes
				EntityAttribute archAttribute = new EntityAttribute();
				archAttribute.setName(stmt.column_string(1));
				archAttribute.setVocab(stmt.column_string(2));
				archAttribute.setMeasure(stmt.column_string(3));
				archAttribute.setText(stmt.column_string(4));
				archAttribute.setCertainty(stmt.column_string(5));
				archAttribute.setType(stmt.column_string(6));
				//archAttribute.setDeleted(stmt.column_string(7) != null ? true : false);
				archAttribute.setDirty(stmt.column_string(8) != null ? true : false);
				archAttribute.setDirtyReason(stmt.column_string(9));
				attributes.add(archAttribute);
			}
			stmt.close();
			stmt = null;
			
			for (EntityAttribute attribute : attributes) {
				FLog.d(attribute.toString());
			}
			
			// get vector geometry
			stmt = db.prepare(DatabaseQueries.FETCH_ARCHENTITY_GEOSPATIALCOLUMN);
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
			stmt = db.prepare(DatabaseQueries.IS_ARCH_ENTITY_FORKED);
			stmt.bind(1, id);
			stmt.step();
			boolean isforked = stmt.column_string(0) != null && Integer.parseInt(stmt.column_string(0)) > 0;
			stmt.close();
			stmt = null;
			if (!isforked) {
				stmt = db.prepare(DatabaseQueries.IS_AENTVALUE_FORKED);
				stmt.bind(1, id);
				stmt.step();
				isforked = stmt.column_string(0) != null && Integer.parseInt(stmt.column_string(0)) > 0;
				stmt.close();
				stmt = null;
			}
			ArchEntity archEntity = new ArchEntity(id, null, attributes, geomList, isforked);
			return archEntity;
		} finally {
			closeStmt(stmt);
			closeDB();
		}
	}
	
	public Geometry getBoundaryForVisibleEntityGeometry(String userQuery) throws Exception {
		Stmt stmt = null;
		Geometry result = null;
		try {
			db = openDB();
			
			if (userQuery == null) {
				userQuery = "";
			} else {
				userQuery =	"JOIN (" + userQuery + ") USING (uuid, aenttimestamp)\n";
			}
			
			String query = DatabaseQueries.GET_BOUNDARY_OF_ALL_VISIBLE_ENTITY_GEOMETRY(userQuery);
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
