package au.org.intersect.faims.android.ui.view;

import com.nativecss.NativeCSS;

import android.content.Context;
import android.widget.RadioButton;

public class CustomRadioButton extends RadioButton {

	public static int rbId = 1;
	private String value;

	public CustomRadioButton(Context context) {
		super(context);
		setId(rbId++);
		NativeCSS.addCSSClass(this, "radio-button");
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}

}
