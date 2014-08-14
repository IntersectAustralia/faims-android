package au.org.intersect.faims.android.ui.view;

import com.nativecss.NativeCSS;

import android.content.Context;
import android.widget.RadioButton;
import au.org.intersect.faims.android.util.ScaleUtil;

public class CustomRadioButton extends RadioButton {

	public static int rbId = 1;
	private static final int PADDING = 25;
	private String value;

	public CustomRadioButton(Context context) {
		super(context);
		setId(rbId++);
		setPadding(0, 0, (int) ScaleUtil.getDip(context, PADDING), 0);
		NativeCSS.addCSSClass(this, "radio-button");
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}

}
