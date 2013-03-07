package au.org.intersect.faims.android.ui.form;

import java.util.Collection;
import java.util.List;

import com.nutiteq.geometry.Geometry;

public class ArchEntity {

	private String type;
	
	private Collection<EntityAttribute> attributes;

	private List<Geometry> geometryList;

	private String id;

	public ArchEntity(String id, String type, Collection<EntityAttribute> attributes, List<Geometry> geometryList){
		this.id = id;
		this.type = type;
		this.attributes = attributes;
		this.geometryList = geometryList;
	}
	
	public String getId() {
		return id;
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
