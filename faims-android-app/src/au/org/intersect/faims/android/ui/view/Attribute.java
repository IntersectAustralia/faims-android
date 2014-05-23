package au.org.intersect.faims.android.ui.view;

public abstract class Attribute {
	
	public static final String FREETEXT = "freetext";
	public static final String MEASURE = "measure";
	public static final String VOCAB = "vocab";
	
	protected String name;
	protected String text;
	protected String vocab;
	protected String certainty;
	protected String type;
	protected boolean isDeleted;
	protected boolean dirty;
	protected String dirtyReason;
	
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

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public String toString() {
		return "(" + name + "," + text + "," + vocab + "," + certainty + "," + isDeleted + ")";
	}

	public void setDirty(boolean value) {
		this.dirty = value;
	}
	
	public boolean isDirty() {
		return this.dirty;
	}
	
	public void setDirtyReason(String value) {
		this.dirtyReason = value;
	}
	
	public String getDirtyReason() {
		return dirtyReason;
	}
	
	public abstract String getValue(String type);
	
	public abstract String getAnnotation(String type);
	
}
