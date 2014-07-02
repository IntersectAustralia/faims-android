package au.org.intersect.faims.android.beanshell;

import java.util.List;

import android.os.AsyncTask;
import au.org.intersect.faims.android.data.EntityAttribute;
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
			final String message = "Error trying to save arch entity";
			FLog.e(message, e);
			if (callback != null) {
				linker.getActivity().runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						try {
							callback.onError(message);
						} catch (Exception ce) {
							linker.reportError("Error found when executing save arch enitty onerror callback", ce);
						}
						
					}
					
				});
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
			final String message = "Error trying to save relationship";
			FLog.e(message, e);
			if (callback != null) {
				linker.getActivity().runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						try {
							callback.onError(message);
						} catch (Exception ce) {
							linker.reportError("Error found when executing save relationship onerror callback", ce);
						}
					}
					
				});
			}
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
					final String message = "Error deleting arch entity";
					FLog.e(message, e);
					if (callback != null) {
						linker.getActivity().runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								try {
									callback.onError(message);
								} catch (Exception ce) {
									linker.reportError("Error found when executing delete arch entity onerror callback", ce);
								}
							}
						});
					}
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
					final String message = "Error deleting relationship";
					FLog.e(message, e);
					if (callback != null) {
						linker.getActivity().runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								try {
									callback.onError(message);
								} catch (Exception ce) {
									linker.reportError("Error found when executing delete relationship onerror callback", ce);
								}
							}
						});
					}
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
					final String message = "Error saving arch entity to relationship";
					FLog.e(message, e);
					if (callback != null) {
						linker.getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								try {
									callback.onError(message);
								} catch (Exception e) {
									linker.reportError("Error found when executing add reln onerror callback", e);
								}
							}
							
						});
					}
				}
				return null;
			}
			
		};
		task.execute();
	}

}
