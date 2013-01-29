package au.org.intersect.faims.android.ui.form;

import java.util.Collection;
import java.util.List;

import com.nutiteq.geometry.Geometry;

public class Relationship {

	private String type;
	
	private Collection<RelationshipAttribute> attributes;

	private List<Geometry> geometryList;

	public Relationship(String type, Collection<RelationshipAttribute> attributes, List<Geometry> geomList){
		this.type = type;
		this.attributes = attributes;
		this.geometryList = geomList;
	}

	public String getType() {
		return type;
	}

	public Collection<RelationshipAttribute> getAttributes() {
		return attributes;
	}
	
	public List<Geometry> getGeometryList() {
		return geometryList;
	}
}
