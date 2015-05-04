package au.org.intersect.faims.android.beanshell;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.view.View;
import au.org.intersect.faims.android.beanshell.callbacks.FetchCallback;
import au.org.intersect.faims.android.beanshell.callbacks.IBeanShellCallback;
import au.org.intersect.faims.android.beanshell.callbacks.SaveCallback;
import au.org.intersect.faims.android.data.ArchEntity;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.EntityAttribute;
import au.org.intersect.faims.android.data.Relationship;
import au.org.intersect.faims.android.data.RelationshipAttribute;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.ui.view.CustomFileList;
import au.org.intersect.faims.android.ui.view.ICustomView;
import au.org.intersect.faims.android.ui.view.Tab;
import au.org.intersect.faims.android.ui.view.TabGroup;

import com.nutiteq.geometry.Geometry;

public class TabGroupHelper {
	
	protected static void saveTabGroupInBackground(final BeanShellLinker linker, final String ref, final String uuid, final List<Geometry> geometry, 
			final List<? extends Attribute> attributes, final SaveCallback callback, final boolean newRecord, boolean blocking) {
		if (blocking) {
			saveTabGroupInBackground(linker, ref, uuid, geometry, attributes, callback, newRecord);
		} else {
			AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					saveTabGroupInBackground(linker, ref, uuid, geometry, attributes, callback, newRecord);
					return null;
				}
				
			};
			task.execute();
		}
	}
	
	private static void saveTabGroupInBackground(final BeanShellLinker linker, final String ref, final String uuid, final List<Geometry> geometry, 
			final List<? extends Attribute> attributes, final SaveCallback callback, final boolean newRecord) {
		final AutoSaveManager autoSaveManager = linker.getAutoSaveManager();
		try {
			final TabGroup tabGroup = linker.getTabGroup(ref);
			checkTabGroupForChangesAndSaveTabGroup(linker, tabGroup, uuid, geometry, attributes, new SaveCallback() {
	
				@Override
				public void onError(String message) {
					if (autoSaveManager.isEnabled()) {
						autoSaveManager.reportError();
					} else {
						TabGroupHelper.onError(linker, callback, null, "Error saving tab group " + tabGroup.getRef(), "Error executing save tab group onerror callback");
					}
				}
	
				@Override
				public void onSave(final String uuid, final boolean newRecord) {
					setTabGroupSaved(linker, tabGroup);
					autoSaveManager.reportSaved();
					if (callback != null) {
						linker.getActivity().runOnUiThread(new Runnable() {
	
							@Override
							public void run() {
								try {
									callback.onSave(uuid, newRecord);
								} catch (Exception e) {
									linker.reportError("Error executing save tab group onsave callback", e);
								}
							}
							
						});
					}
				}
	
				@Override
				public void onSaveAssociation(String entityId,
						String relationshpId) {					
				}
				
			}, newRecord);
		} catch (Exception e) {
			FLog.e("error saving tab group in background", e);
			if (autoSaveManager.isEnabled()) {
				autoSaveManager.reportError();
			} else {
				onError(linker, callback, e, "Error saving tab group " + ref, "Error executing save tab group onerror callback");
			}
		}
	}
	
	protected static void saveTabInBackground(final BeanShellLinker linker, final String ref, final String uuid, final List<Geometry> geometry, 
			final List<? extends Attribute> attributes, final SaveCallback callback, final boolean newRecord, boolean blocking) {
		if (blocking) {
			saveTabInBackground(linker, ref, uuid, geometry, attributes, callback, newRecord);
		} else {
			AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					saveTabInBackground(linker, ref, uuid, geometry, attributes, callback, newRecord);
					return null;
				}
				
			};
			task.execute();
		}
	}
	
	private static void saveTabInBackground(final BeanShellLinker linker, final String ref, final String uuid, final List<Geometry> geometry, 
			final List<? extends Attribute> attributes, final SaveCallback callback, final boolean newRecord) {
		try {
			TabGroup tabGroup = linker.getTabGroupFromRef(ref);
			final Tab tab = linker.getTab(ref);
			saveTab(linker, tabGroup, tab, uuid, geometry, attributes, new SaveCallback() {

				@Override
				public void onError(String message) {
					TabGroupHelper.onError(linker, callback, null, "Error saving tab " + ref, "Error executing save tab onerror callback");
				}

				@Override
				public void onSave(final String uuid, final boolean newRecord) {
					setTabSaved(linker, tab);
					if (callback != null) {
						linker.getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								try {
									callback.onSave(uuid, newRecord);
								} catch (Exception e) {
									linker.reportError("Error executing save tab onsave callback", e);
								}
							}
							
						});
					}
				}

				@Override
				public void onSaveAssociation(String entityId,
						String relationshpId) {
				}
				
			}, newRecord);
		} catch (Exception e) {
			onError(linker, callback, e, "Error saving tab " + ref, "Error executing save tab onerror callback");
		}
	}
	
	private static void checkTabGroupForChangesAndSaveTabGroup(final BeanShellLinker linker, final TabGroup tabGroup, final String uuid, final List<Geometry> geometry, 
			final List<? extends Attribute> attributes, final SaveCallback callback, final boolean newRecord) throws Exception {
		final AutoSaveManager autoSaveManager = linker.getAutoSaveManager();
		if (tabGroup.hasChanges()) {
			if (newRecord || tabGroup.hasRecord(uuid)) {
				collectAttributesAndSaveTabGroup(linker, tabGroup, uuid, geometry, attributes, callback, newRecord);
			} else {
				loadRecordAndSaveTabGroup(linker, tabGroup, uuid, geometry, attributes, callback, newRecord);
			}
		} else {
			autoSaveManager.reportSaved();
		}
	}
		
	private static void loadRecordAndSaveTabGroup(final BeanShellLinker linker, final TabGroup tabGroup, final String uuid, final List<Geometry> geometry, 
			final List<? extends Attribute> attributes, final SaveCallback callback, final boolean newRecord) throws Exception {
		final AutoSaveManager autoSaveManager = linker.getAutoSaveManager();
		if (tabGroup.getArchEntType() != null) {
			DatabaseHelper.fetchArchEnt(linker, uuid, new FetchCallback() {
				
				@Override
				public void onError(String message) {
					if (autoSaveManager.isEnabled()) {
						autoSaveManager.reportError();
					} else {
						TabGroupHelper.onError(linker, callback, null, "Error saving tab group " + tabGroup.getRef(), "Error executing save tab group onerror callback");
					}
				}
	
				@Override
				public void onFetch(Object result) {
					try {
						ArchEntity entity = (ArchEntity) result;
						// cache entity
						tabGroup.setArchEntity(entity);
						collectAttributesAndSaveTabGroup(linker, tabGroup, uuid, geometry, attributes, callback, newRecord);
					} catch (Exception e) {
						if (autoSaveManager.isEnabled()) {
							autoSaveManager.reportError();
						} else {
							TabGroupHelper.onError(linker, callback, e, "Error saving tab group " + tabGroup.getRef(), "Error executing save tab group onerror callback");
						}
					}
				}
				
			}, true);
		} else if (tabGroup.getRelType() != null) {
			DatabaseHelper.fetchRel(linker, uuid, new FetchCallback() {
				
				@Override
				public void onError(String message) {
					if (autoSaveManager.isEnabled()) {
						autoSaveManager.reportError();
					} else {
						TabGroupHelper.onError(linker, callback, null, "Error saving tab group " + tabGroup.getRef(), "Error executing save tab group onerror callback");
					}
				}
	
				@Override
				public void onFetch(Object result) {
					try {
						Relationship relationship = (Relationship) result;
						// cache relationship
						tabGroup.setRelationship(relationship);
						collectAttributesAndSaveTabGroup(linker, tabGroup, uuid, geometry, attributes, callback, newRecord);
					} catch (Exception e) {
						if (autoSaveManager.isEnabled()) {
							autoSaveManager.reportError();
						} else {
							TabGroupHelper.onError(linker, callback, e, "Error saving tab group " + tabGroup.getRef(), "Error executing save tab group onerror callback");
						}
					}
				}
				
			}, true);
		} else {
			throw new Exception("no type specified for tabgroup");
		}
	}
	
	private static void collectAttributesAndSaveTabGroup(BeanShellLinker linker, final TabGroup tabGroup, String uuid, List<Geometry> geometry, 
			List<? extends Attribute> attributes, final SaveCallback callback, boolean newRecord) throws Exception {
		synchronized(TabGroupHelper.class) {
			if (tabGroup.getArchEntType() != null) {
				collectEntityAttributesAndSaveEntity(linker, tabGroup, uuid, geometry, attributes, callback, newRecord, null);
			} else if (tabGroup.getRelType() != null) {
				collectRelationshipAttributesAndSaveRelationship(linker, tabGroup, uuid, geometry, attributes, callback, newRecord, null);
			} else {
				throw new Exception("no type specified for tabgroup");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void collectEntityAttributesAndSaveEntity(BeanShellLinker linker, final TabGroup tabGroup, String uuid, List<Geometry> geometry, 
			List<? extends Attribute> attributes, final SaveCallback callback, boolean newRecord, List<String> excludeAttributes) {
		List<EntityAttribute> entityAttributes = new ArrayList<EntityAttribute>();
		if (attributes != null) {
			entityAttributes.addAll((List<EntityAttribute>) attributes);
		}
		entityAttributes.addAll(getEntityAttributesFromTabGroup(linker, tabGroup));	
		
		makeExcludeAttributesBlankValues(entityAttributes, excludeAttributes);
		
		final List<EntityAttribute> updatedAttributes = entityAttributes;
		if (geometry != null || !entityAttributes.isEmpty()) {
			DatabaseHelper.saveArchEnt(linker, uuid, tabGroup.getArchEntType(), geometry, entityAttributes, new SaveCallback() {

				@Override
				public void onError(String message) {
					callback.onError(message);
				}

				@Override
				public void onSave(String uuid, boolean newRecord) {
					// update cached entity
					ArchEntity entity = tabGroup.getArchEntity();
					if (entity != null) {
						entity.updateAttributes(updatedAttributes);
					}
					callback.onSave(uuid, newRecord);
				}

				@Override
				public void onSaveAssociation(String entityId,
						String relationshpId) {
				}
				
			}, newRecord, true);
		} else {
			callback.onSave(uuid,  newRecord);
		}
	}

	@SuppressWarnings("unchecked")
	private static void collectRelationshipAttributesAndSaveRelationship(BeanShellLinker linker, final TabGroup tabGroup, String uuid, List<Geometry> geometry, 
			List<? extends Attribute> attributes, final SaveCallback callback, boolean newRecord, List<String> excludeAttributes) {
		List<RelationshipAttribute> relationshipAttributes = new ArrayList<RelationshipAttribute>();
		if (attributes != null) {
			relationshipAttributes.addAll((List<RelationshipAttribute>) attributes);
		}
		relationshipAttributes.addAll(getRelationshipAttributesFromTabGroup(linker, tabGroup));
		
		makeExcludeAttributesBlankValues(relationshipAttributes, excludeAttributes);
		
		final List<RelationshipAttribute> updatedAttributes = relationshipAttributes;
		if (geometry != null || !relationshipAttributes.isEmpty()) {
			DatabaseHelper.saveRel(linker, uuid, tabGroup.getRelType(), geometry, relationshipAttributes, new SaveCallback() {

				@Override
				public void onError(String message) {
					callback.onError(message);
				}

				@Override
				public void onSave(String uuid, boolean newRecord) {
					// update cached relationship
					Relationship relationship = tabGroup.getRelationship();
					if (relationship != null) {
						relationship.updateAttributes(updatedAttributes);
					}
					callback.onSave(uuid, newRecord);
				}

				@Override
				public void onSaveAssociation(String entityId,
						String relationshpId) {
				}
				
			}, newRecord, true);
		} else {
			callback.onSave(uuid,  newRecord);
		}
	}
	
	private static void makeExcludeAttributesBlankValues(
			List<? extends Attribute> attributes,
			List<String> excludeAttributes) {
		if (attributes == null || excludeAttributes == null) return;
		
		for (Attribute a : attributes) {
			for (String ea : excludeAttributes) {
				if (a.getName().equals(ea)) {
					a.blank();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void saveTab(BeanShellLinker linker, TabGroup tabGroup, Tab tab, String uuid, List<Geometry> geometry, 
			List<? extends Attribute> attributes, SaveCallback callback, boolean newRecord) throws Exception {
		synchronized(TabGroupHelper.class) {
			if (tab.hasChanges()) {
				if (tabGroup.getArchEntType() != null) {
					List<EntityAttribute> entityAttributes = new ArrayList<EntityAttribute>();
					if (attributes != null) {
						entityAttributes.addAll((List<EntityAttribute>) attributes);
					}
					entityAttributes.addAll(TabGroupHelper.getEntityAttributesFromTab(linker, tab, tabGroup.getArchEntity()));
					if (geometry != null || !entityAttributes.isEmpty()) {
						DatabaseHelper.saveArchEnt(linker, uuid, tabGroup.getArchEntType(), geometry, entityAttributes, callback, newRecord, true);
					} else {
						callback.onSave(uuid, newRecord);
					}
				} else if (tabGroup.getRelType() != null) {			
					List<RelationshipAttribute> relationshipAttributes = new ArrayList<RelationshipAttribute>();
					if (attributes != null) {
						relationshipAttributes.addAll((List<RelationshipAttribute>) attributes);
					}		
					relationshipAttributes.addAll(TabGroupHelper.getRelationshipAttributesFromTab(linker, tab, tabGroup.getRelationship()));	
					if (geometry != null || !relationshipAttributes.isEmpty()) {
						DatabaseHelper.saveRel(linker, uuid, tabGroup.getRelType(), geometry, relationshipAttributes, callback, newRecord, true);
					} else {
						callback.onSave(uuid, newRecord);
					}
				} else {
					throw new Exception("no type specified for tabgroup");
				}
			}
		}
	}
	
	private static void setTabGroupSaved(BeanShellLinker linker, TabGroup tabGroup) {
		List<Tab> tabs = tabGroup.getTabs();
		for (Tab tab : tabs) {
			setTabSaved(linker, tab);
		}
	}
	
	private static void setTabSaved(final BeanShellLinker linker, final Tab tab) {
		final List<View> views = tab.getAttributeViews();
		for (View view : views) {
			if (view instanceof ICustomView) {
				ICustomView customView = (ICustomView) view;
				customView.save();
			}
		}
		// reload files views to get updated paths
		linker.getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				for (View view : views) {
					if (view instanceof CustomFileList) {
						CustomFileList fileView = (CustomFileList) view;
						fileView.reload();
					}
				}
			}
			
		});
	}
	
	private static List<EntityAttribute> getEntityAttributesFromTabGroup(BeanShellLinker linker, TabGroup tabGroup) {
		List<EntityAttribute> attributes = new ArrayList<EntityAttribute>();
		for (Tab tab : tabGroup.getTabs()) {
			attributes.addAll(getEntityAttributesFromTab(linker, tab, tabGroup.getArchEntity()));
		}
		return attributes;
	}
	
	private static List<EntityAttribute> getEntityAttributesFromTab(BeanShellLinker linker, Tab tab, ArchEntity entity) {
		return AttributeHelper.getEntityAttributes(linker, tab, entity);
	}

	private static List<RelationshipAttribute> getRelationshipAttributesFromTabGroup(BeanShellLinker linker, TabGroup tabGroup) {
		List<RelationshipAttribute> attributes = new ArrayList<RelationshipAttribute>();
		for (Tab tab : tabGroup.getTabs()) {
			attributes.addAll(getRelationshipAttributesFromTab(linker, tab, tabGroup.getRelationship()));
		}
		return attributes;
	}
	
	private static List<RelationshipAttribute> getRelationshipAttributesFromTab(BeanShellLinker linker, Tab tab, Relationship relationship) {
		return AttributeHelper.getRelationshipAttributes(linker, tab, relationship);
	}
	
	protected static void showArchEntityTabGroup(final BeanShellLinker linker, final String entityId, final TabGroup tabGroup, final FetchCallback callback) {
		linker.fetchArchEnt(entityId, new FetchCallback() {

			@Override
			public void onError(String message) {
				onWarning(linker, callback, "Loading Error", "Error trying to load arch entity", "Error found when executing show tab group onerror callback");
			}

			@Override
			public void onFetch(Object result) {
				final ArchEntity entity = (ArchEntity) result;
				if (entity instanceof ArchEntity) {
					for (Tab tab : tabGroup.getTabs()) {
						showArchEntityTab(linker, entity, tab);
					}
					
					if (callback != null) {
						linker.getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								try {
									callback.onFetch(entity);
								} catch (Exception e) {
									linker.reportError("Error found when executing show tab group onfetch callback", e);
								}
							}
							
						});
					}
				} else {
					onWarning(linker, callback, "Loading Error", "Error trying to load arch entity", "Error found when executing show tab group onerror callback");
				}
			}
			
		});
		
	}

	protected static void showRelationshipTabGroup(final BeanShellLinker linker, final String relationshipId, final TabGroup tabGroup, final FetchCallback callback)  {
		linker.fetchRel(relationshipId, new FetchCallback() {

			@Override
			public void onError(String message) {
				onWarning(linker, callback, "Loading Error", "Error trying to load relationship", "Error found when executing show tab group onerror callback");
			}

			@Override
			public void onFetch(Object result) {
				final Relationship relationship = (Relationship) result;
				if (relationship instanceof Relationship) {
					for (Tab tab : tabGroup.getTabs()) {
						showRelationshipTab(linker, relationship, tab);
					}
					
					if (callback != null) {
						linker.getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								try {
									callback.onFetch(relationship);
								} catch (Exception e) {
									linker.reportError("Error found when executing show tab group onfetch callback", e);
								}
							}
							
						});
					}
				} else {
					onWarning(linker, callback, "Loading Error", "Error trying to load relationship", "Error found when executing show tab group onerror callback");
				}
			}
			
		});
	}

	protected static void showArchEntityTab(final BeanShellLinker linker, final String entityId, final Tab tab, final FetchCallback callback) throws Exception {
		linker.fetchArchEnt(entityId, new FetchCallback() {

			@Override
			public void onError(String message) {
				onWarning(linker, callback, "Loading Error", "Error trying to load arch entity", "Error found when executing show tab onerror callback");
			}

			@Override
			public void onFetch(Object result) {
				final ArchEntity entity = (ArchEntity) result;
				if (entity instanceof ArchEntity) {
					showArchEntityTab(linker, entity, tab);
					
					if (callback != null) {
						linker.getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								try {
									callback.onFetch(entity);
								} catch (Exception e) {
									linker.reportError("Error found when executing show tab onfetch callback", e);
								}
							}
							
						});
					}
				} else {
					linker.showWarning("Loading Error", "Error trying to load arch entity");
				}
			}
			
		});
	}
	
	protected static void showRelationshipTab(final BeanShellLinker linker, final String relationshipId, final Tab tab, final FetchCallback callback) throws Exception {
		linker.fetchRel(relationshipId, new FetchCallback() {

			@Override
			public void onError(String message) {
				onWarning(linker, callback, "Loading Error", "Error trying to load relationship", "Error found when executing show tab onerror callback");
			}

			@Override
			public void onFetch(Object result) {
				final Relationship relationship = (Relationship) result;
				if (relationship instanceof Relationship) {
					showRelationshipTab(linker, relationship, tab);
					
					if (callback != null) {
						linker.getActivity().runOnUiThread(new Runnable() {

							@Override
							public void run() {
								try {
									callback.onFetch(relationship);
								} catch (Exception e) {
									linker.reportError("Error found when executing show tab onfetch callback", e);
								}
							}
							
						});
					}
				} else {
					linker.showWarning("Loading Error", "Error trying to load relationship");
				}
			}
			
		});
	}
	
	private static void showArchEntityTab(BeanShellLinker linker, ArchEntity archEntity, Tab tab) {
		tab.clearViews();
		AttributeHelper.showArchEntityTab(linker, archEntity, tab);
	}
	
	private static void showRelationshipTab(BeanShellLinker linker, Relationship relationship, Tab tab) {
		tab.clearViews();
		AttributeHelper.showRelationshipTab(linker, relationship, tab);
	}
	
	private static void onWarning(final BeanShellLinker linker, final IBeanShellCallback callback, final String title, final String errorMessage, final String callbackErrorMessage) {
		FLog.w(errorMessage);
		linker.showWarning(title, errorMessage);
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
	
	private static void onError(final BeanShellLinker linker, final IBeanShellCallback callback, final Exception e, final String errorMessage, final String callbackErrorMessage) {
		if (e == null) {
			FLog.e(errorMessage);
		} else {
			FLog.e(errorMessage, e);
		}
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
	
	protected static void duplicateTabGroupInBackground(final BeanShellLinker linker, final String ref, final List<Geometry> geometry, 
			final List<? extends Attribute> attributes, final List<String> excludeAttributes, final SaveCallback callback) {
		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					final TabGroup tabGroup = linker.getTabGroup(ref);
					collectEntityAttributesAndSaveEntity(linker, tabGroup, null, geometry, attributes, callback, true, excludeAttributes);
				} catch (Exception e) {
					TabGroupHelper.onError(linker, callback, e, "Error duplicating tab group " + ref, "Error executing duplicate tab group onerror callback");
				}
				return null;
			}
			
		};
		task.execute();
	}

}
