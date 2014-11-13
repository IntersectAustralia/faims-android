package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import au.org.intersect.faims.android.two.R;

public class PropertiesButton extends ToolButton {

	public PropertiesButton(Context context) {
		super(context);
		setLabel("Properties");
		setImageResource(R.drawable.properties_button);
	}

}
