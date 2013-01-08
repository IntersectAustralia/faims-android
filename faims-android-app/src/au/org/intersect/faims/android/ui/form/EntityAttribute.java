package au.org.intersect.faims.android.ui.form;

public class EntityAttribute {
	
	private String name;
	private String text;
	private String measure;
	private String certainty;
	private String vocab;

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
	
	public String getMeasure() {
		return measure;
	}
	
	public void setMeasure(String value) {
		measure = value;
	}
	
	public String getCertainty() {
		return certainty;
	}
	
	public void setCertainty(String value) {
		certainty = value;
	}
	
	public String getVocab() {
		return vocab;
	}
	
	public void setVocab(String value) {
		vocab = value;
	}
	
	public String toString() {
		return "(" + name + "," + text + "," + vocab + "," + measure + "," + certainty +")";
	}

}
