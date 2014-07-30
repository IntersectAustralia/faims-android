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
import au.org.intersect.faims.android.ui.view.CustomCheckBoxGroup;
import au.org.intersect.faims.android.ui.view.ICustomFileView;
import au.org.intersect.faims.android.ui.view.ICustomView;
import au.org.intersect.faims.android.ui.view.Tab;

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
					String freetext = getValueAt(viewAnnotations, i) == null ? getValueAt(freetexts, i) : getValueAt(viewAnnotations, i);
					String measure = getValueAt(measures, i);
					String vocab = getValueAt(vocabs, i);
					String certainty = getValueAt(viewCertainties, i) == null ? getValueAt(certainties, i) : getValueAt(viewCertainties, i);
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
					String freetext = getValueAt(viewAnnotations, i) == null ? getValueAt(freetexts, i) : getValueAt(viewAnnotations, i);
					String vocab = getValueAt(vocabs, i);
					String certainty = getValueAt(viewCertainties, i) == null ? getValueAt(certainties, i) : getValueAt(viewCertainties, i);
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
			if (cachedAttributes == null || group.hasChanges(linker, cachedAttributes)) {
				viewGroups.add(group);
			}
		}
		return viewGroups;
	} 
	
	private static boolean hasAttachment(BeanShellLinker linker, String filename, boolean sync) {
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
