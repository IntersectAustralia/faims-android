package au.org.intersect.faims.android.beanshell;

import java.util.List;

import android.os.AsyncTask;
import au.org.intersect.faims.android.data.EntityAttribute;
import au.org.intersect.faims.android.data.RelationshipAttribute;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.WKTUtil;
import au.org.intersect.faims.android.util.GeometryUtil;

import com.nutiteq.geometry.Geometry;

public class DatabaseHelper {
	
	protected static void saveArchEnt(final BeanShellLinker linker, final String entityId, final String entityType,
			final List<Geometry> geometry, final List<EntityAttribute> attributes, final SaveCallback callback, final boolean newEntity, boolean blocking) {
		if (blocking) {
			saveArchEnt(linker, entityId, entityType, geometry, attributes, callback, newEntity); 
		} else {
			AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					saveArchEnt(linker, entityId, entityType, geometry, attributes, callback, newEntity);
					return null;
				}
				
			};
			task.execute();
		}
	}
	
	private static void saveArchEnt(final BeanShellLinker linker, final String entityId, final String entityType,
			final List<Geometry> geometry, final List<EntityAttribute> attributes, final SaveCallback callback, final boolean newEntity) {
		try {
			DatabaseManager databaseManager = linker.getDatabaseManager();
			
			List<Geometry> geomList = databaseManager.spatialRecord().convertGeometryFromProjToProj(
					linker.getModule().getSrid(), GeometryUtil.EPSG4326, geometry);
			
			databaseManager.entityRecord().saveArchEnt(entityId,
					entityType, WKTUtil.collectionToWKT(geomList), attributes, newEntity);
			
			linker.getActivity().runOnUiThread(new Runnable() {
	
				@Override
				public void run() {
					if (callback != null) {
						try {
								callback.onSave(entityId, newEntity);
						} catch (Exception e) {
							linker.reportError(e, "Error found when executing save arch entity onsave callback");
						}
					}
				}
				
			});
		} catch (Exception e) {
			String message = "Error trying to save arch entity";
			FLog.e(message, e);
			if (callback != null) {
				try {
					callback.onError(message);
				} catch (Exception ce) {
					linker.reportError(ce, "Error found when executing save arch enitty onerror callback");
				}
			}
		}
	}
	
	protected static void saveRel(final BeanShellLinker linker, final String relationshipId, final String relationshipType,
			final List<Geometry> geometry, final List<RelationshipAttribute> attributes, final SaveCallback callback, final boolean newRelationship, boolean blocking) {
		if (blocking) {
			saveRel(linker, relationshipId, relationshipType, geometry, attributes, callback, newRelationship);
		} else {
			AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					saveRel(linker, relationshipId, relationshipType, geometry, attributes, callback, newRelationship);
					return null;
				}
				
			};
			task.execute();
		}
	}
	
	private static void saveRel(final BeanShellLinker linker, final String relationshipId, final String relationshipType,
			final List<Geometry> geometry, final List<RelationshipAttribute> attributes, final SaveCallback callback, final boolean newRelationship) {
		try {
			DatabaseManager databaseManager = linker.getDatabaseManager();
			
			List<Geometry> geomList = databaseManager.spatialRecord().convertGeometryFromProjToProj(
					linker.getModule().getSrid(), GeometryUtil.EPSG4326, geometry);
			
			databaseManager.relationshipRecord().saveRel(relationshipId, relationshipType,
					WKTUtil.collectionToWKT(geomList), attributes, newRelationship);
			
			linker.getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (callback != null) {
						try {
							callback.onSave(relationshipId, newRelationship);
						} catch (Exception e) {
							linker.reportError(e, "Error found when executing save relationship onsave callback");
						}
					}
				}
				
			});
		} catch (Exception e) {
			String message = "Error trying to save relationship";
			FLog.e(message, e);
			if (callback != null) {
				try {
					callback.onError(message);
				} catch (Exception ce) {
					linker.reportError(ce, "Error found when executing save relationship onerror callback");
				}
			}
		}
	}

}
