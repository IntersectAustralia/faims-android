package au.org.intersect.faims.android.beanshell;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import au.org.intersect.faims.android.data.ArchEntity;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.EntityAttribute;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.data.Relationship;
import au.org.intersect.faims.android.data.RelationshipAttribute;
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
	
	@SuppressWarnings("unchecked")
	public static void saveTabGroup(BeanShellLinker linker, TabGroup tabGroup, String uuid, List<Geometry> geometry, final List<? extends Attribute> attributes, boolean newRecord) throws Exception {
		synchronized(TabGroupHelper.class) {
			if (tabGroup.getArchEntType() != null) {
				List<EntityAttribute> entityAttributes = new ArrayList<EntityAttribute>();
				if (attributes != null) {
					entityAttributes.addAll((List<EntityAttribute>) attributes);
				}
				entityAttributes.addAll(getEntityAttributesFromTabGroup(linker, tabGroup));			
				String entityId = linker.saveArchEnt(uuid, tabGroup.getArchEntType(), geometry, entityAttributes, newRecord);
				if (entityId != null) {
					linker.getInterpreter().set("_saved_record_id", entityId);
					setTabGroupSaved(tabGroup);
				} else {
					throw new Exception("error trying to save entity");
				}
			} else if (tabGroup.getRelType() != null) {
				List<RelationshipAttribute> relationshipAttributes = new ArrayList<RelationshipAttribute>();
				if (attributes != null) {
					relationshipAttributes.addAll((List<RelationshipAttribute>) attributes);
				}	
				relationshipAttributes.addAll(getRelationshipAttributesFromTabGroup(linker, tabGroup));
				String relationshipId = linker.saveRel(uuid, tabGroup.getRelType(), geometry, relationshipAttributes, newRecord);
				if (relationshipId != null) {
					linker.getInterpreter().set("_saved_record_id", relationshipId);
					setTabGroupSaved(tabGroup);
				} else {
					throw new Exception("error trying to save relationship");
				}
			} else {
				throw new Exception("no type specified for tabgroup");
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void saveTab(BeanShellLinker linker, TabGroup tabGroup, Tab tab, String uuid, List<Geometry> geometry, List<? extends Attribute> attributes, boolean newRecord) throws Exception {
		synchronized(TabGroupHelper.class) {
			if (tabGroup.getArchEntType() != null) {
				List<EntityAttribute> entityAttributes = new ArrayList<EntityAttribute>();
				if (attributes != null) {
					entityAttributes.addAll((List<EntityAttribute>) attributes);
				}
				entityAttributes.addAll(TabGroupHelper.getEntityAttributesFromTab(linker, tab));
				String entityId = linker.saveArchEnt(uuid, tabGroup.getArchEntType(), geometry, entityAttributes, newRecord);
				if (entityId != null) {
					linker.getInterpreter().set("_saved_record_id", entityId);
					setTabSaved(tab);
				} else {
					throw new Exception("error trying to save entity");
				}
			} else if (tabGroup.getRelType() != null) {			
				List<RelationshipAttribute> relationshipAttributes = new ArrayList<RelationshipAttribute>();
				if (attributes != null) {
					relationshipAttributes.addAll((List<RelationshipAttribute>) attributes);
				}		
				relationshipAttributes.addAll(TabGroupHelper.getRelationshipAttributesFromTab(linker, tab));			
				String relationshipId = linker.saveRel(uuid, tabGroup.getRelType(), geometry, relationshipAttributes, newRecord);		
				if (relationshipId != null) {
					linker.getInterpreter().set("_saved_record_id", relationshipId);
					setTabSaved(tab);
				} else {
					throw new Exception("error trying to save relationship");
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
	
	public static List<EntityAttribute> getEntityAttributesFromTabGroup(BeanShellLinker linker, TabGroup tabGroup) {
		List<EntityAttribute> attributes = new ArrayList<EntityAttribute>();
		for (Tab tab : tabGroup.getTabs()) {
			attributes.addAll(getEntityAttributesFromTab(linker, tab));
		}
		return attributes;
	}
	
	@SuppressWarnings("unchecked")
	public static List<EntityAttribute> getEntityAttributesFromTab(BeanShellLinker linker, Tab tab) {
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

	public static List<RelationshipAttribute> getRelationshipAttributesFromTabGroup(BeanShellLinker linker, TabGroup tabGroup) {
		List<RelationshipAttribute> attributes = new ArrayList<RelationshipAttribute>();
		for (Tab tab : tabGroup.getTabs()) {
			attributes.addAll(getRelationshipAttributesFromTab(linker, tab));
		}
		return attributes;
	}
	
	@SuppressWarnings("unchecked")
	public static List<RelationshipAttribute> getRelationshipAttributesFromTab(BeanShellLinker linker, Tab tab) {
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
	
	public static void showArchEntityTabGroup(BeanShellLinker linker, String uuid, TabGroup tabGroup) throws Exception {
		Object archEntityObj = linker.fetchArchEnt(uuid);
		if (archEntityObj instanceof ArchEntity) {
			for (Tab tab : tabGroup.getTabs()) {
				showArchEntityTab(linker, (ArchEntity) archEntityObj, tab);
			}
		} else {
			throw new Exception("cannot find entity");
		}
	}

	public static void showRelationshipTabGroup(BeanShellLinker linker, String uuid, TabGroup tabGroup) throws Exception {
		Object relationshipObj = linker.fetchRel(uuid);
		if (relationshipObj instanceof Relationship) {
			for (Tab tab : tabGroup.getTabs()) {
				showRelationshipTab(linker, (Relationship) relationshipObj, tab);
			}
		} else {
			throw new Exception("cannot find relationship");
		}
	}

	public static void showArchEntityTab(BeanShellLinker linker, String uuid, Tab tab) throws Exception {
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
	
	public static void showRelationshipTab(BeanShellLinker linker, String uuid, Tab tab) throws Exception {
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
