package au.org.intersect.faims.android.database;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import jsqlite.Callback;
import jsqlite.Stmt;
import au.org.intersect.faims.android.data.User;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.nutiteq.WKBUtil;
import au.org.intersect.faims.android.nutiteq.WKTUtil;
import au.org.intersect.faims.android.ui.form.ArchEntity;
import au.org.intersect.faims.android.ui.form.EntityAttribute;
import au.org.intersect.faims.android.ui.form.Relationship;
import au.org.intersect.faims.android.ui.form.RelationshipAttribute;
import au.org.intersect.faims.android.util.DateUtil;

import com.google.inject.Singleton;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
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
	
	private String clean(String s) {
		if (s != null && "".equals(s.trim())) return null;
		return s;
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

	public void deleteArchEnt(String entity_id) throws jsqlite.Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("entity_id:" + entity_id);
			
			Stmt st = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				if (entity_id != null) {
					String parenttimestamp = null;
					
					String query = DatabaseQueries.GET_ARCH_ENT_PARENT_TIMESTAMP;
					st = db.prepare(query);
					st.bind(1, entity_id);
					if (st.step()) {
						parenttimestamp = st.column_string(0);
					}
					st.close();
					st = null;
					
					query = DatabaseQueries.DELETE_ARCH_ENT;
					st = db.prepare(query);
					st.bind(1, userId);
					st.bind(2, parenttimestamp);
					st.bind(3, entity_id);
					st.step();
					st.close();
					st = null;
				}
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

	public String saveArchEnt(String entity_id, String entity_type,
			String geo_data, List<EntityAttribute> attributes) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("entity_id:" + entity_id);
			FLog.d("entity_type:" + entity_type);
			FLog.d("geo_data:" + geo_data);
			
			if (attributes != null) {
				for (EntityAttribute attribute : attributes) {
					FLog.d(attribute.toString());
				}
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
				st.bind(3, clean(geo_data));
				st.bind(4, currentTimestamp);
				st.bind(5, parenttimestamp);
				st.bind(6, entity_type);
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
	
	public String updateArchEnt(String uuid, String geo_data) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("uuid:" + uuid);
			FLog.d("geo_data:" + geo_data);
			
			Stmt st = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
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
				st.bind(2, clean(geo_data));
				st.bind(3, parenttimestamp);
				st.bind(4, uuid);
				st.step();
				st.close();
				st = null;
				
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
	
	public String updateRel(String uuid, String geo_data) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("uuid:" + uuid);
			FLog.d("geo_data:" + geo_data);
			
			Stmt st = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
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
				st.bind(2, clean(geo_data));
				st.bind(3, parenttimestamp);
				st.bind(4, uuid);
				st.step();
				st.close();
				st = null;
				
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
	
	public void deleteRel(String rel_id) throws jsqlite.Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("rel_id:" + rel_id);
			
			Stmt st = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				if (rel_id != null) {
					String parenttimestamp = null;
					
					String query = DatabaseQueries.GET_RELATIONSHIP_PARENT_TIMESTAMP;
					st = db.prepare(query);
					st.bind(1, rel_id);
					if (st.step()) {
						parenttimestamp = st.column_string(0);
					}
					st.close();
					st = null;
					
					query = DatabaseQueries.DELETE_RELN;
					st = db.prepare(query);
					st.bind(1, userId);
					st.bind(2, parenttimestamp);
					st.bind(3, rel_id);
					st.step();
					st.close();
					st = null;
				}
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
			
			if (attributes != null) {
				for (RelationshipAttribute attribute : attributes) {
					FLog.d(attribute.toString());
				}
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
				st.bind(3, clean(geo_data));
				st.bind(4, currentTimestamp);
				st.bind(5, parenttimestamp);
				st.bind(6, rel_type);
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
			if (attributes != null) {
				for (EntityAttribute attribute : attributes) {
					String query = DatabaseQueries.CHECK_VALID_AENT;
					
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
			if (attributes != null) {
				for (RelationshipAttribute attribute : attributes) {
					String query = DatabaseQueries.CHECK_VALID_RELN;
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
				
				String parenttimestamp = null;
				
				String query = DatabaseQueries.GET_AENT_RELN_PARENT_TIMESTAMP;
				st = db.prepare(query);
				st.bind(1, entity_id);
				st.bind(2, rel_id);
				if (st.step()) {
					parenttimestamp = st.column_string(0);
				}
				st.close();
				st = null;
				
				String currentTimestamp = DateUtil.getCurrentTimestampGMT();
				
				// create new entity relationship
				query = DatabaseQueries.INSERT_AENT_RELN;
				st = db.prepare(query);
				st.bind(1, entity_id);
				st.bind(2, rel_id);
				st.bind(3, userId);
				st.bind(4, clean(verb));
				st.bind(5, currentTimestamp);
				st.bind(6, parenttimestamp); 
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

	public ArchEntity fetchArchEnt(String id) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			try {
				
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
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
	
	public Relationship fetchRel(String id) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			try {
				 db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
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
		String query = DatabaseQueries.FETCH_ENTITY_LIST(type);
		return fetchAll(query);
	}
	
	public Collection<List<String>> fetchRelationshipList(String type) throws Exception {
		String query = DatabaseQueries.FETCH_RELN_LIST(type);
		return fetchAll(query);
	}
	
	public Vector<Geometry> fetchAllVisibleEntityGeometry(List<MapPos> list, String userQuery, int maxObjects) throws Exception {
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
	
	public Geometry getBoundaryForVisibleEntityGeometry(String userQuery) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			Geometry result = null;
			try {
				if (userQuery == null) {
					userQuery = "";
				} else {
					userQuery =	"JOIN (" + userQuery + ") USING (uuid, aenttimestamp)\n";
				}
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
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

	public Vector<Geometry> fetchAllVisibleRelationshipGeometry(List<MapPos> list, String userQuery, int maxObjects) throws Exception {
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
	
	public Geometry getBoundaryForVisibleRelnGeometry(String userQuery) throws Exception {
		synchronized(DatabaseManager.class) {
			Stmt stmt = null;
			Geometry result = null;
			try {
				if (userQuery == null) {
					userQuery = "";
				} else {
					userQuery =	"JOIN (" + userQuery + ") USING (relationshipid, relntimestamp)\n";
				}
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
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
			st = db.prepare(DatabaseQueries.COUNT_ENTITY_TYPE);
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
			st = db.prepare(DatabaseQueries.COUNT_ENTITY);
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
			st = db.prepare(DatabaseQueries.COUNT_RELN_TYPE);
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
			st = db.prepare(DatabaseQueries.COUNT_RELN);
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
	
				String query = DatabaseQueries.DUMP_DATABASE_TO(file.getAbsolutePath());
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
	
				String query = DatabaseQueries.DUMP_DATABASE_TO(file.getAbsolutePath(), fromTimestamp);
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
	
	public void debugVersion() {
		jsqlite.Database db = null;
		try {
			
			db = new jsqlite.Database();
			db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READWRITE);
			
			db.exec("select spatialite_version(), sqlite_version(), proj4_version(), geos_version(), lwgeom_version();", new Callback() {
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
				String query = DatabaseQueries.MERGE_DATABASE_FROM(file.getAbsolutePath());
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

	public List<String> runDistanceEntityQuery(Geometry geometry, float distance, String srid) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("run distance query");
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
				stmt = db.prepare(DatabaseQueries.RUN_DISTANCE_ENTITY);
				stmt.bind(1, WKTUtil.geometryToWKT(geometry));
				stmt.bind(2, Integer.parseInt(srid));
				stmt.bind(3, distance);
				stmt.bind(4, Integer.parseInt(srid));
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
	
	public List<String> runDistanceRelationshipQuery(Geometry geometry, float distance, String srid) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("run distance query");
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
				stmt = db.prepare(DatabaseQueries.RUN_DISTANCE_RELATIONSHIP);
				stmt.bind(1, WKTUtil.geometryToWKT(geometry));
				stmt.bind(2, Integer.parseInt(srid));
				stmt.bind(3, distance);
				stmt.bind(4, Integer.parseInt(srid));
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

	public Collection<? extends String> runDistanceLegacyQuery(
			String dbPath, String tableName, String idColumn, String geometryColumn, Geometry geometry, float distance, String srid) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("run distance query");
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbPath, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
				stmt = db.prepare("select " + idColumn + " from " + tableName + " where "+ geometryColumn + " is not null and st_intersects(buffer(transform(GeomFromText(?, 4326), ?), ?), transform("+ geometryColumn + ", ?))");
				stmt.bind(1, WKTUtil.geometryToWKT(geometry));
				stmt.bind(2, Integer.parseInt(srid));
				stmt.bind(3, distance);
				stmt.bind(4, Integer.parseInt(srid));
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
	
	public List<String> runIntersectEntityQuery(Geometry geometry) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("run intersect query");
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
				stmt = db.prepare(DatabaseQueries.RUN_INTERSECT_ENTITY);
				stmt.bind(1, WKTUtil.geometryToWKT(geometry));
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
	
	public List<String> runIntersectRelationshipQuery(Geometry geometry) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("run intersect query");
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbname, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
				stmt = db.prepare(DatabaseQueries.RUN_INTERSECT_RELATIONSHIP);
				stmt.bind(1, WKTUtil.geometryToWKT(geometry));
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
	
	public Collection<? extends String> runIntersectLegacyQuery(
			String dbPath, String tableName, String idColumn, String geometryColumn, Geometry geometry) throws Exception {
		synchronized(DatabaseManager.class) {
			FLog.d("run intersect query");
			Stmt stmt = null;
			try {
				db = new jsqlite.Database();
				db.open(dbPath, jsqlite.Constants.SQLITE_OPEN_READONLY);
				
				stmt = db.prepare("select " + idColumn + " from " + tableName + " where "+ geometryColumn + " is not null and st_intersects(GeomFromText(?, 4326), transform("+ geometryColumn + ", 4326))");
				stmt.bind(1, WKTUtil.geometryToWKT(geometry));
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
	
}
