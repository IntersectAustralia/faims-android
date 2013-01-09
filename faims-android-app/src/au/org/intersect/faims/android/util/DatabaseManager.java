package au.org.intersect.faims.android.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jsqlite.Callback;
import jsqlite.Database;
import jsqlite.Stmt;
import au.org.intersect.faims.android.ui.form.EntityAttribute;
import au.org.intersect.faims.android.ui.form.RelationshipAttribute;

public class DatabaseManager {

	private String dbname;

	public DatabaseManager(String filename) {
		this.dbname = filename;
	}

	public String saveArchEnt(String entity_id, String entity_type,
			String geo_data, List<EntityAttribute> attributes) {
		
		FAIMSLog.log("entity_id:" + entity_id);
		FAIMSLog.log("entity_type:" + entity_type);
		
		for (EntityAttribute attribute : attributes) {
			FAIMSLog.log(attribute.toString());
		}
		
		jsqlite.Database db = null;
		try {
			
			db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
			
			String uuid;
			Stmt st;
			
			if (entity_id == null) {
				// check if entity type exists
				if (!hasEntityType(db, entity_type)) {
					return null;
				}
				
				// create new entity
				UUID entity_uuid = UUID.randomUUID();
				
				String query = "INSERT INTO ArchEntity (uuid, userid, AEntTypeID, GeoSpatialColumnType, GeoSpatialColumn, AEntTimestamp) " + 
							   "VALUES (?, 0, ?, 'POLYGON', GeomFromText('GEOMETRYCOLLECTION(POLYGON(101.23 171.82, 201.32 101.5, 215.7 201.953, 101.23 171.82))', 4326), CURRENT_TIMESTAMP);";
				st = db.prepare(query);
				st.bind(1, entity_uuid.getLeastSignificantBits());
				st.bind(2, entity_type);
				st.step();
				st.close();
				
				uuid = String.valueOf(entity_uuid.getLeastSignificantBits());
			} else {
				// check if entity exists
				if (!hasEntity(db, entity_id)) {
					return null;
				}
				
				uuid = entity_id;
			}
			
			// save entity attributes
			for (EntityAttribute attribute : attributes) {
				String query = "INSERT INTO AEntValue (uuid, VocabID, AttributeID, Measure, FreeText, Certainty, ValueTimestamp) " +
							   "SELECT ?, ?, attributeID, ?, ?, ?, CURRENT_TIMESTAMP " + 
							   "FROM AttributeKey " + 
							   "WHERE attributeName = ?;";
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
			
			debugSaveArchEnt(db, uuid);
			
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
	
	public String saveRel(String rel_id, String rel_type,
			String geo_data, List<RelationshipAttribute> attributes) {
		
		FAIMSLog.log("rel_id:" + rel_id);
		FAIMSLog.log("rel_type:" + rel_type);
		
		for (RelationshipAttribute attribute : attributes) {
			FAIMSLog.log(attribute.toString());
		}
		
		jsqlite.Database db = null;
		try {
			
			db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
			
			String uuid;
			Stmt st;
			
			if (rel_id == null) {
				// check if relationship type exists
				if (!hasRelationshipType(db, rel_type)) {
					return null;
				}
				
				// create new relationship
				UUID rel_uuid = UUID.randomUUID();
				
				String query = "INSERT INTO Relationship (RelationshipID, userid, RelnTypeID, GeoSpatialColumnType, GeoSpatialColumn, RelnTimestamp) " + 
							   "VALUES (?, 0, ?, 'POLYGON', GeomFromText('GEOMETRYCOLLECTION(POLYGON(101.23 171.82, 201.32 101.5, 215.7 201.953, 101.23 171.82))', 4326), CURRENT_TIMESTAMP);";
				st = db.prepare(query);
				st.bind(1, rel_uuid.getLeastSignificantBits());
				st.bind(2, rel_type);
				st.step();
				st.close();
				
				uuid = String.valueOf(rel_uuid.getLeastSignificantBits());
			} else {
				// check if relationship exists
				if (!hasRelationship(db, rel_id)) {
					return null;
				}
				
				uuid = rel_id;
			}
			
			// save relationship attributes
			for (RelationshipAttribute attribute : attributes) {
				String query = "INSERT INTO RelnValue (RelationshipID, VocabID, AttributeID, FreeText, RelnValueTimestamp) " +
							   "SELECT ?, ?, 0, ?, CURRENT_TIMESTAMP " + 
							   "FROM AttributeKey " + 
							   "WHERE attributeName = ?;";
				st = db.prepare(query);
				st.bind(1, uuid);
				st.bind(2, attribute.getVocab());
				st.bind(3, attribute.getText());
				st.bind(4, attribute.getName());
				st.step();
				st.close();
			}
			
			debugSaveRel(db, uuid);
			
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
	
	public boolean addReln(String entity_id, String rel_id, String verb) {
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

	public Object fetchArchEnt(String id){
		
		try {
			jsqlite.Database db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
			String query = "SELECT uuid, attributename, vocabid, measure, freetext, certainty FROM (SELECT uuid, attributeid, vocabid, measure, freetext, certainty, valuetimestamp FROM aentvalue WHERE uuid = ? GROUP BY uuid, attributeid HAVINg max(valuetimestamp)) AS latestValue JOIN attributekey USING (attributeid)";
			Stmt stmt = db.prepare(query);
			stmt.bind(1, id);
			Collection<EntityAttribute> archAttributes = new ArrayList<EntityAttribute>();
			while(stmt.step()){
				EntityAttribute archAttribute = new EntityAttribute();
				archAttribute.setName(stmt.column_string(1));
				archAttribute.setVocab(Integer.toString(stmt.column_int(2)));
				archAttribute.setMeasure(Integer.toString(stmt.column_int(3)));
				archAttribute.setText(stmt.column_string(4));
				archAttribute.setCertainty(Double.toString(stmt.column_double(5)));
				archAttributes.add(archAttribute);
			}
			db.close();

			return archAttributes;
		} catch (jsqlite.Exception e) {
			FAIMSLog.log(e);
		}
		return null;
	}
	
	public Object fetchRel(String id){
		
		try {
			jsqlite.Database db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
			String query = "SELECT relationshipid, attributename, vocabname, freetext FROM (SELECT relationshipid, attributeid, vocabid, freetext FROM relnvalue WHERE relationshipid = (select relationshipid from relnvalue limit 1) GROUP BY relationshipid, attributeid HAVINg max(relnvaluetimestamp)) AS latestValue JOIN attributekey USING (attributeid) JOIN vocabulary USING (vocabid);";
			Stmt stmt = db.prepare(query);
			stmt.bind(1, id);
			Collection<RelationshipAttribute> relAttributes = new ArrayList<RelationshipAttribute>();
			while(stmt.step()){
				RelationshipAttribute relAttribute = new RelationshipAttribute();
				relAttribute.setName(stmt.column_string(1));
				relAttribute.setVocab(Integer.toString(stmt.column_int(2)));
				relAttribute.setText(stmt.column_string(3));
				relAttributes.add(relAttribute);
			}
			db.close();

			return relAttributes;
		} catch (jsqlite.Exception e) {
			FAIMSLog.log(e);
		}
		return null;
	}

	public Object fetchOne(String query){
		
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

	public Collection<List<String>> fetchAll(String query){
		
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
	
	private void debugSaveArchEnt(jsqlite.Database db, String uuid) throws Exception {
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
		db.exec("select count(uuid) from ArchEntity;", cb);
		db.exec("select count(uuid) from AEntValue;", cb);
		db.exec("select attributename, vocabname, measure, freetext, certainty, valuetimestamp " + 
				"from aentvalue " +
				"left outer join attributekey using (attributeid) " + 
				"left outer join vocabulary using (attributeid) " +
				"where uuid = " + uuid + " group by uuid, attributeid having max(valuetimestamp);", cb);
	}
	
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
		db.exec("select count(RelationshipID) from Relationship;", cb);
		db.exec("select count(RelationshipID) from RelnValue;", cb);
		db.exec("select attributename, vocabname, freetext, RelnValueTimestamp " + 
				"from RelnValue " +
				"left outer join attributekey using (attributeid) " + 
				"left outer join vocabulary using (attributeid) " +
				"where RelationshipID = " + uuid + " group by RelationshipID, attributeid having max(RelnValueTimestamp);", cb);
	}
	

	private void debugAddReln(Database db, String entity_id, String rel_id) throws Exception {
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
		db.exec("select count(UUID) from AEntReln;", cb);
		db.exec("select UUID, RelationshipID, ParticipatesVerb" + 
				"from AEntReln " +
				"where uuid = " + entity_id + " and RelationshipID = " + rel_id + ";", cb);
	}
}
