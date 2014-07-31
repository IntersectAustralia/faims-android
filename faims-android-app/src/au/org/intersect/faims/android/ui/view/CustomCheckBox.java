package au.org.intersect.faims.android.ui.view;

import com.nativecss.NativeCSS;

import android.content.Context;
import android.widget.CheckBox;

public class CustomCheckBox extends CheckBox {

	private String value;

	public CustomCheckBox(Context context) {
		super(context);
		NativeCSS.addCSSClass(this, "checkbox");
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
}
