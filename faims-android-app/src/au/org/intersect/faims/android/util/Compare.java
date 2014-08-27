package au.org.intersect.faims.android.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.data.NameValuePair;
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
	
	public static boolean compareAttributeValue(ICustomView view, Collection<? extends Attribute> attributes) {
		for (Attribute a : attributes) {
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
		return false;
	}

	@SuppressWarnings("unchecked")
	public static boolean compareAttributeValues(
			ICustomView view,
			Collection<? extends Attribute> attributes) {
		ArrayList<NameValuePair> attributeValues = new ArrayList<NameValuePair>();
		String attributeAnnotation = null;
		String attributeCertainty = null;
			
		for (Attribute a : attributes) {
			if (equal(a.getName(), view.getAttributeName())) {
				attributeValues.add(new NameValuePair(a.getValue(view.getAttributeType()), "true"));
				
				// Note: this assumes that the annotation and certainty are the same for each multi-value
				attributeAnnotation = a.getAnnotation(view.getAttributeType());
				attributeCertainty = a.getCertainty();
			}
		}
			
		boolean valuesEqual = compareValues(clean(attributeValues), clean((List<NameValuePair>) view.getValues()));
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
	public static boolean compareFileAttributeValues(
			ICustomView view,
			Collection<? extends Attribute> attributes, Module module) {
		ArrayList<NameValuePair> attributeValues = new ArrayList<NameValuePair>();
		String attributeAnnotation = null;
		String attributeCertainty = null;
		
		for (Attribute a : attributes) {
			if (equal(a.getName(), view.getAttributeName())) {
				attributeValues.add(new NameValuePair(a.getValue(view.getAttributeType()), "true"));
				
				// Note: this assumes that the annotation and certainty are the same for each multi-value
				attributeAnnotation = a.getAnnotation(view.getAttributeType());
				attributeCertainty = a.getCertainty();
			}
		}
		
		boolean valuesEqual = compareValues(clean(attributeValues), stripModulePath(module, clean((List<NameValuePair>) view.getValues())));
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
	
	public static boolean compareValues(List<NameValuePair> values1, List<NameValuePair> values2) {
		if (values1 == null && values2 == null) return true;
		if (values1 == null && values2 != null) return false;
		if (values1 != null && values2 == null) return false;
		if (values1.size() != values2.size()) return false;
			
		for (int i = 0; i < values1.size(); i++) {
			boolean hasValue = false;
			for (int j = 0; j < values2.size(); j++) {
				if (values1.get(i).equals(values2.get(j))) {
					hasValue = true;
					break;
				}
			}
			if (!hasValue) return false;
		}
		
		return true;
	}
	
	private static String clean(String value) {
		if (value != null && value.isEmpty()) {
			return null;
		}
		return value;
	}
	
	private static List<NameValuePair> clean(List<NameValuePair> values) {
		if (values != null && values.isEmpty()) {
			return null;
		}
		return values;
	}
	
	private static List<NameValuePair> stripModulePath(Module module, List<NameValuePair> values) {
		if (values == null) {
			return null;
		}
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		for (NameValuePair pair : values) {
			String strippedName = pair.getName().replace(module.getDirectoryPath().getPath() + "/", "");
			pairs.add(new NameValuePair(strippedName, pair.getValue()));
		}
		return pairs;
	}
	
}
