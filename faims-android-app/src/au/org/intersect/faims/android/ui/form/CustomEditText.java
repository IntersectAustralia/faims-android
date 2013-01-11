package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.widget.EditText;

public class CustomEditText extends EditText {

	private String name;
	private String type;
	private String value;
	
	public CustomEditText(Context context) {
		super(context);
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String value) {
		name = value;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String value) {
		type = value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
}
