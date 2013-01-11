package au.org.intersect.faims.android.ui.form;

import java.util.Collection;

public class ArchEntity {

	private String type;
	
	private Collection<EntityAttribute> attributes;

	public ArchEntity(String type, Collection<EntityAttribute> attributes){
		this.type = type;
		this.attributes = attributes;
	}

	public String getType() {
		return type;
	}

	public Collection<EntityAttribute> getAttributes() {
		return attributes;
	}
}
