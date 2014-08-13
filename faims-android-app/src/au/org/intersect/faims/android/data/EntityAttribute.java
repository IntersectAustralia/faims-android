package au.org.intersect.faims.android.data;


public class EntityAttribute extends RelationshipAttribute {
	
	private static final long serialVersionUID = 6887790507746782461L;
	
	private String measure;
	
	public EntityAttribute() {
		
	}
	
	public EntityAttribute(String name, String text, String measure, String vocab, String certainty) {
		this(name, text, measure, vocab, certainty, false);
	}
	
	public EntityAttribute(String name, String text, String measure, String vocab, String certainty, boolean isDeleted) {
		super(name, text, vocab, certainty, isDeleted);
		this.measure = measure;
	}

	public String getMeasure() {
		return measure;
	}
	
	public void setMeasure(String value) {
		measure = value;
	}
	
	public String toString() {
		return "(" + name + "," + text + "," + vocab + "," + measure + "," + certainty + "," + isDeleted + ")";
	}
	
	@Override
	public String getValue(String type) {
		if (MEASURE.equals(type)) {
			return measure;
		} else if (VOCAB.equals(type)) {
			return vocab;
		} else if (CERTAINTY.equals(type)) {
			return certainty;
		} else {
			return text;
		}
	}
	
	@Override
	public String getAnnotation(String type) {
		return text;
	}
}
