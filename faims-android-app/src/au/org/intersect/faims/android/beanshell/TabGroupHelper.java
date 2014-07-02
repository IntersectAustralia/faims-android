package au.org.intersect.faims.android.beanshell;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.view.View;
import au.org.intersect.faims.android.data.ArchEntity;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.EntityAttribute;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.data.Relationship;
import au.org.intersect.faims.android.data.RelationshipAttribute;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.ui.view.CameraPictureGallery;
import au.org.intersect.faims.android.ui.view.CustomCheckBoxGroup;
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
			TabGroup tabGroup = linker.getTabGroup(ref);
			saveTabGroup(linker, tabGroup, uuid, geometry, attributes, callback, newRecord);
			setTabGroupSaved(tabGroup);
			autoSaveManager.reportSaved();
			if (callback != null) {
				linker.getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						try {
							callback.onSave(uuid, newRecord);
						} catch (Exception e) {
							linker.reportError(e, "Error executing save tab group onsave callback");
						}
					}
					
				});
			}
		} catch (Exception e) {
			final String message = "Error saving tab group " + ref;
			FLog.e(message, e);
			autoSaveManager.notifyError();
			if (callback != null) {
				linker.getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						try {
							callback.onError(message);
						} catch (Exception ce) {
							linker.reportError(ce, "Error executing save tab group onerror callback");
						}
					}
					
				});
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
		final AutoSaveManager autoSaveManager = linker.getAutoSaveManager();
		try {
			TabGroup tabGroup = linker.getTabGroupFromTabLabel(ref);
			Tab tab = linker.getTab(ref);
			saveTab(linker, tabGroup, tab, uuid, geometry, attributes, callback, newRecord);
			setTabSaved(tab);
			autoSaveManager.reportSaved();
			if (callback != null) {
				linker.getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						try {
							callback.onSave(uuid, newRecord);
						} catch (Exception e) {
							linker.reportError(e, "Error executing save tab callback");
						}
					}
					
				});
			}
		} catch (Exception e) {
			final String message = "Error saving tab " + ref;
			FLog.e(message, e);
			autoSaveManager.notifyError();
			if (callback != null) {
				linker.getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						try {
							callback.onError(message);
						} catch (Exception ce) {
							linker.reportError(ce, "Error executing save tab callback");
						}
					}
					
				});
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void saveTabGroup(BeanShellLinker linker, TabGroup tabGroup, String uuid, List<Geometry> geometry, 
			List<? extends Attribute> attributes, SaveCallback callback, boolean newRecord) throws Exception {
		synchronized(TabGroupHelper.class) {
			if (tabGroup.getArchEntType() != null) {
				List<EntityAttribute> entityAttributes = new ArrayList<EntityAttribute>();
				if (attributes != null) {
					entityAttributes.addAll((List<EntityAttribute>) attributes);
				}
				entityAttributes.addAll(getEntityAttributesFromTabGroup(linker, tabGroup));			
				if (geometry != null || !entityAttributes.isEmpty()) {
					DatabaseHelper.saveArchEnt(linker, uuid, tabGroup.getArchEntType(), geometry, entityAttributes, callback, newRecord, true);
				}
			} else if (tabGroup.getRelType() != null) {
				List<RelationshipAttribute> relationshipAttributes = new ArrayList<RelationshipAttribute>();
				if (attributes != null) {
					relationshipAttributes.addAll((List<RelationshipAttribute>) attributes);
				}	
				relationshipAttributes.addAll(getRelationshipAttributesFromTabGroup(linker, tabGroup));
				if (!relationshipAttributes.isEmpty()) {
					DatabaseHelper.saveRel(linker, uuid, tabGroup.getRelType(), geometry, relationshipAttributes, callback, newRecord, true);
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
			if (tabGroup.getArchEntType() != null) {
				List<EntityAttribute> entityAttributes = new ArrayList<EntityAttribute>();
				if (attributes != null) {
					entityAttributes.addAll((List<EntityAttribute>) attributes);
				}
				entityAttributes.addAll(TabGroupHelper.getEntityAttributesFromTab(linker, tab));
				if (geometry != null || !entityAttributes.isEmpty()) {
					DatabaseHelper.saveArchEnt(linker, uuid, tabGroup.getArchEntType(), geometry, entityAttributes, callback, newRecord, true);
				}
			} else if (tabGroup.getRelType() != null) {			
				List<RelationshipAttribute> relationshipAttributes = new ArrayList<RelationshipAttribute>();
				if (attributes != null) {
					relationshipAttributes.addAll((List<RelationshipAttribute>) attributes);
				}		
				relationshipAttributes.addAll(TabGroupHelper.getRelationshipAttributesFromTab(linker, tab));	
				if (geometry != null || !relationshipAttributes.isEmpty()) {
					DatabaseHelper.saveRel(linker, uuid, tabGroup.getRelType(), geometry, relationshipAttributes, callback, newRecord, true);
				}
			} else {
				throw new Exception("no type specified for tabgroup");
			}
		}
	}
	
	private static void setTabGroupSaved(TabGroup tabGroup) {
		List<Tab> tabs = tabGroup.getTabs();
		for (Tab tab : tabs) {
			setTabSaved(tab);
		}
	}
	
	private static void setTabSaved(Tab tab) {
		List<View> views = tab.getAttributeViews();
		for (View view : views) {
			if (view instanceof ICustomView) {
				ICustomView customView = (ICustomView) view;
				customView.save();
			}
		}
	}
	
	private static List<EntityAttribute> getEntityAttributesFromTabGroup(BeanShellLinker linker, TabGroup tabGroup) {
		List<EntityAttribute> attributes = new ArrayList<EntityAttribute>();
		for (Tab tab : tabGroup.getTabs()) {
			attributes.addAll(getEntityAttributesFromTab(linker, tab));
		}
		return attributes;
	}
	
	@SuppressWarnings("unchecked")
	private static List<EntityAttribute> getEntityAttributesFromTab(BeanShellLinker linker, Tab tab) {
		List<EntityAttribute> attributes = new ArrayList<EntityAttribute>();
		
		List<View> views = tab.getAttributeViews();
		if (views != null) {
			for (View v : views) {
				if (v instanceof ICustomView) {
					ICustomView customView = (ICustomView) v;
					if (customView.hasChanges()) {
						String annotation = customView.getAnnotationEnabled() ? customView.getAnnotation() : null;
						String certainty = customView.getCertaintyEnabled() ? String.valueOf(customView.getCertainty()) : null;
						if (customView instanceof ICustomFileView) {
							List<NameValuePair> pairs = (List<NameValuePair>) customView.getValues();
							if (pairs == null || pairs.isEmpty()) {
								attributes.add(new EntityAttribute(customView.getAttributeName(), null, null, null, null, true));
							} else {
								for (NameValuePair pair : pairs) {
									// strip out full path
									String value = null;
									
									// attach new files
									if (!pair.getName().contains(linker.getModule().getDirectoryPath("files").getPath())) {
										value = linker.attachFile(pair.getName(), ((ICustomFileView) customView).getSync(), null, null);
									} else {
										value = linker.stripAttachedFilePath(pair.getName());
									}
									
									if (Attribute.MEASURE.equals(customView.getAttributeType())) {
										attributes.add(new EntityAttribute(customView.getAttributeName(), annotation, value, null, certainty));
									} else if (Attribute.VOCAB.equals(customView.getAttributeType())) {
										attributes.add(new EntityAttribute(customView.getAttributeName(), annotation, null, value, certainty));
									} else {
										attributes.add(new EntityAttribute(customView.getAttributeName(), value, null, null, certainty));
									}
								}
							}
						} else if (v instanceof CustomCheckBoxGroup) {
							CustomCheckBoxGroup checkboxGroup = (CustomCheckBoxGroup) v;
							List<NameValuePair> pairs = (List<NameValuePair>) checkboxGroup.getValues();
							if (pairs == null || pairs.isEmpty()) {
								attributes.add(new EntityAttribute(customView.getAttributeName(), null, null, null, null, true));
							} else {
								for (NameValuePair pair : pairs) {
									if (Attribute.MEASURE.equals(customView.getAttributeType())) {
										attributes.add(new EntityAttribute(customView.getAttributeName(), annotation, pair.getName(), null, certainty));
									} else if (Attribute.VOCAB.equals(customView.getAttributeType())) {
										attributes.add(new EntityAttribute(customView.getAttributeName(), annotation, null, pair.getName(), certainty));
									} else {
										attributes.add(new EntityAttribute(customView.getAttributeName(), pair.getName(), null, null, certainty));
									}
								}
							}
						} else {
							if (Attribute.MEASURE.equals(customView.getAttributeType())) {
								attributes.add(new EntityAttribute(customView.getAttributeName(), annotation, customView.getValue(), null, certainty));
							} else if (Attribute.VOCAB.equals(customView.getAttributeType())) {
								attributes.add(new EntityAttribute(customView.getAttributeName(), annotation, null, customView.getValue(), certainty));
							} else {
								attributes.add(new EntityAttribute(customView.getAttributeName(), customView.getValue(), null, null, certainty));
							}
						}
					}
				}
			}
		} 
		
		return attributes;
	}

	private static List<RelationshipAttribute> getRelationshipAttributesFromTabGroup(BeanShellLinker linker, TabGroup tabGroup) {
		List<RelationshipAttribute> attributes = new ArrayList<RelationshipAttribute>();
		for (Tab tab : tabGroup.getTabs()) {
			attributes.addAll(getRelationshipAttributesFromTab(linker, tab));
		}
		return attributes;
	}
	
	@SuppressWarnings("unchecked")
	private static List<RelationshipAttribute> getRelationshipAttributesFromTab(BeanShellLinker linker, Tab tab) {
		List<RelationshipAttribute> attributes = new ArrayList<RelationshipAttribute>();
		
		List<View> views = tab.getAttributeViews();
		if (views != null) {
			for (View v : views) {
				if (v instanceof ICustomView) {
					ICustomView customView = (ICustomView) v;
					if (customView.hasChanges()) {
						String annotation = customView.getAnnotationEnabled() ? customView.getAnnotation() : null;
						String certainty = customView.getCertaintyEnabled() ? String.valueOf(customView.getCertainty()) : null;
						if (customView instanceof ICustomFileView) {
							List<NameValuePair> pairs = (List<NameValuePair>) customView.getValues();
							if (pairs == null || pairs.isEmpty()) {
								attributes.add(new EntityAttribute(customView.getAttributeName(), null, null, null, null, true));
							} else {
								for (NameValuePair pair : pairs) {
									// strip out full path
									String value = null;
									
									// attach new files
									if (!pair.getName().contains(linker.getModule().getDirectoryPath("files").getPath())) {
										value = linker.attachFile(pair.getName(), ((ICustomFileView) customView).getSync(), null, null);
									} else {
										value = linker.stripAttachedFilePath(pair.getName());
									}
									
									if (Attribute.VOCAB.equals(customView.getAttributeType())) {
										attributes.add(new RelationshipAttribute(customView.getAttributeName(), annotation, value, certainty));
									} else {
										attributes.add(new RelationshipAttribute(customView.getAttributeName(), value, null, certainty));
									}
								}
							}
						} else if (v instanceof CustomCheckBoxGroup) {
							CustomCheckBoxGroup checkboxGroup = (CustomCheckBoxGroup) v;
							List<NameValuePair> pairs = (List<NameValuePair>) checkboxGroup.getValues();
							if (pairs == null || pairs.isEmpty()) {
								attributes.add(new RelationshipAttribute(customView.getAttributeName(), null, null, null, true));
							} else {
								for (NameValuePair pair : pairs) {
									if (Attribute.VOCAB.equals(customView.getAttributeType())) {
										attributes.add(new RelationshipAttribute(customView.getAttributeName(), annotation, pair.getName(), certainty));
									} else {
										attributes.add(new RelationshipAttribute(customView.getAttributeName(), pair.getName(), null, certainty));
									}
								}
							}
						} else {
							if (Attribute.VOCAB.equals(customView.getAttributeType())) {
								attributes.add(new RelationshipAttribute(customView.getAttributeName(), annotation, customView.getValue(), certainty));
							} else {
								attributes.add(new RelationshipAttribute(customView.getAttributeName(), customView.getValue(), null, certainty));
							}
						}
					}
				}
			}
		} 
		
		return attributes;
	}
	
	protected static void showArchEntityTabGroup(BeanShellLinker linker, String uuid, TabGroup tabGroup) throws Exception {
		Object archEntityObj = linker.fetchArchEnt(uuid);
		if (archEntityObj instanceof ArchEntity) {
			for (Tab tab : tabGroup.getTabs()) {
				showArchEntityTab(linker, (ArchEntity) archEntityObj, tab);
			}
		} else {
			throw new Exception("cannot find entity");
		}
	}

	protected static void showRelationshipTabGroup(BeanShellLinker linker, String uuid, TabGroup tabGroup) throws Exception {
		Object relationshipObj = linker.fetchRel(uuid);
		if (relationshipObj instanceof Relationship) {
			for (Tab tab : tabGroup.getTabs()) {
				showRelationshipTab(linker, (Relationship) relationshipObj, tab);
			}
		} else {
			throw new Exception("cannot find relationship");
		}
	}

	protected static void showArchEntityTab(BeanShellLinker linker, String uuid, Tab tab) throws Exception {
		Object archEntityObj = linker.fetchArchEnt(uuid);
		if (archEntityObj instanceof ArchEntity) {
			showArchEntityTab(linker, (ArchEntity) archEntityObj, tab);
		} else {
			throw new Exception("cannot find entity");
		}
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
	
	protected static void showRelationshipTab(BeanShellLinker linker, String uuid, Tab tab) throws Exception {
		Object relationshipObj = linker.fetchRel(uuid);
		if (relationshipObj instanceof Relationship) {
			showRelationshipTab(linker, (Relationship) relationshipObj, tab);
		} else {
			throw new Exception("cannot find relationship");
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

}
