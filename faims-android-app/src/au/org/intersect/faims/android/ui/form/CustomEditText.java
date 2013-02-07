package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.widget.EditText;

public class CustomEditText extends EditText {

	private String attributeName;
	private String attributeType;
	private String ref;
	private float certainty = 1;
	private float currentCertainty = 1;
	private String annotation = "";
	private String currentAnnotation = "";
	
	public CustomEditText(Context context) {
		super(context);
	}
	
	public CustomEditText(Context context, String attributeName, String attributeType, String ref) {
		super(context);
		this.attributeName = attributeName;
		this.attributeType = attributeType;
		this.ref = ref;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public String getAttributeType() {
		return attributeType;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public float getCertainty() {
		return certainty;
	}

	public void setCertainty(float certainty) {
		this.certainty = certainty;
	}

	public float getCurrentCertainty() {
		return currentCertainty;
	}

	public void setCurrentCertainty(float currentCertainty) {
		this.currentCertainty = currentCertainty;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public String getCurrentAnnotation() {
		return currentAnnotation;
	}

	public void setCurrentAnnotation(String currentAnnotation) {
		this.currentAnnotation = currentAnnotation;
	}
}
