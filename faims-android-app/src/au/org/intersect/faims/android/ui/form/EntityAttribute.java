package au.org.intersect.faims.android.ui.form;

public class EntityAttribute {
	
	private String name;
	private String text;
	private String measure;
	private String certainty;
	private String vocab;
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

	public boolean hasFreeText(){
		return this.text != null;
	}

	public boolean hasMeasure(){
		return this.measure != null;
	}

	public boolean hasCertainty(){
		return this.certainty != null;
	}

	public boolean hasVocab(){
		return this.vocab != null;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
