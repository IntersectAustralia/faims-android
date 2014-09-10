package au.org.intersect.faims.android.database;

import java.io.File;

import jsqlite.Stmt;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.util.DateUtil;

public class SharedRecord extends Database {

	protected String userId;

	public SharedRecord(File dbFile) {
		super(dbFile);
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return this.userId;
	}
	
	public boolean addReln(String entityId, String relationshipId, String verb) throws Exception {
		FLog.d("entityId:" + entityId);
		FLog.d("relationshipId:" + relationshipId);
		FLog.d("verb:" + verb);
		
		jsqlite.Database db = null;
		Stmt st = null;
		try {
			db = openDB(jsqlite.Constants.SQLITE_OPEN_READWRITE);
			beginTransaction(db);
			
			if (!hasEntity(db, entityId) || !hasRelationship(db, relationshipId)) {
				FLog.d("cannot add entity to relationship");
				return false;
			}
			
			String parenttimestamp = null;
			
			String query = DatabaseQueries.GET_AENT_RELN_PARENT_TIMESTAMP;
			st = db.prepare(query);
			st.bind(1, entityId);
			st.bind(2, relationshipId);
			if (st.step()) {
				parenttimestamp = st.column_string(0);
			}
			st.close();
			st = null;
			
			String currentTimestamp = DateUtil.getCurrentTimestampGMT();
			
			// create new entity relationship
			query = DatabaseQueries.INSERT_AENT_RELN;
			st = db.prepare(query);
			st.bind(1, entityId);
			st.bind(2, relationshipId);
			st.bind(3, userId);
			st.bind(4, clean(verb));
			st.bind(5, currentTimestamp);
			st.bind(6, parenttimestamp); 
			st.step();
			st.close();
			st = null;
			
			commitTransaction(db);
			notifyListeners();
			return true;
		} finally {
			closeStmt(st);
			closeDB(db);
		}
	}
	
	protected boolean hasRelationshipType(jsqlite.Database db, String relationshipType) throws Exception {
		Stmt st = null;
		try {
			st = db.prepare(DatabaseQueries.COUNT_RELN_TYPE);
			st.bind(1, relationshipType);
			st.step();
			if (st.column_int(0) == 0) {
				FLog.d("rel type does not exist");
				return false;
			}
		} finally {
			closeStmt(st);
		}
		return true;
	}
	
	protected boolean hasRelationship(jsqlite.Database db, String relationshipId) throws Exception {
		Stmt st = null;
		try {
			st = db.prepare(DatabaseQueries.COUNT_RELN);
			st.bind(1, relationshipId);
			st.step();
			if (st.column_int(0) == 0) {
				FLog.d("rel id " + relationshipId + " does not exist");
				return false;
			}
		} finally {
			closeStmt(st);
		}
		return true;
	}
	
	protected boolean hasEntityType(jsqlite.Database db, String entityType) throws Exception {
		Stmt st = null;
		try {
			st = db.prepare(DatabaseQueries.COUNT_ENTITY_TYPE);
			st.bind(1, entityType);
			st.step();
			if (st.column_int(0) == 0) {
				FLog.d("entity type does not exist");
				return false;
			}
		} finally {
			closeStmt(st);
		}
		return true;
	}
	
	protected boolean hasEntity(jsqlite.Database db, String entityId) throws Exception {
		Stmt st = null;
		try {
			st = db.prepare(DatabaseQueries.COUNT_ENTITY);
			st.bind(1, entityId);
			st.step();
			if (st.column_int(0) == 0) {
				FLog.d("entity id " + entityId + " does not exist");
				return false;
			}
		} finally {
			closeStmt(st);
		}
		return true;
	}
	
	public String generateUUID() {
		String s = userId;
		if (userId != null) {
			while (s.length() < 5) {
				s = "0" + s;
			}
			return "1"+ s + String.valueOf(System.currentTimeMillis());
		}
		return null;
	}

}
