package au.org.intersect.faims.android.beanshell;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import au.org.intersect.faims.android.data.ArchEntity;
import au.org.intersect.faims.android.data.EntityAttribute;
import au.org.intersect.faims.android.data.Relationship;
import au.org.intersect.faims.android.data.RelationshipAttribute;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.WKTUtil;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.view.Tab;
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
			
			databaseManager.entityRecord().saveArchEnt(
					entityId, entityType, WKTUtil.collectionToWKT(geomList), attributes, newEntity);
			
			if (callback != null) {
				linker.getActivity().runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						try {
							callback.onSave(entityId, newEntity);
						} catch (Exception e) {
							linker.reportError("Error found when executing save arch enitty onsave callback", e);
						}
						
					}
				});
			}
		} catch (Exception e) {
			onError(linker, callback, e, "Error trying to save arch entity", "Error found when executing save arch enitty onerror callback");
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
			
			if (callback != null) {
				linker.getActivity().runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						try {
							callback.onSave(relationshipId, newRelationship);
						} catch (Exception e) {
							linker.reportError("Error found when executing save relationship onsave callback", e);
						}
					}
					
				});	
			}
		} catch (Exception e) {
			onError(linker, callback, e, "Error trying to save relationship", "Error found when executing save relationship onerror callback");
		}
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
					
					if (callback != null) {
						linker.getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								try {
									callback.onDelete(entityId);
								} catch (Exception e) {
									linker.reportError("Error found when executing delete arch entity ondelete callback", e);
								}
							}
						});
					}
				} catch (Exception e) {
					onError(linker, callback, e, "Error deleting arch entity " + entityId, "Error found when executing delete arch entity onerror callback");
				}
				return null;
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
					
					if (callback != null) {
						linker.getActivity().runOnUiThread(new Runnable() {
	
							@Override
							public void run() {
								try {
									callback.onDelete(relationshipId);
								} catch (Exception e) {
									linker.reportError("Error found when executing delete relationship ondelete callback", e);
								}
							}
						});
					}
				} catch (Exception e) {
					onError(linker, callback, e, "Error deleting relationship " + relationshipId, "Error found when executing delete relationship onerror callback");
				}
				return null;
			}
			
		};
		task.execute();
	}
	
	public static void addReln(final BeanShellLinker linker, final String entityId, final String relationshpId, final String verb, 
			final SaveCallback callback) {
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					linker.getDatabaseManager().sharedRecord().addReln(entityId, relationshpId, verb);
					if (callback != null) {
						linker.getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								try {
									callback.onSaveAssociation(entityId, relationshpId);
								} catch (Exception e) {
									linker.reportError("Error found when executing add reln onsaveassociation callback", e);
								}
							}
							
						});
					}
				} catch (Exception e) {
					onError(linker, callback, e, "Error saving arch entity to relationship", "Error found when executing add reln onerror callback");
				}
				return null;
			}
			
		};
		task.execute();
	}
	
	public static void fetchArchEnt(final BeanShellLinker linker, final String entityId, final FetchCallback callback) {
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					DatabaseManager databaseManager = linker.getDatabaseManager();
					
					final ArchEntity entity = databaseManager.entityRecord().fetchArchEnt(entityId);
					if (entity != null) {
						List<Geometry> geomList = entity.getGeometryList();
						if (geomList != null) {
							entity.setGeometryList(databaseManager.spatialRecord().convertGeometryFromProjToProj(
									GeometryUtil.EPSG4326, linker.getModule().getSrid(), geomList));
						}
					}
					
					onFetch(linker, callback, entity, "Error found when executing fetch arch entity onfetch callback");
				} catch (Exception e) {
					onError(linker, callback, e, "Error fetching arch entity " + entityId, "Error found when executing fetch arch entity onerror callback");
				}
				return null;
			}
			
		};
		task.execute();
	}

	public static void fetchRel(final BeanShellLinker linker, final String relationshipId, final FetchCallback callback) {
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					DatabaseManager databaseManager = linker.getDatabaseManager();
					
					final Relationship relationship = databaseManager.relationshipRecord().fetchRel(relationshipId);
					if (relationship != null) {
						List<Geometry> geomList = relationship.getGeometryList();
						if (geomList != null) {
							relationship.setGeometryList(databaseManager.spatialRecord().convertGeometryFromProjToProj(
									GeometryUtil.EPSG4326, linker.getModule().getSrid(), geomList));
						}
					}
					
					onFetch(linker, callback, relationship, "Error found when executing fetch relationship onfetch callback");
				} catch (Exception e) {
					onError(linker, callback, e, "Error fetching relationship " + relationshipId, "Error found when executing fetch relationship onerror callback");
				}
				return null;
			}
			
		};
		task.execute();
	}

	public static void fetchOne(final BeanShellLinker linker, final String query, final FetchCallback callback) {
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					final ArrayList<String> result = linker.getDatabaseManager().fetchRecord().fetchOne(query);
					onFetch(linker, callback, result, "Error found when executing fetch query result onfetch callback");
				} catch (Exception e) {
					onError(linker, callback, e, "Error fetching query result", "Error found when executing fetch query result onerror callback");
				}
				return null;
			}
		};
		task.execute();		
	}

	public static void fetchAll(final BeanShellLinker linker, final String query, final FetchCallback callback) {
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					final ArrayList<List<String>> result = linker.getDatabaseManager().fetchRecord().fetchAll(query);
					onFetch(linker, callback, result, "Error found when executing fetch query results onfetch callback");
				} catch (Exception e) {
					onError(linker, callback, e, "Error fetching query results", "Error found when executing fetch query results onerror callback");
				}
				return null;
			}
		};
		task.execute();
	}

	public static void fetchEntityList(final BeanShellLinker linker, final String entityType, final FetchCallback callback) {
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					final ArrayList<List<String>> result = linker.getDatabaseManager().fetchRecord().fetchEntityList(entityType);
					onFetch(linker, callback, result, "Error found when executing fetch entity list onfetch callback");
				} catch (Exception e) {
					onError(linker, callback, e, "Error fetching entity list", "Error found when executing fetch entity list onerror callback");
				}
				return null;
			}
		};
		task.execute();
	}

	public static void fetchRelationshipList(final BeanShellLinker linker, final String relationshipType, final FetchCallback callback) {
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					final ArrayList<List<String>> result = linker.getDatabaseManager().fetchRecord().fetchRelationshipList(relationshipType);
					onFetch(linker, callback, result, "Error found when executing fetch relationship list onfetch callback");
				} catch (Exception e) {
					onError(linker, callback, e, "Error fetching relationship list", "Error found when executing fetch relationship list onerror callback");
				}
				return null;
			}
		};
		task.execute();
	}
	
	private static void onFetch(final BeanShellLinker linker, final FetchCallback callback, final Object result, final String callbackErrorMessage) {
		if (callback != null) {
			linker.getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					try {
						callback.onFetch(result);
					} catch (Exception e) {
						linker.reportError(callbackErrorMessage, e);
					}
				}
				
			});
		}
	}
	
	private static void onError(final BeanShellLinker linker, final IBeanShellCallback callback, final Exception e, final String errorMessage, final String callbackErrorMessage) {
		FLog.e(errorMessage, e);
		if (callback != null) {
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

}
