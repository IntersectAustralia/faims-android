package au.org.intersect.faims.android.database;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import jsqlite.Callback;
import jsqlite.Stmt;
import au.org.intersect.faims.android.data.User;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.nutiteq.WKTUtil;
import au.org.intersect.faims.android.ui.form.ArchEntity;
import au.org.intersect.faims.android.ui.form.EntityAttribute;
import au.org.intersect.faims.android.ui.form.Relationship;
import au.org.intersect.faims.android.ui.form.RelationshipAttribute;
import au.org.intersect.faims.android.util.DateUtil;

import com.google.inject.Singleton;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.ui.Label;
import com.nutiteq.utils.Utils;
import com.nutiteq.utils.WkbRead;

@Singleton
public class DatabaseManager {

	private String dbname;
	private String userId;
	
	private jsqlite.Database db;

	public void init(String filename) {
		this.dbname = filename;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public String getUserId() {
		return this.userId;
	}
	
	public void interrupt() {
		try {
			if (db != null) {
				db.close();
				db = null;
			}
		} catch (Exception e) {
			FLog.e("error closing database", e);
		}
	}

	public double getDistanceBetween(String prevLong, String prevLat, String curLong, String curLat) throws jsqlite.Exception {
		synchronized(DatabaseManager.class) {
			Stmt st = null;
			double distance = 0;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				String query = "select geodesiclength(linefromtext('LINESTRING("+ prevLong+" " + prevLat + ", " + curLong + " " + curLat + ")', 4326));";
				st = db.prepare(query);
				
				if(st.step()){
					distance = st.column_double(0);
				}
				st.close();
				st = null;
				return distance;
			} finally {
				try {
					if (st != null) st.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	public void saveGPSTrack(List<Geometry> geo_data, String longitude, String latitude, String heading, String accuracy, String type){
		String currentTimestamp = DateUtil.getCurrentTimestampGMT();
		List<EntityAttribute> attributes = new ArrayList<EntityAttribute>();
		EntityAttribute user_attribute = new EntityAttribute();
		user_attribute.setName("gps_user");
		user_attribute.setText(userId);
		EntityAttribute timestamp_attribute = new EntityAttribute();
		timestamp_attribute.setName("gps_timestamp");
		timestamp_attribute.setText(currentTimestamp);
		EntityAttribute long_attribute = new EntityAttribute();
		long_attribute.setName("gps_longitude");
		long_attribute.setText(longitude);
		EntityAttribute lat_attribute = new EntityAttribute();
		lat_attribute.setName("gps_latitude");
		lat_attribute.setText(latitude);
		EntityAttribute heading_attribute = new EntityAttribute();
		heading_attribute.setName("gps_heading");
		heading_attribute.setText(heading);
		EntityAttribute accuracy_attribute = new EntityAttribute();
		accuracy_attribute.setName("gps_accuracy");
		accuracy_attribute.setText(accuracy);
		EntityAttribute type_attribute = new EntityAttribute();
		type_attribute.setName("gps_type");
		if("time".equals(type)){
			type_attribute.setText("track using time");
		}else if("distance".equals(type)){
			type_attribute.setText("track using distance");
		}
		attributes.add(user_attribute);
		attributes.add(timestamp_attribute);
		attributes.add(long_attribute);
		attributes.add(lat_attribute);
		attributes.add(heading_attribute);
		attributes.add(accuracy_attribute);
		attributes.add(type_attribute);
		try {
			saveArchEnt(null, "gps_track", WKTUtil.collectionToWKT(geo_data), attributes);
		} catch (Exception e) {
			FLog.e("error saving gps track data", e);
		}
	}

	public String saveArchEnt(String entity_id, String entity_type,
			String geo_data, List<EntityAttribute> attributes) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("entity_id:" + entity_id);
			FLog.d("entity_type:" + entity_type);
			FLog.d("geo_data:" + geo_data);
			
			for (EntityAttribute attribute : attributes) {
				FLog.d(attribute.toString());
			}
			
			Stmt st = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				if (!validArchEnt(db, entity_id, entity_type, geo_data, attributes)) {
					FLog.d("arch entity not valid");
					return null;
				}
				
				String uuid;
				
				if (entity_id == null) {
					// create new entity
					uuid = generateUUID();
				} else {
					// update entity
					uuid = entity_id;
				}
				
				String currentTimestamp = DateUtil.getCurrentTimestampGMT();
				
				String query = "INSERT INTO ArchEntity (uuid, userid, AEntTypeID, GeoSpatialColumn, AEntTimestamp) " +
									"SELECT cast(? as integer), ?, aenttypeid, GeomFromText(?, 4326), ? " +
									"FROM aenttype " + 
									"WHERE aenttypename = ? COLLATE NOCASE;";
				st = db.prepare(query);
				st.bind(1, uuid);
				st.bind(2, userId);
				st.bind(3, geo_data);
				st.bind(4, currentTimestamp);
				st.bind(5, entity_type);
				st.step();
				st.close();
				st = null;
				
				// save entity attributes
				for (EntityAttribute attribute : attributes) {
					query = "INSERT INTO AEntValue (uuid, userid, VocabID, AttributeID, Measure, FreeText, Certainty, ValueTimestamp, deleted) " +
								   "SELECT cast(? as integer), ?, ?, attributeID, ?, ?, ?, ?, ? " +
								   "FROM AttributeKey " + 
								   "WHERE attributeName = ? COLLATE NOCASE;";
					st = db.prepare(query);
					st.bind(1, uuid);
					st.bind(2, userId);
					st.bind(3, attribute.getVocab());
					st.bind(4, attribute.getMeasure());
					st.bind(5, attribute.getText());
					st.bind(6, attribute.getCertainty());
					st.bind(7, currentTimestamp);
					st.bind(8, attribute.isDeleted() ? "true" : null);
					st.bind(9, attribute.getName());
					st.step();
					st.close();
					st = null;
				}
				
				return uuid;
				
			} finally {
				try {
					if (st != null) st.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	public String saveRel(String rel_id, String rel_type,
			String geo_data, List<RelationshipAttribute> attributes) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("rel_id:" + rel_id);
			FLog.d("rel_type:" + rel_type);
			FLog.d("geo_data:" + geo_data);
			
			for (RelationshipAttribute attribute : attributes) {
				FLog.d(attribute.toString());
			}
			
			Stmt st = null;
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				if (!validRel(db, rel_id, rel_type, geo_data, attributes)) {
					FLog.d("relationship not valid");
					return null;
				}
				
				String uuid;
				
				if (rel_id == null) {
					// create new relationship
					uuid = generateUUID();
					
				} else {
					
					uuid = rel_id;
				}
				
				String currentTimestamp = DateUtil.getCurrentTimestampGMT();
				
				String query = "INSERT INTO Relationship (RelationshipID, userid, RelnTypeID, GeoSpatialColumn, RelnTimestamp) " +
									"SELECT cast(? as integer), ?, relntypeid, GeomFromText(?, 4326), ? " +
									"FROM relntype " +
									"WHERE relntypename = ? COLLATE NOCASE;";
				st = db.prepare(query);
				st.bind(1, uuid);
				st.bind(2, userId);
				st.bind(3, geo_data);
				st.bind(4, currentTimestamp);
				st.bind(5, rel_type);
				st.step();
				st.close();
				st = null;
				
				// save relationship attributes
				for (RelationshipAttribute attribute : attributes) {
					query = "INSERT INTO RelnValue (RelationshipID, UserId, VocabID, AttributeID, FreeText, Certainty, RelnValueTimestamp, deleted) " +
								   "SELECT cast(? as integer), ?, ?, attributeId, ?, ?, ?, ? " +
								   "FROM AttributeKey " + 
								   "WHERE attributeName = ? COLLATE NOCASE;";
					st = db.prepare(query);
					st.bind(1, uuid);
					st.bind(2, userId);
					st.bind(3, attribute.getVocab());
					st.bind(4, attribute.getText());
					st.bind(5, attribute.getCertainty());
					st.bind(6, currentTimestamp);
					st.bind(7, attribute.isDeleted() ? "true" : null);
					st.bind(8, attribute.getName());
					st.step();
					st.close();
					st = null;
				}
				
				return uuid;
				
			} finally {
				try {
					if (st != null) st.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
			
		}
	}
	
	private boolean validArchEnt(jsqlite.Database db, String entity_id, String entity_type, String geo_data, List<EntityAttribute> attributes) throws Exception {
		Stmt st = null;
		try {
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
				
				st = db.prepare(query);
				st.bind(1, entity_type);
				st.bind(2, attribute.getName());
				st.step();
				if (st.column_int(0) == 0) {
					return false;
				}
				st.close();
				st = null;
			}
		} finally {
			if (st != null) st.close();
		}
		return true;
	}
	
	private boolean validRel(jsqlite.Database db, String rel_id, String rel_type, String geo_data, List<RelationshipAttribute> attributes) throws Exception {
		Stmt st = null;
		try {
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
				st = db.prepare(query);
				st.bind(1, rel_type);
				st.bind(2, attribute.getName());
				st.step();
				if (st.column_int(0) == 0) {
					return false;
				}
				st.close();
				st = null;
			}
		} finally {
			if (st != null) st.close();
		}
		
		return true;
	}
	
	public boolean addReln(String entity_id, String rel_id, String verb) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("entity_id:" + entity_id);
			FLog.d("rel_id:" + rel_id);
			FLog.d("verb:" + verb);
			
			Stmt st = null;
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				if (!hasEntity(db, entity_id) || !hasRelationship(db, rel_id)) {
					FLog.d("cannot add entity to relationship");
					return false;
				}
				
				String currentTimestamp = DateUtil.getCurrentTimestampGMT();
				
				// create new entity relationship
				String query = "INSERT INTO AEntReln (UUID, RelationshipID, UserId, ParticipatesVerb, AEntRelnTimestamp) " +
							   "VALUES (?, ?, ?, ?, ?);";
				st = db.prepare(query);
				st.bind(1, entity_id);
				st.bind(2, rel_id);
				st.bind(3, userId);
				st.bind(4, verb);
				st.bind(5, currentTimestamp);
				st.step();
				st.close();
				st = null;
				
				return true;
				
			} finally {
				try {
					if (st != null) st.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public Object fetchArchEnt(String id) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				if (!hasEntity(db, id)) {
					return null;
				}
	
				String query = "SELECT uuid, attributename, vocabid, measure, freetext, certainty, attributetype, aentvaluedeleted, aentdirty, aentdirtyreason FROM " +
					    "(SELECT uuid, attributeid, vocabid, measure, freetext, certainty, valuetimestamp, aentvalue.deleted as aentvaluedeleted, aentvalue.isDirty as aentdirty, aentvalue.isDirtyReason as aentdirtyreason FROM aentvalue WHERE uuid || valuetimestamp || attributeid in " +
					        "(SELECT uuid || max(valuetimestamp) || attributeid FROM aentvalue WHERE uuid = ? GROUP BY uuid, attributeid) ) " +
					"JOIN attributekey USING (attributeid) " +
					"JOIN ArchEntity USING (uuid) " +
					"where uuid || aenttimestamp in ( select uuid || max(aenttimestamp) from archentity group by uuid having deleted is null);";
				stmt = db.prepare(query);
				stmt.bind(1, id);
				Collection<EntityAttribute> attributes = new ArrayList<EntityAttribute>();
				while(stmt.step()){
					EntityAttribute archAttribute = new EntityAttribute();
					archAttribute.setName(stmt.column_string(1));
					archAttribute.setVocab(Integer.toString(stmt.column_int(2)));
					archAttribute.setMeasure(Double.toString(stmt.column_double(3)));
					archAttribute.setText(stmt.column_string(4));
					archAttribute.setCertainty(Double.toString(stmt.column_double(5)));
					archAttribute.setType(stmt.column_string(6));
					archAttribute.setDeleted(stmt.column_string(7) != null ? true : false);
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
				stmt = db.prepare("SELECT uuid, HEX(AsBinary(GeoSpatialColumn)) from ArchEntity where uuid || aenttimestamp IN ( SELECT uuid || max(aenttimestamp) FROM archentity WHERE uuid = ?);");
				stmt.bind(1, id);
				List<Geometry> geomList = new ArrayList<Geometry>();
				if(stmt.step()){
					Geometry[] gs = WkbRead.readWkb(
		                    new ByteArrayInputStream(Utils
		                            .hexStringToByteArray(stmt.column_string(1))), null);
					if (gs != null) {
			            for (int i = 0; i < gs.length; i++) {
			            	Geometry g = gs[i];
			                geomList.add(GeometryUtil.fromGeometry(g));
			            }
					}
				}
				stmt.close();
				stmt = null;
	
				ArchEntity archEntity = new ArchEntity(id, null, attributes, geomList);
				
				return archEntity;
			} finally {
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	public Object fetchRel(String id) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			try {
				 db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
				if (!hasRelationship(db, id)) {
					return null;
				}
				
				String query = "SELECT relationshipid, attributename, vocabid, freetext, certainty, attributetype, relnvaluedeleted, relndirty, relndirtyreason FROM " +
					    "(SELECT relationshipid, attributeid, vocabid, freetext, certainty, relnvalue.deleted as relnvaluedeleted, relnvalue.isDirty as relndirty, relnvalue.isDirtyReason as relndirtyreason FROM relnvalue WHERE relationshipid || relnvaluetimestamp || attributeid in " +
					        "(SELECT relationshipid || max(relnvaluetimestamp) || attributeid FROM relnvalue WHERE relationshipid = ? GROUP BY relationshipid, attributeid having deleted is null)) " +
					"JOIN attributekey USING (attributeid) " +
					"JOIN Relationship USING (relationshipid) " +
					"where relationshipid || relntimestamp in (select relationshipid || max (relntimestamp) from relationship group by relationshipid having deleted is null )";
				stmt = db.prepare(query);
				stmt.bind(1, id);
				Collection<RelationshipAttribute> attributes = new ArrayList<RelationshipAttribute>();
				while(stmt.step()){
					RelationshipAttribute relAttribute = new RelationshipAttribute();
					relAttribute.setName(stmt.column_string(1));
					relAttribute.setVocab(Integer.toString(stmt.column_int(2)));
					relAttribute.setText(stmt.column_string(3));
					relAttribute.setCertainty(stmt.column_string(4));
					relAttribute.setType(stmt.column_string(5));
					relAttribute.setDeleted(stmt.column_string(6) != null ? true : false);
					relAttribute.setDirty(stmt.column_string(7) != null ? true : false);
					relAttribute.setDirtyReason(stmt.column_string(8));
					attributes.add(relAttribute);
				}
				stmt.close();
				stmt = null;
				
				// get vector geometry
				stmt = db.prepare("SELECT relationshipid, HEX(AsBinary(GeoSpatialColumn)) from relationship where relationshipid || relntimestamp IN ( SELECT relationshipid || max(relntimestamp) FROM relationship WHERE relationshipid = ?);");
				stmt.bind(1, id);
				List<Geometry> geomList = new ArrayList<Geometry>();
				if(stmt.step()){
					Geometry[] gs = WkbRead.readWkb(
		                    new ByteArrayInputStream(Utils
		                            .hexStringToByteArray(stmt.column_string(1))), null);
					if (gs != null) {
			            for (int i = 0; i < gs.length; i++) {
			            	Geometry g = gs[i];
			                geomList.add(GeometryUtil.fromGeometry(g));
			            }
					}
				}
				stmt.close();
				stmt = null;
				
				Relationship relationship = new Relationship(id, null, attributes, geomList);
	
				return relationship;
			} finally {
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public Object fetchOne(String query) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				stmt = db.prepare(query);
				Collection<String> results = new ArrayList<String>();
				if(stmt.step()){
					for(int i = 0; i < stmt.column_count(); i++){
						results.add(stmt.column_string(i));
					}
				}
				stmt.close();
				stmt = null;
				
				return results;
			} finally {
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public Collection<List<String>> fetchAll(String query) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
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
	
				return results;
			} finally {
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public Vector<Geometry> fetchVisibleGPSTrackingForUser(MapPos min, MapPos max, int maxObjects, String querySql, String userid) throws Exception{
		if(querySql == null){
			return null;
		}

		String s = userid;
		while (s.length() < 5) {
			s = "0" + s;
		}
		String uuidForUser = "1" + s;
		Vector<Geometry> geometries = fetchAllVisibleEntityGeometry(min, max, querySql, maxObjects);
		Vector<Geometry> userGeometries = new Vector<Geometry>();
		for (Geometry geometry : geometries) {
			String[] userData = (String[]) geometry.userData;
			if(userData[0].startsWith(uuidForUser)){
				userGeometries.add(geometry);
			}
		}
		return userGeometries;
	}

	public List<User> fetchAllUser() throws Exception{
		Collection<List<String>> users = fetchAll("select userid, fname, lname from user");
		List<User> userList = new ArrayList<User>();
		for(List<String> userData : users){
			User user = new User(userData.get(0), userData.get(1), userData.get(2));
			userList.add(user);
		}
		return userList;
	}

	public Collection<List<String>> fetchEntityList(String type) throws Exception {
		String query = 
			"select uuid, group_concat(coalesce(measure    || ' '  || vocabname  || '(' ||freetext||'; '|| (certainty * 100.0) || '% certain)',\n" + 
			"                                                                                              measure    || ' (' || freetext   ||'; '|| (certainty * 100.0)  || '% certain)',\n" + 
			"                                                                                              vocabname  || ' (' || freetext   ||'; '|| (certainty * 100.0)  || '% certain)',\n" + 
			"                                                                                              measure    || ' ' || vocabname   ||' ('|| (certainty * 100.0)  || '% certain)',\n" + 
			"                                                                                              vocabname  || ' (' || freetext || ')',\n" + 
			"                                                                                              measure    || ' (' || freetext || ')',\n" + 
			"                                                                                              measure    || ' (' || (certainty * 100.0) || '% certain)',\n" + 
			"                                                                                              vocabname  || ' (' || (certainty * 100.0) || '% certain)',\n" + 
			"                                                                                              freetext   || ' (' || (certainty * 100.0) || '% certain)',\n" + 
			"                                                                                              measure,\n" + 
			"                                                                                              vocabname,\n" + 
			"                                                                                              freetext), ' | ') as response\n" + 
			"FROM (  SELECT uuid, attributeid, vocabid, attributename, vocabname, measure, freetext, certainty, attributetype, valuetimestamp\n" + 
			"          FROM aentvalue\n" + 
			"          JOIN attributekey USING (attributeid)\n" + 
			"          join archentity USING (uuid)\n" + 
			"          join (select attributeid, aenttypeid from idealaent join aenttype using (aenttypeid) where isIdentifier is 'true' and lower(aenttypename) = lower('" + type + "')) USING (attributeid, aenttypeid)\n" + 
			"          LEFT OUTER JOIN vocabulary USING (vocabid, attributeid)\n" + 
			"          JOIN (SELECT uuid, attributeid, valuetimestamp\n" + 
			"                  FROM aentvalue\n" + 
			"                  JOIN archentity USING (uuid)\n" + 
			"                 WHERE archentity.deleted is NULL\n" + 
			"              GROUP BY uuid, attributeid\n" + 
			"                HAVING MAX(ValueTimestamp)\n" + 
			"                   AND MAX(AEntTimestamp)) USING (uuid, attributeid, valuetimestamp)\n" + 
			"          WHERE aentvalue.deleted is NULl\n" + 
			"       ORDER BY uuid, attributename ASC)\n" + 
			"group by uuid;";
		return fetchAll(query);
	}
	
	public Collection<List<String>> fetchRelationshipList(String type) throws Exception {
		String query = 
			"select relationshipid, group_concat(coalesce(vocabname  || ' (' || freetext   ||'; '|| (certainty * 100.0)  || '% certain)',\n" + 
			"                                                                                         vocabname  || ' (' || freetext || ')',\n" + 
			"                                                                                         vocabname  || ' (' || (certainty * 100.0) || '% certain)',\n" + 
			"                                                                                         freetext   || ' (' || (certainty * 100.0) || '% certain)',\n" + 
			"                                                                                         vocabname,\n" + 
			"                                                                                         freetext), ' | ') as response\n" + 
			"from (\n" + 
			"SELECT relationshipid, vocabid, attributeid, attributename, freetext, certainty, vocabname, relntypeid, attributetype, relnvaluetimestamp\n" + 
			"    FROM relnvalue\n" + 
			"    JOIN attributekey USING (attributeid)\n" + 
			"    JOIN relationship USING (relationshipid)\n" + 
			"    join  (select attributeid, relntypeid from idealreln join relntype using (relntypeid) where isIdentifier is 'true' and lower(relntypename) = lower('" + type + "')) USING (attributeid, relntypeid)\n" + 
			"    LEFT OUTER JOIN vocabulary USING (vocabid, attributeid)\n" + 
			"    JOIN ( SELECT relationshipid, attributeid, relnvaluetimestamp, relntypeid\n" + 
			"             FROM relnvalue\n" + 
			"             JOIN relationship USING (relationshipid)\n" + 
			"            WHERE relationship.deleted is NULL\n" + 
			"         GROUP BY relationshipid, attributeid\n" + 
			"           HAVING MAX(relnvaluetimestamp)\n" + 
			"              AND MAX(relntimestamp)\n" + 
			"      ) USING (relationshipid, attributeid, relnvaluetimestamp, relntypeid)\n" + 
			"   WHERE relnvalue.deleted is NULL\n" + 
			"ORDER BY relationshipid, attributename asc)\n" + 
			"group by relationshipid;";
		return fetchAll(query);
	}
	
	public Vector<Geometry> fetchAllVisibleEntityGeometry(MapPos min, MapPos max, String userQuery, int maxObjects) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			try {
				if (userQuery == null) {
					userQuery = "";
				} else {
					userQuery =	"JOIN (" + userQuery + ") USING (uuid, aenttimestamp)\n";
				}
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				String query = "SELECT uuid, coalesce(group_concat(measure || vocabname), group_concat(vocabname, ', '), group_concat(measure, ', '), group_concat(freetext, ', ')) AS response, Hex(AsBinary(geospatialcolumn))\n" + 
						"    FROM (SELECT uuid, attributeid, valuetimestamp, aenttimestamp\n" + 
						"            FROM archentity\n" + 
						"            JOIN aentvalue USING (uuid)\n" + 
						"            JOIN idealaent using (aenttypeid, attributeid)\n" + 
						"           WHERE isIdentifier = 'true'\n" + 
						"             AND uuid IN (SELECT uuid\n" + 
						"                            FROM (SELECT uuid, max(aenttimestamp) as aenttimestamp, deleted as entDel\n" + 
						"                                    FROM archentity\n" + 
						"                                   where st_intersects(geospatialcolumn, PolyFromText(?, 4326))\n" + 
						"                                GROUP BY uuid, aenttypeid\n" + 
						"                                  HAVING max(aenttimestamp)\n" + 
						"                                     )\n" + 
						"                            JOIN (SELECT uuid, max(valuetimestamp) as valuetimestamp\n" + 
						"                                    FROM aentvalue --this gives us a temporal ordering...\n" + 
						"                                  WHERE deleted is null\n" + 
						"                                GROUP BY uuid\n" + 
						"                                  HAVING max(valuetimestamp)\n" + 
						"                                    )\n" + 
						"                            USING (uuid)\n" +
															userQuery +
						"                           WHERE entDel is null\n" + 
						"                           GROUP BY uuid\n" + 
						"                        ORDER BY max(valuetimestamp, aenttimestamp) desc, uuid\n" + 
						"                        LIMIT ?\n" + 
						"                        -- OFFSET ?\n" + 
						"                      )\n" + 
						"        GROUP BY uuid, attributeid\n" + 
						"          HAVING MAX(ValueTimestamp)\n" + 
						"             AND MAX(AEntTimestamp)\n" + 
						"             )\n" + 
						"    JOIN attributekey using (attributeid)\n" + 
						"    JOIN aentvalue using (uuid, attributeid, valuetimestamp)\n" + 
						"    JOIN (SELECT uuid, max(valuetimestamp) AS tstamp FROM aentvalue GROUP BY uuid) USING (uuid)\n" + 
						"    JOIN (SELECT uuid, max(aenttimestamp) AS astamp FROM archentity GROUP BY uuid) USING (uuid)\n" + 
						"    JOIN archentity using (uuid, aenttimestamp)\n" + 
						"    JOIN aenttype using (aenttypeid)\n" + 
						"    LEFT OUTER JOIN vocabulary USING (vocabid, attributeid)\n" + 
						"WHERE aentvalue.deleted is null\n" + 
						"group by uuid\n" + 
						"ORDER BY max(tstamp,astamp) desc, uuid, attributename;";
				stmt = db.prepare(query);
				ArrayList<MapPos> list = new ArrayList<MapPos>();
				list.add(new MapPos(min.x, min.y));
				list.add(new MapPos(max.x, min.y));
				list.add(new MapPos(max.x, max.y));
				list.add(new MapPos(min.x, max.y));
				list.add(new MapPos(min.x, min.y));
				stmt.bind(1, WKTUtil.geometryToWKT(new Polygon(list, (Label) null, (PolygonStyle) null, (Object) null)));
				stmt.bind(2, maxObjects);
				Vector<Geometry> results = new Vector<Geometry>();
				while(stmt.step()){
					String uuid = stmt.column_string(0);
					String response = stmt.column_string(1);
					Geometry[] gs = WkbRead.readWkb(
		                    new ByteArrayInputStream(Utils
		                            .hexStringToByteArray(stmt.column_string(2))), null);
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
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	public Vector<Geometry> fetchAllVisibleRelationshipGeometry(MapPos min, MapPos max, String userQuery, int maxObjects) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			try {
				if (userQuery == null) {
					userQuery = "";
				} else {
					userQuery =	"JOIN (" + userQuery + ") USING (relationshipid, relntimestamp)\n";
				}
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				String query = "SELECT relationshipid, coalesce(group_concat(vocabname, ', '), group_concat(freetext, ', ')) as response, Hex(AsBinary(geospatialcolumn))\n" + 
						"   FROM ( SELECT relationshipid, attributeid, relntimestamp, relnvaluetimestamp\n" + 
						"            FROM relationship\n" + 
						"            JOIN relnvalue USING (relationshipid)\n" + 
						"            JOIN idealreln using (relntypeid, attributeid)\n" + 
						"           WHERE isIdentifier = 'true'\n" + 
						"             AND relationshipid in (SELECT distinct relationshipid\n" + 
						"                                      FROM (SELECT relationshipid, max(relntimestamp) as relntimestamp, deleted as relnDeleted\n" + 
						"                                              FROM relationship\n" + 
						"                                              where st_intersects(geospatialcolumn, PolyFromText(?, 4326))\n" + 
						"                                          GROUP BY relationshipid\n" + 
						"                                            HAVING max(relntimestamp))\n" + 
						"                                      JOIN (SELECT relationshipid, attributeid, max(relnvaluetimestamp) as relnvaluetimestamp\n" + 
						"                                              FROM relnvalue\n" + 
						"                                             WHERE deleted is null\n" + 
						"                                          GROUP BY relationshipid, attributeid, vocabid\n" + 
						"                                            HAVING max(relnvaluetimestamp)\n" + 
						"                                        ) USING (relationshipid)\n" + 
																	userQuery +
						"                                     WHERE relnDeleted is null\n" + 
						"                                  GROUP BY relationshipid\n" + 
						"                                  ORDER BY max(relnvaluetimestamp, relntimestamp) desc, relationshipid\n" + 
						"                                  LIMIT ?\n" + 
						"                                  --OFFSET ?\n" + 
						"                                    )\n" + 
						"        GROUP BY relationshipid, attributeid\n" + 
						"          HAVING MAX(relntimestamp)\n" + 
						"             AND MAX(relnvaluetimestamp))\n" + 
						"   JOIN relationship using (relationshipid, relntimestamp)\n" + 
						"   JOIN relntype using (relntypeid)\n" + 
						"   JOIN attributekey using (attributeid)\n" + 
						"   JOIN relnvalue using (relationshipid, relnvaluetimestamp, attributeid)\n" + 
						"   LEFT OUTER JOIN vocabulary using (vocabid, attributeid)\n" + 
						"   JOIN (SELECT relationshipid, max(relnvaluetimestamp) AS tstamp FROM relnvalue GROUP BY relationshipid) USING (relationshipid)\n" + 
						"   JOIN (SELECT relationshipid, max(relntimestamp) AS astamp FROM relationship GROUP BY relationshipid) USING (relationshipid)\n" + 
						"  WHERE relnvalue.deleted is NULL\n" + 
						"GROUP BY relationshipid, attributeid, relnvaluetimestamp\n" + 
						"ORDER BY max(tstamp,astamp) desc, relationshipid, attributename;";
				stmt = db.prepare(query);
				ArrayList<MapPos> list = new ArrayList<MapPos>();
				list.add(new MapPos(min.x, min.y));
				list.add(new MapPos(max.x, min.y));
				list.add(new MapPos(max.x, max.y));
				list.add(new MapPos(min.x, max.y));
				list.add(new MapPos(min.x, min.y));
				stmt.bind(1, WKTUtil.geometryToWKT(new Polygon(list, (Label) null, (PolygonStyle) null, (Object) null)));
				stmt.bind(2, maxObjects);
				Vector<Geometry> results = new Vector<Geometry>();
				while(stmt.step()){
					String uuid = stmt.column_string(0);
					String response = stmt.column_string(1);
					Geometry[] gs = WkbRead.readWkb(
		                    new ByteArrayInputStream(Utils
		                            .hexStringToByteArray(stmt.column_string(2))), null);
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
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	private boolean hasEntityType(jsqlite.Database db, String entity_type) throws Exception {
		Stmt st = null;
		try {
			st = db.prepare("select count(AEntTypeID) from AEntType where AEntTypeName = ? COLLATE NOCASE;");
			st.bind(1, entity_type);
			st.step();
			if (st.column_int(0) == 0) {
				FLog.d("entity type does not exist");
				return false;
			}
		} finally {
			if (st != null) st.close();
		}
		return true;
	}
	
	private boolean hasEntity(jsqlite.Database db, String entity_id) throws Exception {
		Stmt st = null;
		try {
			st = db.prepare("select count(UUID) from ArchEntity where UUID = ?;");
			st.bind(1, entity_id);
			st.step();
			if (st.column_int(0) == 0) {
				FLog.d("entity id " + entity_id + " does not exist");
				return false;
			}
		} finally {
			if (st != null) st.close();
		}
		return true;
	}
	
	private boolean hasRelationshipType(jsqlite.Database db, String rel_type) throws Exception {
		Stmt st = null;
		try {
			st = db.prepare("select count(RelnTypeID) from RelnType where RelnTypeName = ? COLLATE NOCASE;");
			st.bind(1, rel_type);
			st.step();
			if (st.column_int(0) == 0) {
				FLog.d("rel type does not exist");
				return false;
			}
		} finally {
			if (st != null) st.close();
		}
		return true;
	}
	
	private boolean hasRelationship(jsqlite.Database db, String rel_id) throws Exception {
		Stmt st = null;
		try {
			st = db.prepare("select count(RelationshipID) from Relationship where RelationshipID = ?;");
			st.bind(1, rel_id);
			st.step();
			if (st.column_int(0) == 0) {
				FLog.d("rel id " + rel_id + " does not exist");
				return false;
			}
		} finally {
			if (st != null) st.close();
		}
		return true;
	}
	
	private Callback createCallback() {
		return new Callback() {
			@Override
			public void columns(String[] coldata) {
				FLog.d("Columns: " + Arrays.toString(coldata));
			}

			@Override
			public void types(String[] types) {
				FLog.d("Types: " + Arrays.toString(types));
			}

			@Override
			public boolean newrow(String[] rowdata) {
				FLog.d("Row: " + Arrays.toString(rowdata));

				return false;
			}
		};
	}
	
	private String generateUUID() {
		String s = userId;
		while (s.length() < 5) {
			s = "0" + s;
		}
		return "1"+ s + String.valueOf(System.currentTimeMillis());
	}

	public void dumpDatabaseTo(File file) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("dumping database to " + file.getAbsolutePath());
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
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	public void dumpDatabaseTo(File file, String fromTimestamp) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("dumping database to " + file.getAbsolutePath());
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
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
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
					FLog.d("Columns: " + Arrays.toString(coldata));
				}

				@Override
				public void types(String[] types) {
					FLog.d("Types: " + Arrays.toString(types));
				}

				@Override
				public boolean newrow(String[] rowdata) {
					FLog.d("Row: " + Arrays.toString(rowdata));

					return false;
				}
			});
			
		} catch (Exception e) {
			FLog.e("error dumping database", e);
		} finally {
			try {
				if (db != null) {
					db.close();
					db = null;
				}
			} catch (Exception e) {
				FLog.e("error closing database", e);
			}
		}
	}

	public boolean isEmpty(File file) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("checking if database " + file.getAbsolutePath() + " is empty");
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
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	private boolean isTableEmpty(jsqlite.Database db, String table) throws Exception {
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
	
	public void mergeDatabaseFrom(File file) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("merging database");
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				String query = 
						"attach database '" + file.getAbsolutePath() + "' as import;" +
						"insert or replace into archentity (\n" + 
						"         uuid, aenttimestamp, userid, doi, aenttypeid, deleted, isdirty, isdirtyreason, isforked, parenttimestamp, geospatialcolumntype, geospatialcolumn) \n" + 
						"  select uuid, aenttimestamp, userid, doi, aenttypeid, deleted, isdirty, isdirtyreason, isforked, parenttimestamp, geospatialcolumntype, geospatialcolumn \n" + 
						"  from import.archentity;\n" +
						"delete from aentvalue\n" + 
						"    where uuid || valuetimestamp || attributeid || coalesce(vocabid, '')|| coalesce(freetext, '')|| coalesce(measure, '')|| coalesce(certainty, '')|| userid IN\n" + 
						"    (select uuid || valuetimestamp || attributeid || coalesce(vocabid, '')|| coalesce(freetext, '')|| coalesce(measure, '')|| coalesce(certainty, '')|| userid from import.aentvalue);" +
						"insert into aentvalue (\n" + 
						"         uuid, valuetimestamp, userid, attributeid, vocabid, freetext, measure, certainty, deleted, isdirty, isdirtyreason, isforked, parenttimestamp) \n" + 
						"  select uuid, valuetimestamp, userid, attributeid, vocabid, freetext, measure, certainty, deleted, isdirty, isdirtyreason, isforked, parenttimestamp \n" + 
						"  from import.aentvalue where uuid || valuetimestamp || attributeid not in (select uuid || valuetimestamp||attributeid from aentvalue);\n" + 
						"insert or replace into relationship (\n" + 
						"         relationshipid, userid, relntimestamp, relntypeid, deleted, isdirty, isdirtyreason, isforked, parenttimestamp, geospatialcolumntype, geospatialcolumn) \n" + 
						"  select relationshipid, userid, relntimestamp, relntypeid, deleted, isdirty, isdirtyreason, isforked, parenttimestamp, geospatialcolumntype, geospatialcolumn\n" + 
						"  from import.relationship;\n" + 
						"delete from relnvalue\n" + 
						"    where relationshipid || relnvaluetimestamp || attributeid || coalesce(vocabid, '')|| coalesce(freetext, '')||  coalesce(certainty, '')|| userid IN\n" + 
						"    (select relationshipid || relnvaluetimestamp || attributeid || coalesce(vocabid, '')|| coalesce(freetext, '')|| coalesce(certainty, '')|| userid from import.relnvalue);" +
						"insert into relnvalue (\n" + 
						"         relationshipid, relnvaluetimestamp, userid, attributeid, vocabid, freetext, certainty, deleted, isdirty, isdirtyreason, isforked, parenttimestamp) \n" + 
						"  select relationshipid, relnvaluetimestamp, userid, attributeid, vocabid, freetext, certainty, deleted, isdirty, isdirtyreason, isforked, parenttimestamp \n" + 
						"  from import.relnvalue where relationshipid || relnvaluetimestamp || attributeid not in (select relationshipid || relnvaluetimestamp || attributeid from relnvalue);\n" + 
						"insert into aentreln (\n" + 
						"         uuid, relationshipid, userid, aentrelntimestamp, participatesverb, deleted, isdirty, isdirtyreason, isforked, parenttimestamp) \n" + 
						"  select uuid, relationshipid, userid, aentrelntimestamp, participatesverb, deleted, isdirty, isdirtyreason, isforked, parenttimestamp\n" + 
						"  from import.aentreln where uuid || relationshipid || aentrelntimestamp not in (select uuid || relationshipid || aentrelntimestamp from aentreln);\n" + 
						"insert or replace into vocabulary (\n" + 
						"         vocabid, attributeid, vocabname, SemanticMapURL,PictureURL) \n" + 
						"  select vocabid, attributeid, vocabname, SemanticMapURL,PictureURL\n" + 
						"  from import.vocabulary;\n" + 
						"detach database import;";
				db.exec(query, createCallback());
			} finally {
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public List<String> runSelectionQuery(String sql, ArrayList<String> values) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("run selection query");
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
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
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public List<String> runLegacySelectionQuery(String dbPath,
			String tableName, String sql, ArrayList<String> values) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("run legacy selection query");
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbPath, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
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
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public List<String> runPointDistanceEntityQuery(Point point, float distance) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("run point distance query");
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
				stmt = db.prepare(
"select uuid, aenttimestamp\n" + 
" from (select uuid, max(aenttimestamp) as aenttimestamp, deleted, geospatialcolumn\n" + 
"          from archentity \n" + 
"      group by uuid \n" + 
"        having max(aenttimestamp))\n" + 
" where deleted is null\n" +
" and geospatialcolumn is not null\n" +
" and st_intersects(buffer(transform(PointFromText(?, 4326), 3785), ?), transform(geospatialcolumn,3785))");
				FLog.d(WKTUtil.geometryToWKT(point));
				FLog.d(""+distance);
				stmt.bind(1, WKTUtil.geometryToWKT(point));
				stmt.bind(2, distance);
				ArrayList<String> result = new ArrayList<String>();
				while(stmt.step()) {
					result.add(stmt.column_string(0));
				}
				return result;
			} finally {
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
	
	public List<String> runPointDistanceRelationshipQuery(Point point, float distance) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("run point distance query");
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
				stmt = db.prepare(
"select relationshipid, relntimestamp\n" + 
" from (select relationshipid, max(relntimestamp) as relntimestamp, deleted, geospatialcolumn\n" + 
"          from relationship \n" + 
"      group by relationshipid \n" + 
"        having max(relntimestamp))\n" + 
" where deleted is null\n" +
" and geospatialcolumn is not null\n" +
" and st_intersects(buffer(transform(PointFromText(?, 4326), 3785), ?), transform(geospatialcolumn,3785))");
				FLog.d(WKTUtil.geometryToWKT(point));
				FLog.d(""+distance);
				stmt.bind(1, WKTUtil.geometryToWKT(point));
				stmt.bind(2, distance);
				ArrayList<String> result = new ArrayList<String>();
				while(stmt.step()) {
					result.add(stmt.column_string(0));
				}
				return result;
			} finally {
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}

	public Collection<? extends String> runPointDistanceLegacyQuery(
			String dbPath, String tableName, String idColumn, String geometryColumn, Point point, float distance) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("run point distance query");
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbPath, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
				stmt = db.prepare("select " + idColumn + " from " + tableName + " where "+ geometryColumn + " is not null and st_intersects(buffer(transform(PointFromText(?, 4326), 3785), ?), transform("+ geometryColumn + ",3785))");
				FLog.d(WKTUtil.geometryToWKT(point));
				FLog.d(""+distance);
				stmt.bind(1, WKTUtil.geometryToWKT(point));
				stmt.bind(2, distance);
				ArrayList<String> result = new ArrayList<String>();
				while(stmt.step()) {
					result.add(dbPath + ":" + tableName + ":" + stmt.column_string(0));
				}
				FLog.d("Result: " + result.toString());
				return result;
			} finally {
				try {
					if (stmt != null) stmt.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
		}
	}
}
