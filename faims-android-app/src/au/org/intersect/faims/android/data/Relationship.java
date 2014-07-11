package au.org.intersect.faims.android.data;

import java.util.Collection;
import java.util.List;

import com.nutiteq.geometry.Geometry;

public class Relationship extends Record {
	private static final long serialVersionUID = 4505186290558078330L;

	private Collection<RelationshipAttribute> attributes;

	public Relationship(String id, String type, Collection<RelationshipAttribute> attributes, List<Geometry> geomList, boolean forked){
		this.id = id;
		this.type = type;
		this.attributes = attributes;
		this.geometryList = geomList;
		this.forked = forked;
	}

	public Collection<RelationshipAttribute> getAttributes() {
		return attributes;
	}
	
	@SuppressWarnings("unchecked")
	public void updateAttributes(Collection<RelationshipAttribute> attributes) {
		if (this.attributes == null) {
			this.attributes = attributes;
		} else if (attributes != null) {
			this.attributes = (Collection<RelationshipAttribute>) updateAttributes(this.attributes, attributes);
		}
	}

}
