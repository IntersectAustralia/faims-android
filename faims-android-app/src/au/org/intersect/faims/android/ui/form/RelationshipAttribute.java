package au.org.intersect.faims.android.ui.form;

public class RelationshipAttribute {

	private String name;
	private String text;
	private String vocab;
	private String certainty;
	private String type;
	private boolean isDeleted;
	private boolean dirty;
	private String dirtyReason;

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

}
