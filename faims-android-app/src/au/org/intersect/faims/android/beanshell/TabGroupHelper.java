package au.org.intersect.faims.android.beanshell;

import java.util.ArrayList;
import java.util.List;

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
import au.org.intersect.faims.android.tasks.CancelableTask;
import au.org.intersect.faims.android.ui.view.CameraPictureGallery;
import au.org.intersect.faims.android.ui.view.FileListGroup;
import au.org.intersect.faims.android.ui.view.ICustomFileView;
import au.org.intersect.faims.android.ui.view.ICustomView;
import au.org.intersect.faims.android.ui.view.Tab;
import au.org.intersect.faims.android.ui.view.TabGroup;
import au.org.intersect.faims.android.ui.view.VideoGallery;

import com.nutiteq.geometry.Geometry;

public class TabGroupHelper {
	
	protected static void saveTabGroupInBackground(final BeanShellLinker linker, final String ref, final String uuid, final List<Geometry> geometry, 
			final List<? extends Attribute> attributes, final SaveCallback callback, final boolean newRecord, boolean blocking) {
		if (blocking) {
			saveTabGroupInBackground(linker, ref, uuid, geometry, attributes, callback, newRecord);
		} else {
			CancelableTask task = new CancelableTask() {

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
			saveTabGroup(linker, tabGroup, uuid, geometry, attributes, new SaveCallback() {
	
				@Override
				public void onError(String message) {
					if (autoSaveManager.isEnabled()) {
						autoSaveManager.notifyError();
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
				autoSaveManager.notifyError();
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
			CancelableTask task = new CancelableTask() {

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
	
	private static void saveTabGroup(final BeanShellLinker linker, final TabGroup tabGroup, final String uuid, final List<Geometry> geometry, 
			final List<? extends Attribute> attributes, final SaveCallback callback, final boolean newRecord) throws Exception {
		final AutoSaveManager autoSaveManager = linker.getAutoSaveManager();
		if (tabGroup.hasChanges()) {
			if (newRecord || tabGroup.hasRecord(uuid)) {
				saveTabGroupAttributes(linker, tabGroup, uuid, geometry, attributes, callback, newRecord);
			} else {
				if (tabGroup.getArchEntType() != null) {
					linker.fetchArchEnt(uuid, new FetchCallback() {
						
						@Override
						public void onError(String message) {
							if (autoSaveManager.isEnabled()) {
								autoSaveManager.notifyError();
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
								saveTabGroupAttributes(linker, tabGroup, uuid, geometry, attributes, callback, newRecord);
							} catch (Exception e) {
								if (autoSaveManager.isEnabled()) {
									autoSaveManager.notifyError();
								} else {
									TabGroupHelper.onError(linker, callback, e, "Error saving tab group " + tabGroup.getRef(), "Error executing save tab group onerror callback");
								}
							}
						}
						
					});
				} else if (tabGroup.getRelType() != null) {
					linker.fetchRel(uuid, new FetchCallback() {
						
						@Override
						public void onError(String message) {
							if (autoSaveManager.isEnabled()) {
								autoSaveManager.notifyError();
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
								saveTabGroupAttributes(linker, tabGroup, uuid, geometry, attributes, callback, newRecord);
							} catch (Exception e) {
								if (autoSaveManager.isEnabled()) {
									autoSaveManager.notifyError();
								} else {
									TabGroupHelper.onError(linker, callback, e, "Error saving tab group " + tabGroup.getRef(), "Error executing save tab group onerror callback");
								}
							}
						}
						
					});
				} else {
					throw new Exception("no type specified for tabgroup");
				}
			}
		} else {
			callback.onSave(uuid,  newRecord);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void saveTabGroupAttributes(BeanShellLinker linker, final TabGroup tabGroup, String uuid, List<Geometry> geometry, 
			List<? extends Attribute> attributes, final SaveCallback callback, boolean newRecord) throws Exception {
		synchronized(TabGroupHelper.class) {
			if (tabGroup.getArchEntType() != null) {
				List<EntityAttribute> entityAttributes = new ArrayList<EntityAttribute>();
				if (attributes != null) {
					entityAttributes.addAll((List<EntityAttribute>) attributes);
				}
				entityAttributes.addAll(getEntityAttributesFromTabGroup(linker, tabGroup));	
				
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
			} else if (tabGroup.getRelType() != null) {
				List<RelationshipAttribute> relationshipAttributes = new ArrayList<RelationshipAttribute>();
				if (attributes != null) {
					relationshipAttributes.addAll((List<RelationshipAttribute>) attributes);
				}
				relationshipAttributes.addAll(getRelationshipAttributesFromTabGroup(linker, tabGroup));
				
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
			} else {
				throw new Exception("no type specified for tabgroup");
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
					if (view instanceof ICustomFileView) {
						ICustomFileView fileView = (ICustomFileView) view;
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
		for (EntityAttribute attribute : archEntity.getAttributes()) {
			if (tab.hasView(attribute.getName())) {
				List<View> views = tab.getAttributeViews(attribute.getName());
				if (views != null) {
					setAttribute(linker, attribute, views);
				}
			}
		}
	}
	
	private static void showRelationshipTab(BeanShellLinker linker, Relationship relationship, Tab tab) {
		tab.clearViews();
		for (RelationshipAttribute attribute : relationship.getAttributes()) {
			if (tab.hasView(attribute.getName())) {
				List<View> views = tab.getAttributeViews(attribute.getName());
				if (views != null) {
					setAttribute(linker, attribute, views);
				}
			}
		}
	}

	private static void setAttribute(BeanShellLinker linker, Attribute attribute, List<View> views) {
		for (View v : views) {
			if (v instanceof ICustomView) {
				ICustomView customView = (ICustomView) v;
				if (customView.getAttributeName().equals(attribute.getName())) {
					if (v instanceof FileListGroup) {
						// add full path
						FileListGroup fileList = (FileListGroup) v;
						fileList.addFile(linker.getAttachedFilePath(attribute.getValue(customView.getAttributeType())));
					} else if (v instanceof CameraPictureGallery) {
						CameraPictureGallery cameraGallery = (CameraPictureGallery) v;
						// add full path
						cameraGallery.addPicture(linker.getAttachedFilePath(attribute.getValue(customView.getAttributeType())));
					} else if (v instanceof VideoGallery) {
						VideoGallery videoGallery = (VideoGallery) v;
						// add full path
						videoGallery.addVideo(linker.getAttachedFilePath(attribute.getValue(customView.getAttributeType())));
					} else {
						linker.setFieldValue(customView.getRef(), attribute.getValue(customView.getAttributeType()));
						linker.setFieldCertainty(customView.getRef(), attribute.getCertainty());
						linker.setFieldAnnotation(customView.getRef(), attribute.getAnnotation(customView.getAttributeType()));
						linker.appendFieldDirty(customView.getRef(), attribute.isDirty(), attribute.getDirtyReason());
					}
					customView.save();
					break;
				}
			}
		}
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

}
