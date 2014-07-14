package au.org.intersect.faims.android.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.nutiteq.geometry.Geometry;

public class Record implements Serializable {
	
	private static final long serialVersionUID = -2427576343527659836L;
	
	protected String type;
	protected List<Geometry> geometryList;
	protected String id;
	protected boolean forked;
	
	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}
	
	public List<Geometry> getGeometryList() {
		return geometryList;
	}
	
	public void setGeometryList(List<Geometry> geomList) {
		geometryList = geomList;
	}
	
	public boolean isForked() {
		return forked;
	}
	
	protected Collection<? extends Attribute> updateAttributes(
			Collection<? extends Attribute> attributes, Collection<? extends Attribute> newAttributes) {
		ArrayList<Attribute> modifiedAttributes = new ArrayList<Attribute>();
		HashMap<String, ArrayList<Attribute>> attributesMap = convertCollectionToMap(attributes);
		HashMap<String, ArrayList<Attribute>> newAttributesMap = convertCollectionToMap(newAttributes);
		// add new attributes that don't exist
		for (String name : newAttributesMap.keySet()) {
			if (attributesMap.get(name) == null) {
				ArrayList<Attribute> attrs = newAttributesMap.get(name);
				if (attrs != null && !attrs.isEmpty()) {
					// only add attributes that are not deleted
					if (!attrs.get(0).isDeleted()) {
						modifiedAttributes.addAll(attrs);
					}
				}
			}
		}
		// update/delete attributes that exist
		for (String name : attributesMap.keySet()) {
			ArrayList<Attribute> attrs = newAttributesMap.get(name);
			if (attrs != null && !attrs.isEmpty()) {
				// only add attributes that are not deleted
				if (!attrs.get(0).isDeleted()) {
					modifiedAttributes.addAll(attrs);
				}
			} else {
				modifiedAttributes.addAll(attributesMap.get(name));
			}
		}
		return modifiedAttributes;
	}

	private HashMap<String, ArrayList<Attribute>> convertCollectionToMap(
			Collection<? extends Attribute> attributes) {
		HashMap<String, ArrayList<Attribute>> map = new HashMap<String, ArrayList<Attribute>>();
		for (Attribute a : attributes) {
			if (map.get(a.getName()) == null) {
				map.put(a.getName(), new ArrayList<Attribute>());
			}
			map.get(a.getName()).add(a);
		}
		return map;
	}
}
