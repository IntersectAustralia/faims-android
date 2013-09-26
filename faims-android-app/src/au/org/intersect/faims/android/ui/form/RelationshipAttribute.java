package au.org.intersect.faims.android.ui.form;

public class RelationshipAttribute extends Attribute {
	
	public RelationshipAttribute() {
		
	}
	
	public RelationshipAttribute(String name, String text, String vocab, String certainty) {
		this(name, text, vocab, certainty, false);
	}
	
	public RelationshipAttribute(String name, String text, String vocab, String certainty, boolean isDeleted) {
		this.name = name;
		this.text = text;
		this.vocab = vocab;
		this.certainty = certainty;
		this.isDeleted = isDeleted;
	}
	
	public String getValue() {
		if (VOCAB.equals(type)) {
			return vocab;
		} else {
			return text;
		}
	}
	
	public String getAnnotation() {
		if (VOCAB.equals(type)) {
			return text;
		} else {
			return null;
		}
	}

}
