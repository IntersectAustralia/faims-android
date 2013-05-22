package au.org.intersect.faims.android.ui.form;

public class RelationshipAttribute {

	private String name;
	private String text;
	private String vocab;
	private String certainty;
	private String type;

	public String getName() {
		return name;
	}
	
	public void setName(String value) {
		name = value;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String value) {
		text = value;
	}
	
	public String getVocab() {
		return vocab;
	}
	
	public void setVocab(String value) {
		vocab = value;
	}
	
	public String getCertainty() {
		return certainty;
	}
	
	public void setCertainty(String value) {
		certainty = value;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String toString() {
		return "(" + name + "," + text + "," + vocab + "," + certainty + ")";
	}

}
