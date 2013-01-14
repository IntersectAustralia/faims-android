package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.widget.EditText;

public class CustomEditText extends EditText {

	private String attributeName;
	private String attributeType;
	private String ref;
	private String value;
	
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

	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
}
