package au.org.intersect.faims.android.ui.view;

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
	
	@Override
	public String getValue(String type) {
		if (VOCAB.equals(type)) {
			return vocab;
		} else {
			return text;
		}
	}
	
	@Override
	public String getAnnotation(String type) {
		if (VOCAB.equals(type)) {
			return text;
		} else {
			return null;
		}
	}

}
