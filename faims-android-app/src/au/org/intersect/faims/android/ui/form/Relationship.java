package au.org.intersect.faims.android.ui.form;

import java.util.Collection;

public class Relationship {

	private String type;
	
	private Collection<RelationshipAttribute> attributes;

	public Relationship(String type, Collection<RelationshipAttribute> attributes){
		this.type = type;
		this.attributes = attributes;
	}

	public String getType() {
		return type;
	}

	public Collection<RelationshipAttribute> getAttributes() {
		return attributes;
	}
}
