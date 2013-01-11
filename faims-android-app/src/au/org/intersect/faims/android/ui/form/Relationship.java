package au.org.intersect.faims.android.ui.form;

public class Relationship {

	private String type;
	
	private RelationshipAttribute relationshipAttribute;

	public Relationship(String type, RelationshipAttribute relationshipAttribute){
		this.type = type;
		this.relationshipAttribute = relationshipAttribute;
	}

	public String getType() {
		return type;
	}

	public RelationshipAttribute getRelationshipAttribute() {
		return relationshipAttribute;
	}
}
