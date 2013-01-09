package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.widget.DatePicker;

public class CustomDatePicker extends DatePicker {

	private String name;
	private String type;
	
	public CustomDatePicker(Context context) {
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
}
