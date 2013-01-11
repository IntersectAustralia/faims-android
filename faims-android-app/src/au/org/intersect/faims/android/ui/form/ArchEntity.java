package au.org.intersect.faims.android.ui.form;

public class ArchEntity {

	private String type;
	
	private EntityAttribute entityAttribute;

	public ArchEntity(String type, EntityAttribute entityAttribute){
		this.type = type;
		this.entityAttribute = entityAttribute;
	}

	public String getType() {
		return type;
	}

	public EntityAttribute getEntityAttribute() {
		return entityAttribute;
	}
}
