package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.widget.DatePicker;

public class CustomDatePicker extends DatePicker {

	private String attributeName;
	private String attributeType;
	private String ref;
	
	public CustomDatePicker(Context context) {
		super(context);
	}
	
	public CustomDatePicker(Context context, String attributeName, String attributeType, String ref) {
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
}
