package au.org.intersect.faims.android.ui.view;

import android.content.Context;
import android.widget.RadioButton;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.managers.CSSManager;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.google.inject.Inject;

public class CustomRadioButton extends RadioButton {
	
	@Inject
	CSSManager cssManager;

	public static int rbId = 1;
	private static final int PADDING = 25;
	private String value;

	public CustomRadioButton(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		setId(rbId++);
		setPadding(0, 0, (int) ScaleUtil.getDip(context, PADDING), 0);
		cssManager.addCSS(this, "radio-button");
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}

}
