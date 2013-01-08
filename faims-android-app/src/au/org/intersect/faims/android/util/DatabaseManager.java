package au.org.intersect.faims.android.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import au.org.intersect.faims.android.ui.form.EntityAttribute;
import au.org.intersect.faims.android.ui.form.RelationshipAttribute;

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
				archAttribute.setName(stmt.column_string(0));
				archAttribute.setVocab(Integer.toString(stmt.column_int(1)));
				archAttribute.setMeasure(Integer.toString(stmt.column_int(2)));
				archAttribute.setText(stmt.column_string(3));
				archAttribute.setCertainty(Double.toString(stmt.column_double(4)));
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
				relAttribute.setName(stmt.column_string(0));
				relAttribute.setVocab(Integer.toString(stmt.column_int(1)));
				relAttribute.setText(stmt.column_string(2));
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
