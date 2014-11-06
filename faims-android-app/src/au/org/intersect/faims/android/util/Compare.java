package au.org.intersect.faims.android.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.ui.view.CustomCheckBoxGroup;
import au.org.intersect.faims.android.ui.view.CustomFileList;
import au.org.intersect.faims.android.ui.view.ICustomView;

public class Compare {
	
	public static boolean equal(String s1, String s2) {
		return (s1 == null && s2 == null) || (s1 != null && s2 != null && s1.equals(s2));
	}
	
	public static boolean equal(float f1, float f2) {
		return f1 == f2;
	}
	
	private static boolean cleanAndEqual(String s1, String s2) {
		return equal(clean(s1), clean(s2));
	}
	
	public static boolean compareAttributeValue(ICustomView view, HashMap<String, ArrayList<Attribute>> attributes) {
		if (attributes.get(view.getAttributeName()) != null) {
			for (Attribute a : attributes.get(view.getAttributeName())) {
				if (equal(a.getName(), view.getAttributeName())) {
					boolean valueEqual = cleanAndEqual(a.getValue(view.getAttributeType()), view.getValue());
					if (!valueEqual) return true;
					
					boolean annotationEqual;
					if (view.getAnnotationEnabled()) {
						annotationEqual = cleanAndEqual(a.getAnnotation(view.getAttributeType()), view.getAnnotation());
					} else {
						annotationEqual = true;
					}
					if (!annotationEqual) return true;
					
					boolean certaintyEqual;
					if (view.getCertaintyEnabled()) {
						certaintyEqual = cleanAndEqual(a.getCertainty(), String.valueOf(view.getCertainty()));
					} else {
						certaintyEqual = true;
					}
					if (!certaintyEqual) return true;
					
					break;
				}
			}
		}
		return false;
	}
	
	public static boolean compareAttributeValues(
			ICustomView view,
			HashMap<String, ArrayList<Attribute>>  attributes) {
		HashSet<String> attributeValues = new HashSet<String>();
		String attributeAnnotation = null;
		String attributeCertainty = null;
			
		if (attributes.get(view.getAttributeName()) != null) {
			for (Attribute a : attributes.get(view.getAttributeName())) {
				if (equal(a.getName(), view.getAttributeName())) {
					attributeValues.add(a.getValue(view.getAttributeType()));
					
					// Note: this assumes that the annotation and certainty are the same for each multi-value
					attributeAnnotation = a.getAnnotation(view.getAttributeType());
					attributeCertainty = a.getCertainty();
				}
			}
		}
			
		boolean valuesEqual = compareValues(clean(attributeValues), clean(convertToSet(view.getValues())));
		if (!valuesEqual) return true;
		
		boolean annotationEqual;
		if (view.getAnnotationEnabled() && attributeAnnotation != null) {
			annotationEqual = cleanAndEqual(attributeAnnotation, view.getAnnotation());
		} else {
			annotationEqual = true;
		}
		if (!annotationEqual) return true;
		
		boolean certaintyEqual;
		if (view.getCertaintyEnabled() && attributeCertainty != null) {
			certaintyEqual = cleanAndEqual(attributeCertainty, String.valueOf(view.getCertainty()));
		} else {
			certaintyEqual = true;
		}
		if (!certaintyEqual) return true;
		
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private static HashSet<String> convertToSet(List<?> list) {
		HashSet<String> set = new HashSet<String>();
		if (list == null || list.size() == 0) return set;
		if (list.get(0) instanceof NameValuePair) {
			List<NameValuePair> pairs = (List<NameValuePair>) list;
			for (NameValuePair pair : pairs) {
				set.add(pair.getName());
			}
		} else {
			List<String> values = (List<String>) list;
			for (String value : values) {
				set.add(value);
			}
		}
		return set;
	}

	public static boolean compareFileAttributeValues(
			CustomFileList view,
			HashMap<String, ArrayList<Attribute>> attributes, Module module) {
		HashSet<String> attributeValues = new HashSet<String>();
		HashSet<String> attributeAnnotations = new HashSet<String>();
		HashSet<String> attributeCertainties = new HashSet<String>();
		
		if (attributes.get(view.getAttributeName()) != null) {
			for (Attribute a : attributes.get(view.getAttributeName())) {
				if (equal(a.getName(), view.getAttributeName())) {
					attributeValues.add(a.getValue(view.getAttributeType()));
					attributeAnnotations.add(a.getAnnotation(view.getAttributeType()) == null ? "" : a.getAnnotation(view.getAttributeType()));
					attributeCertainties.add(a.getCertainty());
				}
			}
		}
		
		boolean valuesEqual = compareValues(clean(attributeValues), stripModulePath(module, (HashSet<String>) clean(convertToSet(view.getValues()))));
		if (!valuesEqual) return true;
		
		boolean annotationEqual;
		if (view.getAnnotationEnabled()) {
			annotationEqual = compareValues(clean(attributeAnnotations), clean(convertToSet(view.getAnnotations())));
		} else {
			annotationEqual = true;
		}
		if (!annotationEqual) return true;
		
		boolean certaintyEqual;
		if (view.getCertaintyEnabled()) {
			certaintyEqual = compareValues(clean(attributeCertainties), clean(convertToSet(view.getCertainties())));
		} else {
			certaintyEqual = true;
		}
		if (!certaintyEqual) return true;
		
		return false;
	}
	
	public static boolean compareMultiAttributeValues(
			CustomCheckBoxGroup view,
			HashMap<String, ArrayList<Attribute>> attributes, Module module) {
		HashSet<String> attributeValues = new HashSet<String>();
		HashSet<String> attributeAnnotations = new HashSet<String>();
		HashSet<String> attributeCertainties = new HashSet<String>();
		
		if (attributes.get(view.getAttributeName()) != null) {
			for (Attribute a : attributes.get(view.getAttributeName())) {
				if (equal(a.getName(), view.getAttributeName())) {
					attributeValues.add(a.getValue(view.getAttributeType()));
					attributeAnnotations.add(a.getAnnotation(view.getAttributeType()) == null ? "" : a.getAnnotation(view.getAttributeType()));
					attributeCertainties.add(a.getCertainty());
				}
			}
		}
		
		boolean valuesEqual = compareValues(clean(attributeValues), clean(convertToSet(view.getValues())));
		if (!valuesEqual) return true;
		
		boolean annotationEqual;
		if (view.getAnnotationEnabled()) {
			annotationEqual = compareValues(clean(attributeAnnotations), clean(convertToSet(view.getAnnotations())));
		} else {
			annotationEqual = true;
		}
		if (!annotationEqual) return true;
		
		boolean certaintyEqual;
		if (view.getCertaintyEnabled()) {
			certaintyEqual = compareValues(clean(attributeCertainties), clean(convertToSet(view.getCertainties())));
		} else {
			certaintyEqual = true;
		}
		if (!certaintyEqual) return true;
		
		return false;
	}
	
	public static boolean compareValues(HashSet<?> values1, HashSet<?> values2) {
		if (values1 == null && values2 == null) return true;
		if (values1 == null && values2 != null) return false;
		if (values1 != null && values2 == null) return false;
		if (values1.size() != values2.size()) return false;
		return values1.equals(values2);
	}
	
	public static boolean compareValues(List<?> values1, List<?> values2) {
		if (values1 == null && values2 == null) return true;
		if (values1 == null && values2 != null) return false;
		if (values1 != null && values2 == null) return false;
		if (values1.size() != values2.size()) return false;
		return values1.equals(values2);
	}
	
	private static String clean(String value) {
		if (value != null && value.isEmpty()) {
			return null;
		}
		return value;
	}
	
	private static HashSet<String> clean(HashSet<String> values) {
		if (values != null && values.isEmpty()) {
			return null;
		}
		return values;
	}
	
	private static HashSet<String> stripModulePath(Module module, HashSet<String> values) {
		if (values == null) {
			return null;
		}
		HashSet<String> newValues = new HashSet<String>();
		for (String value : values) {
			String strippedName = value.replace(module.getDirectoryPath().getPath() + "/", "");
			newValues.add(strippedName);
		}
		return newValues;
	}
	
}
