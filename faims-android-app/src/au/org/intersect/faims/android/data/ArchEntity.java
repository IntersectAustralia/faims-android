package au.org.intersect.faims.android.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;


import com.nutiteq.geometry.Geometry;

public class ArchEntity implements Serializable {

	private static final long serialVersionUID = 1815713472141263495L;

	private String type;
	
	private Collection<EntityAttribute> attributes;

	private List<Geometry> geometryList;

	private String id;

	private boolean forked;

	public ArchEntity(String id, String type, Collection<EntityAttribute> attributes, List<Geometry> geometryList, boolean forked){
		this.id = id;
		this.type = type;
		this.attributes = attributes;
		this.geometryList = geometryList;
		this.forked = forked;
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
	
	public void setGeometryList(List<Geometry> geomList) {
		geometryList = geomList;
	}
	
	public boolean isForked() {
		return forked;
	}
}
