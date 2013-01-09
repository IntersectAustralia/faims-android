package au.org.intersect.faims.android.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jsqlite.Callback;
import jsqlite.Stmt;
import au.org.intersect.faims.android.ui.form.EntityAttribute;
import au.org.intersect.faims.android.ui.form.RelationshipAttribute;

public class DatabaseManager {

	private String dbname;

	public DatabaseManager(String filename) {
		this.dbname = filename;
	}

	public void saveArchEnt(String entity_id, String entity_type,
			String geo_data, List<EntityAttribute> attributes) {
		
		for (EntityAttribute attribute : attributes) {
			FAIMSLog.log(attribute.toString());
		}
		
		try {
			
			jsqlite.Database db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
			
			String uuid;
			
			if (entity_id == null) {
				// check if entity type exists
				
				UUID entity_uuid = UUID.randomUUID();
				
				String query = "INSERT INTO ArchEntity (uuid, MSB_UUID, userid, AEntTypeID, GeoSpatialColumnType, GeoSpatialColumn, AEntTimestamp) " + 
							   "VALUES (?, ?, 0, ?, 'POLYGON', GeomFromText('GEOMETRYCOLLECTION(POLYGON(101.23 171.82, 201.32 101.5, 215.7 201.953, 101.23 171.82))', 4326), CURRENT_TIMESTAMP);";
				Stmt st = db.prepare(query);
				st.bind(1, entity_uuid.getLeastSignificantBits());
				st.bind(2, entity_uuid.getMostSignificantBits());
				st.bind(3, entity_type);
				st.step();
				st.close();
				
				uuid = String.valueOf(entity_uuid.getLeastSignificantBits());
			} else {
				// check if uuid exists
				
				uuid = entity_id;
			}
			
			for (EntityAttribute attribute : attributes) {
				// check if attribute exists
				
				String query = "INSERT INTO AEntValue (uuid, VocabID, AttributeID, Measure, FreeText, Certainty, ValueTimestamp) " +
							   "SELECT ?, ?, attributeID, ?, ?, ?, CURRENT_TIMESTAMP " + 
							   "FROM AttributeKey LEFT OUTER JOIN Vocabulary USING (attributeID) " + 
							   "WHERE attributeName = ? " + 
							   "AND (vocabID = :VocabID OR vocabID is null);";
				Stmt st = db.prepare(query);
				st.bind(1, uuid);
				st.bind(2, attribute.getVocab());
				st.bind(3, attribute.getMeasure());
				st.bind(4, attribute.getText());
				st.bind(5, attribute.getCertainty());
				st.bind(6, attribute.getName());
				st.step();
				st.close();
			}
			
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
			db.exec("select uuid, valuetimestamp, attributeid from aentvalue where uuid || valuetimestamp || attributeid in (select uuid || max(valuetimestamp) || attributeid from aentvalue group by uuid, attributeid);", cb);
			db.exec("select attributeid, valuetimestamp from aentvalue where uuid="+uuid+";", cb);
			db.close();
			
		} catch (Exception e) {
			FAIMSLog.log(e);
		}
	}
	
	public Object fetchArchEnt(String id){
		
		try {
			jsqlite.Database db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
			String query = "SELECT uuid, attributename, vocabid, measure, freetext, certainty FROM (SELECT uuid, attributeid, vocabid, measure, freetext, certainty, valuetimestamp FROM aentvalue WHERE uuid || valuetimestamp || attributeid in (SELECT uuid || max(valuetimestamp) || attributeid FROM aentvalue WHERE uuid = ? GROUP BY uuid, attributeid)) JOIN attributekey USING (attributeid);";
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
			String query = "SELECT relationshipid, attributename, vocabname, freetext FROM (SELECT relationshipid, attributeid, vocabid, freetext FROM relnvalue WHERE relationshipid || relnvaluetimestamp || attributeid in (SELECT relationshipid || max(relnvaluetimestamp) || attributeid FROM relnvalue WHERE relationshipid = ? GROUP BY relationshipid, attributeid)) JOIN attributekey USING (attributeid) JOIN vocabulary USING (vocabid);";
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
}
