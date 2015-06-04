package au.org.intersect.faims.android.ui.view;

import android.content.Context;
import android.widget.CheckBox;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.managers.CSSManager;

import com.google.inject.Inject;

public class CustomCheckBox extends CheckBox {
	
	@Inject
	CSSManager cssManager;

	private String value;

	public CustomCheckBox(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		cssManager.addCSS(this, "checkbox");
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
}
