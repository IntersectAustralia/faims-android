package au.org.intersect.faims.android.util;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jsqlite.Callback;
import jsqlite.Stmt;
import au.org.intersect.faims.android.ui.form.EntityAttribute;

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
			db.exec("select attributename, vocabname, measure, freetext, certainty " + 
					"from aentvalue " +
					"left outer join attributekey using (attributeid) " + 
					"left outer join vocabulary using (attributeid) " +
					"where uuid = " + uuid + " group by " + uuid + ", attributeid having max(valuetimestamp);", cb);
			
			db.close();
			
		} catch (Exception e) {
			FAIMSLog.log(e);
		}
	}
	
}
