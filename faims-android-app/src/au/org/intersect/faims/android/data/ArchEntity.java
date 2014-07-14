package au.org.intersect.faims.android.data;

import java.util.Collection;
import java.util.List;

import com.nutiteq.geometry.Geometry;

public class ArchEntity extends Record {

	private static final long serialVersionUID = 1815713472141263495L;

	private Collection<EntityAttribute> attributes;

	public ArchEntity(String id, String type, Collection<EntityAttribute> attributes, List<Geometry> geometryList, boolean forked){
		this.id = id;
		this.type = type;
		this.attributes = attributes;
		this.geometryList = geometryList;
		this.forked = forked;
	}

	public Collection<EntityAttribute> getAttributes() {
		return attributes;
	}
	
	@SuppressWarnings("unchecked")
	public void updateAttributes(Collection<EntityAttribute> attributes) {
		if (this.attributes == null) {
			this.attributes = attributes;
		} else if (attributes != null) {
			this.attributes = (Collection<EntityAttribute>) updateAttributes(this.attributes, attributes);
		}
	}
}
