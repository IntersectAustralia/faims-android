package au.org.intersect.faims.android.managers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jsqlite.Callback;
import jsqlite.Database;
import jsqlite.Stmt;
import android.util.Log;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.form.ArchEntity;
import au.org.intersect.faims.android.ui.form.EntityAttribute;
import au.org.intersect.faims.android.ui.form.Relationship;
import au.org.intersect.faims.android.ui.form.RelationshipAttribute;
import au.org.intersect.faims.android.util.FAIMSLog;

import com.google.inject.Singleton;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.utils.Utils;
import com.nutiteq.utils.WkbRead;

@Singleton
public class DatabaseManager {

	private String dbname;
	private String userId;

	public void init(String filename) {
		this.dbname = filename;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getUserId() {
		return this.userId;
	}

	public String saveArchEnt(String entity_id, String entity_type,
			String geo_data, List<EntityAttribute> attributes) {
		synchronized(DatabaseManager.class) {
			FAIMSLog.log("entity_id:" + entity_id);
			FAIMSLog.log("entity_type:" + entity_type);
			
			for (EntityAttribute attribute : attributes) {
				FAIMSLog.log(attribute.toString());
			}
			
			jsqlite.Database db = null;
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				if (!validArchEnt(db, entity_id, entity_type, geo_data, attributes)) {
					FAIMSLog.log("not valid arch entity");
					return null;
				}
				
				String uuid;
				Stmt st;
				
				if (entity_id == null) {
					
					// create new entity
					uuid = generateUUID();
					
				} else {
					
					// update entity
					uuid = entity_id;
					
				}
				
				String query = "INSERT INTO ArchEntity (uuid, userid, AEntTypeID, GeoSpatialColumn, AEntTimestamp) " +
									"SELECT cast(? as integer), ?, aenttypeid, GeomFromText(?, 4326), CURRENT_TIMESTAMP " +
									"FROM aenttype " + 
									"WHERE aenttypename = ?;";
				st = db.prepare(query);
				st.bind(1, uuid);
				st.bind(2, userId);
				st.bind(3, geo_data);
				st.bind(4, entity_type);
				st.step();
				st.close();
				
				// save entity attributes
				for (EntityAttribute attribute : attributes) {
					query = "INSERT INTO AEntValue (uuid, VocabID, AttributeID, Measure, FreeText, Certainty, ValueTimestamp) " +
								   "SELECT cast(? as integer), ?, attributeID, ?, ?, ?, CURRENT_TIMESTAMP " + 
								   "FROM AttributeKey " + 
								   "WHERE attributeName = ? COLLATE NOCASE;";
					st = db.prepare(query);
					st.bind(1, uuid);
					st.bind(2, attribute.getVocab());
					st.bind(3, attribute.getMeasure());
					st.bind(4, attribute.getText());
					st.bind(5, attribute.getCertainty());
					st.bind(6, attribute.getName());
					st.step();
					st.close();
				}
				
				//debugSaveArchEnt(db, uuid);
				
				return uuid;
				
			} catch (Exception e) {
				FAIMSLog.log(e);
			} finally {
				try {
					if (db != null) db.close();
				} catch (Exception e) {
					FAIMSLog.log(e);
				}
			}
			
			return null;
		}
	}
	
	public String saveRel(String rel_id, String rel_type,
			String geo_data, List<RelationshipAttribute> attributes) {
		synchronized(DatabaseManager.class) {
			FAIMSLog.log("rel_id:" + rel_id);
			FAIMSLog.log("rel_type:" + rel_type);
			
			for (RelationshipAttribute attribute : attributes) {
				FAIMSLog.log(attribute.toString());
			}
			
			jsqlite.Database db = null;
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				if (!validRel(db, rel_id, rel_type, geo_data, attributes)) {
					FAIMSLog.log("not valid rel");
					return null;
				}
				
				String uuid;
				Stmt st;
				
				if (rel_id == null) {
					// create new relationship
					uuid = generateUUID();
					
				} else {
					
					uuid = rel_id;
				}
				
				String query = "INSERT INTO Relationship (RelationshipID, userid, RelnTypeID, GeoSpatialColumn, RelnTimestamp) " +
									"SELECT cast(? as integer), ?, relntypeid, GeomFromText(?, 4326), CURRENT_TIMESTAMP " +
									"FROM relntype " +
									"WHERE relntypename = ?;";
				st = db.prepare(query);
				st.bind(1, uuid);
				st.bind(2, userId);
				st.bind(3, geo_data);
				st.bind(4, rel_type);
				st.step();
				st.close();
				
				// save relationship attributes
				for (RelationshipAttribute attribute : attributes) {
					query = "INSERT INTO RelnValue (RelationshipID, VocabID, AttributeID, FreeText, RelnValueTimestamp) " +
								   "SELECT cast(? as integer), ?, attributeId, ?, CURRENT_TIMESTAMP " + 
								   "FROM AttributeKey " + 
								   "WHERE attributeName = ? COLLATE NOCASE;";
					st = db.prepare(query);
					st.bind(1, uuid);
					st.bind(2, attribute.getVocab());
					st.bind(3, attribute.getText());
					st.bind(4, attribute.getName());
					st.step();
					st.close();
				}
	
				//debugSaveRel(db, uuid);
				
				return uuid;
				
			} catch (Exception e) {
				FAIMSLog.log(e);
			} finally {
				try {
					if (db != null) db.close();
				} catch (Exception e) {
					FAIMSLog.log(e);
				}
			}
			
			return null;
		}
	}
	
	private boolean validArchEnt(jsqlite.Database db, String entity_id, String entity_type, String geo_data, List<EntityAttribute> attributes) throws Exception {
		
		if (entity_id == null && !hasEntityType(db, entity_type)) {
			return false;
		} else if (entity_id != null && !hasEntity(db, entity_id)) {
			return false;
		}
		
		// check if attributes exist
		for (EntityAttribute attribute : attributes) {
			String query = "SELECT count(AEntTypeName) " + 
						   "FROM IdealAEnt left outer join AEntType using (AEntTypeId) left outer join AttributeKey using (AttributeId) " + 
						   "WHERE AEntTypeName = ? COLLATE NOCASE and AttributeName = ? COLLATE NOCASE;";
			
			Stmt st = db.prepare(query);
			st.bind(1, entity_type);
			st.bind(2, attribute.getName());
			st.step();
			if (st.column_int(0) == 0) {
				st.close();
				return false;
			}
			st.close();
		}
		
		return true;
	}
	
	private boolean validRel(jsqlite.Database db, String rel_id, String rel_type, String geo_data, List<RelationshipAttribute> attributes) throws Exception {
		
		if (rel_id == null && !hasRelationshipType(db, rel_type)) {
			return false;
		} else if (rel_id != null && !hasRelationship(db, rel_id)) {
			return false;
		}
		
		// check if attributes exist
		for (RelationshipAttribute attribute : attributes) {
			String query = "SELECT count(RelnTypeName) " + 
					   	   "FROM IdealReln left outer join RelnType using (RelnTypeID) left outer join AttributeKey using (AttributeId) " + 
					       "WHERE RelnTypeName = ? COLLATE NOCASE and AttributeName = ? COLLATE NOCASE;";
			Stmt st = db.prepare(query);
			st.bind(1, rel_type);
			st.bind(2, attribute.getName());
			st.step();
			if (st.column_int(0) == 0) {
				st.close();
				return false;
			}
			st.close();
		}
		
		return true;
	}
	
	public boolean addReln(String entity_id, String rel_id, String verb) {
		synchronized(DatabaseManager.class) {
			FAIMSLog.log("entity_id:" + entity_id);
			FAIMSLog.log("rel_id:" + rel_id);
			FAIMSLog.log("verb:" + verb);
			
			jsqlite.Database db = null;
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				if (!hasEntity(db, entity_id) || !hasRelationship(db, rel_id)) {
					return false;
				}
				
				// create new entity relationship
				String query = "INSERT INTO AEntReln (UUID, RelationshipID, ParticipatesVerb, AEntRelnTimestamp) " +
							   "VALUES (?, ?, ?, CURRENT_TIMESTAMP);";
				Stmt st = db.prepare(query);
				st.bind(1, entity_id);
				st.bind(2, rel_id);
				st.bind(3, verb);
				st.step();
				st.close();
				
				FAIMSLog.log("test");
				
				debugAddReln(db, entity_id, rel_id);
				
				return true;
				
			} catch (Exception e) {
				FAIMSLog.log(e);
			} finally {
				try {
					if (db != null) db.close();
				} catch (Exception e) {
					FAIMSLog.log(e);
				}
			}
			
			return false;
		}
	}

	public Object fetchArchEnt(String id){
		synchronized(DatabaseManager.class) {
			try {
				
				jsqlite.Database db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				if (!hasEntity(db, id)) {
					return null;
				}
	
				String query = "SELECT uuid, attributename, vocabid, measure, freetext, certainty, AEntTypeID FROM (SELECT uuid, attributeid, vocabid, measure, freetext, certainty, valuetimestamp FROM aentvalue WHERE uuid || valuetimestamp || attributeid in (SELECT uuid || max(valuetimestamp) || attributeid FROM aentvalue WHERE uuid = ? GROUP BY uuid, attributeid)) JOIN attributekey USING (attributeid) JOIN ArchEntity USING (uuid);";
				Stmt stmt = db.prepare(query);
				stmt.bind(1, id);
				Collection<EntityAttribute> attributes = new ArrayList<EntityAttribute>();
				String type = null;
				while(stmt.step()){
					type = stmt.column_string(6);
					EntityAttribute archAttribute = new EntityAttribute();
					archAttribute.setName(stmt.column_string(1));
					archAttribute.setVocab(Integer.toString(stmt.column_int(2)));
					archAttribute.setMeasure(Double.toString(stmt.column_double(3)));
					archAttribute.setText(stmt.column_string(4));
					archAttribute.setCertainty(Double.toString(stmt.column_double(5)));
					attributes.add(archAttribute);
				}
				
				// get vector geometry
				stmt = db.prepare("SELECT uuid, HEX(AsBinary(GeoSpatialColumn)) from ArchEntity where uuid || aenttimestamp IN ( SELECT uuid || max(aenttimestamp) FROM archentity WHERE uuid = ?);");
				stmt.bind(1, id);
				List<Geometry> geomList = new ArrayList<Geometry>();
				if(stmt.step()){
					Geometry[] g1 = WkbRead.readWkb(
		                    new ByteArrayInputStream(Utils
		                            .hexStringToByteArray(stmt.column_string(1))), null);
					if (g1 != null) {
			            for (int i = 0; i < g1.length; i++) {
			                geomList.add(GeometryUtil.fromGeometry(g1[i]));
			            }
					}
				}
	
				ArchEntity archEntity = new ArchEntity(id, type, attributes, geomList);
				
				db.close();
	
				return archEntity;
			} catch (Exception e) {
				FAIMSLog.log(e);
			}
			return null;
		}
	}
	
	public Object fetchRel(String id){
		synchronized(DatabaseManager.class) {
			try {
				jsqlite.Database db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
				if (!hasRelationship(db, id)) {
					return null;
				}
				
				String query = "SELECT relationshipid, attributename, vocabid, freetext, relntypeid FROM (SELECT relationshipid, attributeid, vocabid, freetext FROM relnvalue WHERE relationshipid || relnvaluetimestamp || attributeid in (SELECT relationshipid || max(relnvaluetimestamp) || attributeid FROM relnvalue WHERE relationshipid = ? GROUP BY relationshipid, attributeid)) JOIN attributekey USING (attributeid) JOIN Relationship USING (relationshipid);";
				Stmt stmt = db.prepare(query);
				stmt.bind(1, id);
				Collection<RelationshipAttribute> attributes = new ArrayList<RelationshipAttribute>();
				String type = null;
				while(stmt.step()){
					type = stmt.column_string(4);
					RelationshipAttribute relAttribute = new RelationshipAttribute();
					relAttribute.setName(stmt.column_string(1));
					relAttribute.setVocab(Integer.toString(stmt.column_int(2)));
					relAttribute.setText(stmt.column_string(3));
					attributes.add(relAttribute);
				}
				
				// get vector geometry
				stmt = db.prepare("SELECT relationshipid, HEX(AsBinary(GeoSpatialColumn)) from relationship where relationshipid || relntimestamp IN ( SELECT relationshipid || max(relntimestamp) FROM relationship WHERE relationshipid = ?);");
				stmt.bind(1, id);
				List<Geometry> geomList = new ArrayList<Geometry>();
				if(stmt.step()){
					Geometry[] g1 = WkbRead.readWkb(
		                    new ByteArrayInputStream(Utils
		                            .hexStringToByteArray(stmt.column_string(1))), null);
					if (g1 != null) {
			            for (int i = 0; i < g1.length; i++) {
			                geomList.add(GeometryUtil.fromGeometry(g1[i]));
			            }
					}
				}
				
				Relationship relationship = new Relationship(id, type, attributes, geomList);
				
				db.close();
	
				return relationship;
			} catch (Exception e) {
				FAIMSLog.log(e);
			}
			return null;
		}
	}

	public Object fetchOne(String query){
		synchronized(DatabaseManager.class) {
			try {
				jsqlite.Database db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				Stmt stmt = db.prepare(query);
				Collection<String> results = new ArrayList<String>();
				if(stmt.step()){
					for(int i = 0; i < stmt.column_count(); i++){
						results.add(stmt.column_string(i));
					}
				}
				db.close();
	
				return results;
			} catch (jsqlite.Exception e) {
				FAIMSLog.log(e);
			}
			return null;
		}
	}

	public Collection<List<String>> fetchAll(String query){
		synchronized(DatabaseManager.class) {
			try {
				jsqlite.Database db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				Stmt stmt = db.prepare(query);
				Collection<List<String>> results = new ArrayList<List<String>>();
				while(stmt.step()){
					List<String> result = new ArrayList<String>();
					for(int i = 0; i < stmt.column_count(); i++){
						result.add(stmt.column_string(i));
					}
					results.add(result);
				}
				db.close();
	
				return results;
			} catch (jsqlite.Exception e) {
				FAIMSLog.log(e);
			}
			return null;
		}
	}
	
	private boolean hasEntityType(jsqlite.Database db, String entity_type) throws Exception {
		Stmt st = db.prepare("select count(AEntTypeID) from AEntType where AEntTypeName = ? COLLATE NOCASE;");
		st.bind(1, entity_type);
		st.step();
		if (st.column_int(0) == 0) {
			FAIMSLog.log("entity type does not exist");
			st.close();
			return false;
		}
		st.close();
		return true;
	}
	
	private boolean hasEntity(jsqlite.Database db, String entity_id) throws Exception {
		Stmt st = db.prepare("select count(UUID) from ArchEntity where UUID = ?;");
		st.bind(1, entity_id);
		st.step();
		if (st.column_int(0) == 0) {
			FAIMSLog.log("entity id " + entity_id + " does not exist");
			st.close();
			return false;
		}
		st.close();
		return true;
	}
	
	/*
	private void debugSaveArchEnt(jsqlite.Database db, String uuid) throws Exception {

		// Test various queries
		db.exec("select uuid, AsText(GeoSpatialColumn) from ArchEntity;", createCallback());
		db.exec("select uuid, valuetimestamp, attributename, freetext, vocabid, measure, certainty from aentvalue left outer join attributekey using (attributeid) where uuid || valuetimestamp || attributeid in (select uuid || max(valuetimestamp) || attributeid from aentvalue group by uuid, attributeid);", createCallback());
		//db.exec("select attributeid, valuetimestamp from aentvalue where uuid="+uuid+";", cb);
	}
	*/
	
	private boolean hasRelationshipType(jsqlite.Database db, String rel_type) throws Exception {
		Stmt st = db.prepare("select count(RelnTypeID) from RelnType where RelnTypeName = ? COLLATE NOCASE;");
		st.bind(1, rel_type);
		st.step();
		if (st.column_int(0) == 0) {
			FAIMSLog.log("rel type does not exist");
			st.close();
			return false;
		}
		st.close();
		return true;
	}
	
	private boolean hasRelationship(jsqlite.Database db, String rel_id) throws Exception {
		Stmt st = db.prepare("select count(RelationshipID) from Relationship where RelationshipID = ?;");
		st.bind(1, rel_id);
		st.step();
		if (st.column_int(0) == 0) {
			FAIMSLog.log("rel id " + rel_id + " does not exist");
			st.close();
			return false;
		}
		st.close();
		return true;
	}
	
	/*
	private void debugSaveRel(jsqlite.Database db, String uuid) throws Exception {
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
		
		// Test various queries
		db.exec("select RelationshipID, AsText(GeoSpatialColumn) from Relationship;", cb);
		db.exec("select attributename, vocabname, freetext, RelnValueTimestamp " + 
				"from RelnValue " +
				"left outer join attributekey using (attributeid) " + 
				"left outer join vocabulary using (attributeid) " +
				"where RelationshipID = " + uuid + " group by RelationshipID, attributeid having max(RelnValueTimestamp);", cb);
	}
	*/
	
	private Callback createCallback() {
		return new Callback() {
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
	}

	private void debugAddReln(Database db, String entity_id, String rel_id) throws Exception {
		
		// Test various queries
		db.exec("select count(UUID) from AEntReln;", createCallback());
		db.exec("select UUID, RelationshipID, ParticipatesVerb " + 
				"from AEntReln " +
				"where uuid = " + entity_id + " and RelationshipID = " + rel_id + ";", createCallback());
	}
	
	private String generateUUID() {
		String s = userId;
		while (s.length() < 5) {
			s = "0" + s;
		}
		return "1"+ s + String.valueOf(System.currentTimeMillis());
	}

	public void dumpDatabaseTo(File file) throws jsqlite.Exception {
		synchronized(DatabaseManager.class) {
			Log.d("FAIMS", "dumping database to " + file.getAbsolutePath());
			jsqlite.Database db = null;
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
	
				String query = 
							"attach database '" + file.getAbsolutePath() + "' as export;" +
							"create table export.archentity as select * from archentity;" +
							"create table export.aentvalue as select * from aentvalue;" +
							"create table export.aentreln as select * from aentreln;" + 
							"create table export.relationship as select * from relationship;" +
							"create table export.relnvalue as select * from relnvalue;" +
							"detach database export;";
				db.exec(query, createCallback());
				
			} finally {
				try {
					if (db != null) db.close();
				} catch (Exception e) {
					FAIMSLog.log(e);
				}
			}
		}
	}
	
	public void dumpDatabaseTo(File file, String fromTimestamp) throws jsqlite.Exception {
		synchronized(DatabaseManager.class) {
			Log.d("FAIMS", "dumping database to " + file.getAbsolutePath());
			jsqlite.Database db = null;
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
	
				String query = 
							"attach database '" + file.getAbsolutePath() + "' as export;" +
							"create table export.archentity as select * from archentity where aenttimestamp > '" + fromTimestamp + "';" +
							"create table export.aentvalue as select * from aentvalue where valuetimestamp > '" + fromTimestamp + "';" +
							"create table export.aentreln as select * from aentreln where aentrelntimestamp > '" + fromTimestamp + "';" +
							"create table export.relationship as select * from relationship where relntimestamp > '" + fromTimestamp + "';" +
							"create table export.relnvalue as select * from relnvalue where relnvaluetimestamp > '" + fromTimestamp + "';" +
							"detach database export;";
				db.exec(query, createCallback());
				
			} finally {
				try {
					if (db != null) db.close();
				} catch (Exception e) {
					FAIMSLog.log(e);
				}
			}
		}
	}

	public static void debugDump(File file) {
		jsqlite.Database db = null;
		try {
			
			db = new jsqlite.Database();
			db.open(file.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE);
			
			db.exec("select * from archentity;", new Callback() {
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
			});
			
		} catch (Exception e) {
			FAIMSLog.log(e);
		} finally {
			try {
				if (db != null) db.close();
			} catch (Exception e) {
				FAIMSLog.log(e);
			}
		}
	}

	public boolean isEmpty(File file) throws jsqlite.Exception {
		synchronized(DatabaseManager.class) {
			Log.d("FAIMS", "checking if database " + file.getAbsolutePath() + " is empty");
			jsqlite.Database db = null;
			try {
				
				db = new jsqlite.Database();
				db.open(file.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE);
				if (!isTableEmpty(db, "archentity")) return false;
				if (!isTableEmpty(db, "aentvalue")) return false;
				if (!isTableEmpty(db, "relationship")) return false;
				if (!isTableEmpty(db, "relnvalue")) return false;
				if (!isTableEmpty(db, "aentreln")) return false;
				
				return true;
			} finally {
				try {
					if (db != null) db.close();
				} catch (Exception e) {
					FAIMSLog.log(e);
				}
			}
		}
	}
	
	private boolean isTableEmpty(jsqlite.Database db, String table) throws jsqlite.Exception {
		Stmt st = null;
		try {
			st = db.prepare("select count(*) from " + table + ";");
			st.step();
			int count = st.column_int(0);
			if (count == 0) {
				return true;
			}
			return false;
		} finally {
			if (st != null) st.close();
		}
		
	}
	
	public void mergeDatabaseFrom(File file) throws jsqlite.Exception {
		synchronized(DatabaseManager.class) {
			Log.d("FAIMS", "merging database");
			jsqlite.Database db = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				String query = 
						"attach database '" + file.getAbsolutePath() + "' as import;" +
						"insert into archentity (uuid, aenttimestamp, userid, doi, aenttypeid, geospatialcolumntype, geospatialcolumn, deleted) select uuid, aenttimestamp, userid, doi, aenttypeid, geospatialcolumntype, geospatialcolumn, deleted from import.archentity where uuid || aenttimestamp not in (select uuid || aenttimestamp from archentity);" +
						"insert into aentvalue (uuid, valuetimestamp, vocabid, attributeid, freetext, measure, certainty) select uuid, valuetimestamp, vocabid, attributeid, freetext, measure, certainty from import.aentvalue where uuid || valuetimestamp || attributeid not in (select uuid || valuetimestamp||attributeid from aentvalue);" +
						"insert into relationship (relationshipid, userid, relntimestamp, geospatialcolumntype, relntypeid, geospatialcolumn, deleted) select relationshipid, userid, relntimestamp, geospatialcolumntype, relntypeid, geospatialcolumn, deleted from import.relationship where relationshipid || relntimestamp not in (select relationshipid || relntimestamp from relationship);" +
						"insert into relnvalue (relationshipid, attributeid, vocabid, relnvaluetimestamp, freetext) select relationshipid, attributeid, vocabid, relnvaluetimestamp, freetext from import.relnvalue where relationshipid || relnvaluetimestamp || attributeid not in (select relationshipid || relnvaluetimestamp || attributeid from relnvalue);" + 
						"insert into aentreln (uuid, relationshipid, participatesverb, aentrelntimestamp, deleted) select uuid, relationshipid, participatesverb, aentrelntimestamp, deleted from import.aentreln where uuid || relationshipid || aentrelntimestamp not in (select uuid || relationshipid || aentrelntimestamp from aentreln);" +
						"detach database import;";
				db.exec(query, createCallback());
			} finally {
				try {
					if (db != null) db.close();
				} catch (Exception e) {
					FAIMSLog.log(e);
				}
			}
		}
	}
	
}
