package au.org.intersect.faims.android.beanshell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import android.view.View;
import au.org.intersect.faims.android.R;
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
import au.org.intersect.faims.android.ui.view.VideoGallery;

public class AttributeHelper {
	
	public static class AttributeViewGroup {
		
		public String name;
		public List<ICustomView> views;
		private List<String> viewAnnotations;
		private List<String> viewCertainties;
		
		public AttributeViewGroup(String name) {
			this.name = name;
			this.views = new ArrayList<ICustomView>();
			this.viewAnnotations = new ArrayList<String>();
			this.viewCertainties = new ArrayList<String>();
		}
		
		public void addView(ICustomView view) {
			views.add(view);
		}
		
		public ArrayList<EntityAttribute> getEntityAttributes(BeanShellLinker linker) {
			HashMap<String, ArrayList<String>> valuesByType = getValuesByType(linker);
			
			ArrayList<String> freetexts = valuesByType.get(Attribute.FREETEXT);
			ArrayList<String> measures = valuesByType.get(Attribute.MEASURE);
			ArrayList<String> vocabs = valuesByType.get(Attribute.VOCAB);
			ArrayList<String> certainties = valuesByType.get(Attribute.CERTAINTY);
			
			ArrayList<EntityAttribute> attributes = new ArrayList<EntityAttribute>();
			int size = getMaxSize(freetexts, measures, vocabs, certainties);
			if (size == 0) {
				attributes.add(new EntityAttribute(name, null, null, null, null, true));
			} else {
				for (int i = 0; i < size; i++) {
					// view annotation and view certainty will override values
					String freetext = getValueAt(freetexts, i) == null ? getValueAt(viewAnnotations, i) : getValueAt(freetexts, i);
					String measure = getValueAt(measures, i);
					String vocab = getValueAt(vocabs, i);
					String certainty = getValueAt(certainties, i) == null ? getValueAt(viewCertainties, i)  : getValueAt(certainties, i);					
					attributes.add(new EntityAttribute(name, freetext, measure, vocab, certainty));
				}
			}
			
			return attributes;
		}
		
		public ArrayList<RelationshipAttribute> getRelationshipAttributes(BeanShellLinker linker) {
			HashMap<String, ArrayList<String>> valuesByType = getValuesByType(linker);
			
			ArrayList<String> freetexts = valuesByType.get(Attribute.FREETEXT);
			ArrayList<String> vocabs = valuesByType.get(Attribute.VOCAB);
			ArrayList<String> certainties = valuesByType.get(Attribute.CERTAINTY);
			
			ArrayList<RelationshipAttribute> attributes = new ArrayList<RelationshipAttribute>();
			int size = getMaxSize(freetexts, null, vocabs, certainties);
			if (size == 0) {
				attributes.add(new RelationshipAttribute(name, null, null, null, true));
			} else {
				for (int i = 0; i < size; i++) {
					// view annotation and view certainty will override values
					String freetext = getValueAt(freetexts, i) == null ? getValueAt(viewAnnotations, i) : getValueAt(freetexts, i);
					String vocab = getValueAt(vocabs, i);
					String certainty = getValueAt(certainties, i) == null ? getValueAt(viewCertainties, i)  : getValueAt(certainties, i);
					attributes.add(new RelationshipAttribute(name, freetext, vocab, certainty));
				}
			}
			
			return attributes;
		}
		
		private HashMap<String, ArrayList<String>> getValuesByType(BeanShellLinker linker) {
			HashMap<String, ArrayList<String>> valuesByType = new HashMap<String, ArrayList<String>>();
			for (int i = 0; i < views.size(); i++) {
				ICustomView customView = views.get(i);
				
				if (customView.getAnnotationEnabled()) {
					viewAnnotations.add(customView.getAnnotation());
				}
				
				if (customView.getCertaintyEnabled()) {
					viewCertainties.add(String.valueOf(customView.getCertainty()));
				}
				
				String type = customView.getAttributeType();
				if (valuesByType.get(type) == null) {
					valuesByType.put(type, new ArrayList<String>());
				}
				valuesByType.get(type).addAll(getViewValues(linker, customView));
			}
			return valuesByType;
		}
		
		@SuppressWarnings("unchecked")
		private ArrayList<String> getViewValues(BeanShellLinker linker, ICustomView customView) {
			ArrayList<String> values = new ArrayList<String>();
			if (customView instanceof ICustomFileView) {
				ICustomFileView fileView = (ICustomFileView) customView;
				List<NameValuePair> newPairs = new ArrayList<NameValuePair>();
				List<NameValuePair> pairs = (List<NameValuePair>) customView.getValues();
				if (pairs != null) {
					for (NameValuePair pair : pairs) {
						// strip out full path
						String value = null;
						
						// attach new files
						boolean sync = fileView.getSync();
						String attachment = pair.getName();
						if (hasAttachment(linker, attachment, sync)) {
							value = linker.stripAttachedFilePath(attachment);
						} else {
							value = linker.attachFile(attachment, sync, null, null);
						}
						
						values.add(value);
						newPairs.add(new NameValuePair(attachment, linker.getModule().getDirectoryPath(value).getPath()));
					}
					// reload new paths into file views
					fileView.setReloadPairs(newPairs);
				}
			} else if (customView instanceof CustomCheckBoxGroup) {
				List<NameValuePair> pairs = (List<NameValuePair>) customView.getValues();
				if (pairs != null) {
					for (NameValuePair pair : pairs) {
						values.add(pair.getName());
					}
				}
			} else {
				values.add(customView.getValue());
			}
			
			return  values;
		}
		
		private int getMaxSize(List<?> list1, List<?> list2, List<?> list3, List<?> list4) {
			int max = 0;
			if (list1 != null) {
				max = Math.max(list1.size(), max);
			}
			if (list2 != null) {
				max = Math.max(list2.size(), max);
			}
			if (list3 != null) {
				max = Math.max(list3.size(), max);
			}
			if (list4 != null) {
				max = Math.max(list4.size(), max);
			}
			return max;
		}
		
		private String getValueAt(List<String> list, int index) {
			if (list != null && list.size() > index) {
				return list.get(index);
			}
			return null;
		}

		public boolean hasChanges(BeanShellLinker linker,
				Collection<? extends Attribute> cachedAttributes) {
			for (ICustomView customView : views) {
				if (customView instanceof ICustomFileView) {
					if (((ICustomFileView) customView).hasFileAttributeChanges(linker.getModule(), cachedAttributes)) {
						return true;
					}
				} else if (customView.hasAttributeChanges(cachedAttributes)) {
					return true;
				}
			}
			return false;
		}

		public void setEntityAttributes(BeanShellLinker linker, List<EntityAttribute> attributes) {
			if (attributes == null || attributes.isEmpty()) return;
			HashMap<String, ArrayList<ICustomView>> viewsByType = getViewsByType();
			
			ArrayList<ICustomView> freetexts = viewsByType.get(Attribute.FREETEXT);
			ArrayList<ICustomView> measures = viewsByType.get(Attribute.MEASURE);
			ArrayList<ICustomView> vocabs = viewsByType.get(Attribute.VOCAB);
			ArrayList<ICustomView> certainties = viewsByType.get(Attribute.CERTAINTY);
			
			for (int i = 0; i < attributes.size(); i++) {
				EntityAttribute attribute = attributes.get(i);
				boolean hasTextView = hasViewAt(freetexts, i);
				boolean hasCertaintyView = hasViewAt(certainties, i);
				setViewAt(linker, freetexts, attribute, i, false, hasCertaintyView);
				setViewAt(linker, measures, attribute, i, hasTextView, hasCertaintyView);
				setViewAt(linker, vocabs, attribute, i, hasTextView, hasCertaintyView);
				setViewAt(linker, certainties, attribute, i, hasTextView, false);
			}
		}

		public void setRelationshipAttributes(BeanShellLinker linker,
				List<RelationshipAttribute> attributes) {
			if (attributes == null || attributes.isEmpty()) return;
			HashMap<String, ArrayList<ICustomView>> viewsByType = getViewsByType();
			
			ArrayList<ICustomView> freetexts = viewsByType.get(Attribute.FREETEXT);
			ArrayList<ICustomView> vocabs = viewsByType.get(Attribute.VOCAB);
			ArrayList<ICustomView> certainties = viewsByType.get(Attribute.CERTAINTY);
			
			for (int i = 0; i < attributes.size(); i++) {
				RelationshipAttribute attribute = attributes.get(i);
				boolean hasTextView = hasViewAt(freetexts, i);
				boolean hasCertaintyView = hasViewAt(certainties, i);
				setValueAt(linker, freetexts, attribute, i, false, hasCertaintyView);
				setValueAt(linker, vocabs, attribute, i, hasTextView, hasCertaintyView);
				setValueAt(linker, certainties, attribute, i, hasTextView, false);
			}
		}
		
		private HashMap<String, ArrayList<ICustomView>> getViewsByType() {
			HashMap<String, ArrayList<ICustomView>> viewsByType = new HashMap<String, ArrayList<ICustomView>>();
			for (int i = 0; i < views.size(); i++) {
				ICustomView customView = views.get(i);
				String type = customView.getAttributeType();
				if (viewsByType.get(type) == null) {
					viewsByType.put(type, new ArrayList<ICustomView>());
				}
				viewsByType.get(type).add(customView);
			}
			return viewsByType;
		}
		
		private boolean hasViewAt(List<ICustomView> views, int i) {
			return views != null && views.size() > i;
		}
		
		private void setViewAt(BeanShellLinker linker, List<ICustomView> views, EntityAttribute attribute, int i, boolean ignoreAnnotation, boolean ignoreCertainty){
			if (views != null) {
				if (views.size() > 0) {
					ICustomView firstView = views.get(0);
					// if the first view is a checkbox view or file view then set attribute value to the first view
					// else set attribute value to the view by index
					if (firstView instanceof CustomCheckBoxGroup || 
						firstView instanceof ICustomFileView) {
						setAttribute(linker, attribute, firstView, ignoreAnnotation, ignoreCertainty);
					} else if (views.size() > i) {
						ICustomView view = views.get(i);
						setAttribute(linker, attribute, view, ignoreAnnotation, ignoreCertainty);
					}
				}
			}	
		}
		
		private void setValueAt(BeanShellLinker linker, List<ICustomView> views, RelationshipAttribute attribute, int i, boolean ignoreAnnotation, boolean ignoreCertainty){
			if (views != null && views.size() > i) {
				ICustomView view = views.get(i);
				setAttribute(linker, attribute, view, ignoreAnnotation, ignoreCertainty);
			}
		}
		
		private static void setAttribute(BeanShellLinker linker, Attribute attribute, ICustomView customView, boolean ignoreAnnotation, boolean ignoreCertainty) {
			if (customView instanceof FileListGroup) {
				// add full path
				FileListGroup fileList = (FileListGroup) customView;
				fileList.addFile(linker.getAttachedFilePath(attribute.getValue(customView.getAttributeType())));
			} else if (customView instanceof CameraPictureGallery) {
				CameraPictureGallery cameraGallery = (CameraPictureGallery) customView;
				// add full path
				cameraGallery.addPicture(linker.getAttachedFilePath(attribute.getValue(customView.getAttributeType())));
			} else if (customView instanceof VideoGallery) {
				VideoGallery videoGallery = (VideoGallery) customView;
				// add full path
				videoGallery.addVideo(linker.getAttachedFilePath(attribute.getValue(customView.getAttributeType())));
			} else {
				linker.setFieldValue(customView.getRef(), attribute.getValue(customView.getAttributeType()));
				if (!ignoreCertainty && customView.getCertaintyEnabled()) {
					linker.setFieldCertainty(customView.getRef(), attribute.getCertainty());
				}
				if (!ignoreAnnotation && customView.getAnnotationEnabled()) {
					linker.setFieldAnnotation(customView.getRef(), attribute.getAnnotation(customView.getAttributeType()));
				}
				linker.appendFieldDirty(customView.getRef(), attribute.isDirty(), attribute.getDirtyReason());
			}
			customView.save();
		}
		
		private boolean hasAttachment(BeanShellLinker linker, String filename, boolean sync) {
			if (filename == null) return false;
			// strip the module path from filename if it exists
			String strippedFilename = linker.stripAttachedFilePath(filename);
			// get directory to attach to
			String directory;
			if (sync) {
				directory = linker.getActivity().getResources().getString(R.string.app_dir);
			} else {
				directory = linker.getActivity().getResources().getString(R.string.server_dir);
			}
			// check if file exists and is in correct directory
			return linker.getModule().getDirectoryPath(strippedFilename).exists() && strippedFilename.contains(directory);
		}

	}

	public static ArrayList<EntityAttribute> getEntityAttributes(BeanShellLinker linker, Tab tab, ArchEntity entity) {
		ArrayList<EntityAttribute> entityAttributes = new ArrayList<EntityAttribute>();
		List<View> views = tab.getAttributeViews();
		if (views != null) {
			Collection<AttributeViewGroup> viewGroups = getChangedAttributeGroups(linker, views, entity != null ? entity.getAttributes() : null);
			if (viewGroups != null) {
				for (AttributeViewGroup group : viewGroups) {
					entityAttributes.addAll(group.getEntityAttributes(linker));
				}
			}
		}
		return entityAttributes;
	}
	
	public static ArrayList<RelationshipAttribute> getRelationshipAttributes(BeanShellLinker linker, Tab tab, Relationship relationship) {
		ArrayList<RelationshipAttribute> relationshipAttributes = new ArrayList<RelationshipAttribute>();
		List<View> views = tab.getAttributeViews();
		if (views != null) {
			Collection<AttributeViewGroup> viewGroups = getChangedAttributeGroups(linker, views, relationship != null ? relationship.getAttributes() : null);
			if (viewGroups != null) {
				for (AttributeViewGroup group : viewGroups) {
					relationshipAttributes.addAll(group.getRelationshipAttributes(linker));
				}
			}
		}
		return relationshipAttributes;
	}
	
	private static Collection<AttributeViewGroup> getChangedAttributeGroups(BeanShellLinker linker, List<View> views, Collection<? extends Attribute> cachedAttributes) {
		ArrayList<AttributeViewGroup> viewGroups = new ArrayList<AttributeViewGroup>();
		for (AttributeViewGroup group : getAttributeGroups(linker, views)) {
			if (cachedAttributes == null || group.hasChanges(linker, cachedAttributes)) {
				viewGroups.add(group);
			}
		}
		return viewGroups;
	} 
	
	private static Collection<AttributeViewGroup> getAttributeGroups(BeanShellLinker linker, List<View> views) {
		HashMap<String, AttributeViewGroup> viewGroupMap = new HashMap<String, AttributeViewGroup>();
		for (View v : views) {
			if (v instanceof ICustomView) {
				ICustomView customView = (ICustomView) v;
				String name = customView.getAttributeName();
				if (viewGroupMap.get(name) == null) {
					viewGroupMap.put(name, new AttributeViewGroup(name));
				}
				viewGroupMap.get(name).addView(customView);
			}
		}
		ArrayList<AttributeViewGroup> viewGroups = new ArrayList<AttributeViewGroup>();
		for (AttributeViewGroup group : viewGroupMap.values()) {
			viewGroups.add(group);
		}
		return viewGroups;
	} 
	
	public static void showArchEntityTab(BeanShellLinker linker,
			ArchEntity archEntity, Tab tab) {
		List<EntityAttribute> attributes = (List<EntityAttribute>) archEntity.getAttributes();
		HashMap<String, List<EntityAttribute>> attributesByName = getEntityAttributesByName(attributes);
		List<View> views = tab.getAttributeViews();
		if (views != null) {
			Collection<AttributeViewGroup> viewGroups = getAttributeGroups(linker, views);
			if (viewGroups != null) {
				for (AttributeViewGroup group : viewGroups) {
					group.setEntityAttributes(linker, attributesByName.get(group.name));
				}
			}
		}
	}

	public static void showRelationshipTab(BeanShellLinker linker,
			Relationship relationship, Tab tab) {
		List<RelationshipAttribute> attributes = (List<RelationshipAttribute>) relationship.getAttributes();
		HashMap<String, List<RelationshipAttribute>> attributesByName = getRelationshipAttributesByName(attributes);
		List<View> views = tab.getAttributeViews();
		if (views != null) {
			Collection<AttributeViewGroup> viewGroups = getAttributeGroups(linker, views);
			if (viewGroups != null) {
				for (AttributeViewGroup group : viewGroups) {
					group.setRelationshipAttributes(linker, attributesByName.get(group.name));
				}
			}
		}
	}
	
	private static HashMap<String, List<EntityAttribute>> getEntityAttributesByName(List<EntityAttribute> attributes) {
		HashMap<String, List<EntityAttribute>> attributesByName = new HashMap<String, List<EntityAttribute>>();
		for (EntityAttribute attribute : attributes) {
			String name = attribute.getName();
			List<EntityAttribute> list = attributesByName.get(name);
			if (list == null) {
				list = new ArrayList<EntityAttribute>();
				attributesByName.put(name, list);
			}
			list.add(attribute);
		}
		return attributesByName;
	}
	
	private static HashMap<String, List<RelationshipAttribute>> getRelationshipAttributesByName(List<RelationshipAttribute> attributes) {
		HashMap<String, List<RelationshipAttribute>> attributesByName = new HashMap<String, List<RelationshipAttribute>>();
		for (RelationshipAttribute attribute : attributes) {
			String name = attribute.getName();
			List<RelationshipAttribute> list = attributesByName.get(name);
			if (list == null) {
				list = new ArrayList<RelationshipAttribute>();
				attributesByName.put(name, list);
			}
			list.add(attribute);
		}
		return attributesByName;
	}
}
