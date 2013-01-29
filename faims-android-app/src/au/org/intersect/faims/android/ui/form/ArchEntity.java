package au.org.intersect.faims.android.ui.form;

import java.util.Collection;
import java.util.List;

import com.nutiteq.geometry.Geometry;

public class ArchEntity {

	private String type;
	
	private Collection<EntityAttribute> attributes;

	private List<Geometry> geometryList;

	public ArchEntity(String type, Collection<EntityAttribute> attributes, List<Geometry> geometryList){
		this.type = type;
		this.attributes = attributes;
		this.geometryList = geometryList;
	}

	public String getType() {
		return type;
	}

	public Collection<EntityAttribute> getAttributes() {
		return attributes;
	}
	
	public List<Geometry> getGeometryList() {
		return geometryList;
	}
}
