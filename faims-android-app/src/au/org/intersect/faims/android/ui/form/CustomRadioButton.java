package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.widget.RadioButton;

public class CustomRadioButton extends RadioButton {

	public static int rbId = 1;
	private String value;

	public CustomRadioButton(Context context) {
		super(context);
		setId(rbId++);
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}

}
