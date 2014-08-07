package au.org.intersect.faims.android.beanshell;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import au.org.intersect.faims.android.beanshell.callbacks.DeleteCallback;
import au.org.intersect.faims.android.beanshell.callbacks.FetchCallback;
import au.org.intersect.faims.android.beanshell.callbacks.IBeanShellCallback;
import au.org.intersect.faims.android.beanshell.callbacks.SaveCallback;
import au.org.intersect.faims.android.data.ArchEntity;
import au.org.intersect.faims.android.data.EntityAttribute;
import au.org.intersect.faims.android.data.Relationship;
import au.org.intersect.faims.android.data.RelationshipAttribute;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.WKTUtil;
import au.org.intersect.faims.android.tasks.CancelableTask;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.view.Tab;
import au.org.intersect.faims.android.util.GeometryUtil;

import com.nutiteq.geometry.Geometry;

public class DatabaseHelper {
	
	protected static void saveArchEnt(final BeanShellLinker linker, final String entityId, final String entityType,
			final List<Geometry> geometry, final List<EntityAttribute> attributes, final SaveCallback callback, final boolean newEntity, boolean blocking) {
		if (blocking) {
			final String uuid = saveArchEnt(linker, entityId, entityType, geometry, attributes, callback, newEntity); 
			if (callback != null) {
				linker.getActivity().runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						try {
							callback.onSave(uuid, newEntity);
						} catch (Exception e) {
							linker.reportError("Error found when executing save arch enitty " + entityId + " onsave callback", e);
						}
						
					}
				});
			}
		} else {
			AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

				private String uuid;

				@Override
				protected Void doInBackground(Void... params) {
					uuid = saveArchEnt(linker, entityId, entityType, geometry, attributes, callback, newEntity);
					return null;
				}
				
				@Override
				protected void onPostExecute(Void result) {
					if (uuid == null) return;
					if (callback == null) return;
					try {
						callback.onSave(uuid, newEntity);
					} catch (Exception e) {
						linker.reportError("Error found when executing save arch enitty " + entityId + " onsave callback", e);
					}
				}
				
			};
			task.execute();
		}
	}
	
	private static String saveArchEnt(final BeanShellLinker linker, final String entityId, final String entityType,
			final List<Geometry> geometry, final List<EntityAttribute> attributes, final SaveCallback callback, final boolean newEntity) {
		try {
			DatabaseManager databaseManager = linker.getDatabaseManager();
			
			List<Geometry> geomList = databaseManager.spatialRecord().convertGeometryFromProjToProj(
					linker.getModule().getSrid(), GeometryUtil.EPSG4326, geometry);
			
			final String uuid = databaseManager.entityRecord().saveArchEnt(
					entityId, entityType, WKTUtil.collectionToWKT(geomList), attributes, newEntity);
			
			if (uuid == null) {
				throw new Exception();
			}
			
			return uuid;
		} catch (Exception e) {
			onError(linker, callback, e, "Error trying to save arch entity", "Error found when executing save arch enitty " + entityId + " onerror callback");
		}
		return null;
	}
	
	protected static void saveRel(final BeanShellLinker linker, final String relationshipId, final String relationshipType,
			final List<Geometry> geometry, final List<RelationshipAttribute> attributes, final SaveCallback callback, final boolean newRelationship, boolean blocking) {
		if (blocking) {
			final String uuid = saveRel(linker, relationshipId, relationshipType, geometry, attributes, callback, newRelationship);
			if (callback != null) {
				linker.getActivity().runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						try {
							callback.onSave(uuid, newRelationship);
						} catch (Exception e) {
							linker.reportError("Error found when executing save relationship " + relationshipId + " onsave callback", e);
						}
					}
					
				});	
			}
		} else {
			AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

				private String uuid;

				@Override
				protected Void doInBackground(Void... params) {
					uuid = saveRel(linker, relationshipId, relationshipType, geometry, attributes, callback, newRelationship);
					return null;
				}
				
				@Override
				protected void onPostExecute(Void result) {
					if (uuid == null) return;
					if (callback == null) return;
					try {
						callback.onSave(uuid, newRelationship);
					} catch (Exception e) {
						linker.reportError("Error found when executing save relationship " + relationshipId + " onsave callback", e);
					}
				}
			};
			task.execute();
		}
	}
	
	private static String saveRel(final BeanShellLinker linker, final String relationshipId, final String relationshipType,
			final List<Geometry> geometry, final List<RelationshipAttribute> attributes, final SaveCallback callback, final boolean newRelationship) {
		try {
			DatabaseManager databaseManager = linker.getDatabaseManager();
			
			List<Geometry> geomList = databaseManager.spatialRecord().convertGeometryFromProjToProj(
					linker.getModule().getSrid(), GeometryUtil.EPSG4326, geometry);
			
			final String uuid = databaseManager.relationshipRecord().saveRel(relationshipId, relationshipType,
					WKTUtil.collectionToWKT(geomList), attributes, newRelationship);
			
			if (uuid == null) {
				throw new Exception();
			}
			
			return uuid;
		} catch (Exception e) {
			onError(linker, callback, e, "Error trying to save relationship", "Error found when executing save relationship " + relationshipId + " onerror callback");
		}
		return null;
	}
	
	protected static void deleteArchEnt(final BeanShellLinker linker, final String entityId, final DeleteCallback callback) {
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					linker.getDatabaseManager().entityRecord().deleteArchEnt(entityId);
					
					for(Tab tab : linker.getUIRenderer().getTabList()){
						for(CustomMapView mapView : tab.getMapViewList()){
							mapView.removeFromAllSelections(entityId);
							mapView.updateSelections();
						}
					}
				} catch (Exception e) {
					onError(linker, callback, e, "Error deleting arch entity " + entityId, "Error found when executing delete arch entity " + entityId + " onerror callback");
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				if (callback == null) return;
				try {
					callback.onDelete(entityId);
				} catch (Exception e) {
					linker.reportError("Error found when executing delete arch entity " + entityId + " ondelete callback", e);
				}
			}
			
		};
		task.execute();
	}

	protected static void deleteRel(final BeanShellLinker linker, final String relationshipId, final DeleteCallback callback){
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					linker.getDatabaseManager().relationshipRecord().deleteRel(relationshipId);
					
					for(Tab tab : linker.getUIRenderer().getTabList()){
						for(CustomMapView mapView : tab.getMapViewList()){
							mapView.removeFromAllSelections(relationshipId);
							mapView.updateSelections();
						}
					}
				} catch (Exception e) {
					onError(linker, callback, e, "Error deleting relationship " + relationshipId, "Error found when executing delete relationship " + relationshipId + " onerror callback");
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				if (callback == null) return;
				try {
					callback.onDelete(relationshipId);
				} catch (Exception e) {
					linker.reportError("Error found when executing delete relationship " + relationshipId + " ondelete callback", e);
				}
			}
			
		};
		task.execute();
	}
	
	public static void addReln(final BeanShellLinker linker, final String entityId, final String relationshpId, final String verb, 
			final SaveCallback callback) {
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

			private boolean added;

			@Override
			protected Void doInBackground(Void... params) {
				try {
					added = linker.getDatabaseManager().sharedRecord().addReln(entityId, relationshpId, verb);
					if (added == false) {
						throw new Exception();
					}
				} catch (Exception e) {
					onError(linker, callback, e, "Error saving arch entity to relationship", "Error found when executing add reln onerror callback");
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				if (!added) return;
				if (callback == null) return;
				try {
					callback.onSaveAssociation(entityId, relationshpId);
				} catch (Exception e) {
					linker.reportError("Error found when executing add reln onsaveassociation callback", e);
				}
			}
			
		};
		task.execute();
	}
	
	public static void fetchArchEnt(final BeanShellLinker linker, final String entityId, final FetchCallback callback) {
		CancelableTask task = new CancelableTask() {

			private ArchEntity entity;

			@Override
			protected Void doInBackground(Void... params) {
				try {
					DatabaseManager databaseManager = linker.getDatabaseManager();
					
					entity = databaseManager.entityRecord().fetchArchEnt(entityId);
					if (entity == null) {
						throw new Exception();
					} else {
						List<Geometry> geomList = entity.getGeometryList();
						if (geomList != null) {
							entity.setGeometryList(databaseManager.spatialRecord().convertGeometryFromProjToProj(
									GeometryUtil.EPSG4326, linker.getModule().getSrid(), geomList));
						}
					}
				} catch (Exception e) {
					onError(linker, callback, e, "Error fetching arch entity " + entityId, "Error found when executing fetch arch entity " + entityId + " onerror callback");
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				if (entity == null) return;
				onFetch(linker, callback, entity, "Error found when executing fetch arch entity " + entityId + " onfetch callback");
			}
			
		};
		task.execute();
	}

	public static void fetchRel(final BeanShellLinker linker, final String relationshipId, final FetchCallback callback) {
		CancelableTask task = new CancelableTask() {

			private Relationship relationship;

			@Override
			protected Void doInBackground(Void... params) {
				try {
					DatabaseManager databaseManager = linker.getDatabaseManager();
					
					relationship = databaseManager.relationshipRecord().fetchRel(relationshipId);
					if (relationship == null) {
						throw new Exception();
					} else {
						List<Geometry> geomList = relationship.getGeometryList();
						if (geomList != null) {
							relationship.setGeometryList(databaseManager.spatialRecord().convertGeometryFromProjToProj(
									GeometryUtil.EPSG4326, linker.getModule().getSrid(), geomList));
						}
					}
				} catch (Exception e) {
					onError(linker, callback, e, "Error fetching relationship " + relationshipId, "Error found when executing fetch relationship " + relationshipId + " onerror callback");
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				if (relationship == null) return;
				onFetch(linker, callback, relationship, "Error found when executing fetch relationship " + relationshipId + " onfetch callback");
			}
			
		};
		task.execute();
	}

	public static void fetchOne(final BeanShellLinker linker, final String query, final FetchCallback callback) {
		CancelableTask task = new CancelableTask() {

			private ArrayList<String> results;

			@Override
			protected Void doInBackground(Void... params) {
				try {
					results = linker.getDatabaseManager().fetchRecord().fetchOne(query);
				} catch (Exception e) {
					onError(linker, callback, e, "Error fetching query result", "Error found when executing fetch " + query + " result onerror callback");
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				onFetch(linker, callback, results, "Error found when executing fetch " + query + " result onfetch callback");
			}
		};
		task.execute();		
	}

	public static void fetchAll(final BeanShellLinker linker, final String query, final FetchCallback callback) {
		CancelableTask task = new CancelableTask() {

			private ArrayList<List<String>> results;

			@Override
			protected Void doInBackground(Void... params) {
				try {
					results = linker.getDatabaseManager().fetchRecord().fetchAll(query);
				} catch (Exception e) {
					onError(linker, callback, e, "Error fetching query results", "Error found when executing fetch " + query + " results onerror callback");
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				onFetch(linker, callback, results, "Error found when executing fetch " + query + " results onfetch callback");
			}
		};
		task.execute();
	}

	public static void fetchEntityList(final BeanShellLinker linker, final String entityType, final FetchCallback callback) {
		CancelableTask task = new CancelableTask() {

			private ArrayList<List<String>> results;

			@Override
			protected Void doInBackground(Void... params) {
				try {
					results = linker.getDatabaseManager().fetchRecord().fetchEntityList(entityType);
				} catch (Exception e) {
					onError(linker, callback, e, "Error fetching entity list", "Error found when executing fetch entity  " + entityType + " list onerror callback");
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				onFetch(linker, callback, results, "Error found when executing fetch entity " + entityType + " list onfetch callback");
			}
		};
		task.execute();
	}

	public static void fetchRelationshipList(final BeanShellLinker linker, final String relationshipType, final FetchCallback callback) {
		CancelableTask task = new CancelableTask() {

			private ArrayList<List<String>> results;

			@Override
			protected Void doInBackground(Void... params) {
				try {
					results = linker.getDatabaseManager().fetchRecord().fetchRelationshipList(relationshipType);
				} catch (Exception e) {
					onError(linker, callback, e, "Error fetching relationship list", "Error found when executing fetch relationship " + relationshipType + " list onerror callback");
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				onFetch(linker, callback, results, "Error found when executing fetch relationship " + relationshipType + " list onfetch callback");
			}
		};
		task.execute();
	}
	
	private static void onFetch(final BeanShellLinker linker, final FetchCallback callback, final Object result, final String callbackErrorMessage) {
		if (callback == null) return;
		try {
			callback.onFetch(result);
		} catch (Exception e) {
			linker.reportError(callbackErrorMessage, e);
		}
	}
	
	private static void onError(final BeanShellLinker linker, final IBeanShellCallback callback, final Exception e, final String errorMessage, final String callbackErrorMessage) {
		FLog.e(errorMessage, e);
		if (callback == null) return;
		linker.getActivity().runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				try {
					callback.onError(errorMessage);
				} catch (Exception e) {
					linker.reportError(callbackErrorMessage, e);
				}
			}
			
		});
	}

}
