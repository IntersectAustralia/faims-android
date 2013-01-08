package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.widget.CheckBox;

public class CustomCheckBox extends CheckBox {

	private String value;

	public CustomCheckBox(Context context) {
		super(context);
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
}
