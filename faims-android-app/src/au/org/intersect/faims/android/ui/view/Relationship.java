package au.org.intersect.faims.android.ui.view;

import java.util.Collection;
import java.util.List;

import com.nutiteq.geometry.Geometry;

public class Relationship {

	private String type;
	
	private Collection<RelationshipAttribute> attributes;

	private List<Geometry> geometryList;

	private String id;

	private boolean forked;

	public Relationship(String id, String type, Collection<RelationshipAttribute> attributes, List<Geometry> geomList, boolean forked){
		this.id = id;
		this.type = type;
		this.attributes = attributes;
		this.geometryList = geomList;
		this.forked = forked;
	}
	
	public String getId() {
		return id;
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
	
	public void setGeometryList(List<Geometry> geomList) {
		geometryList = geomList;
	}
	
	public boolean isForked() {
		return forked;
	}
}
