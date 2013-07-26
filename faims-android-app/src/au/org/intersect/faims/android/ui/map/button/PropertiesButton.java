package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.widget.ImageButton;
import au.org.intersect.faims.android.R;

public class PropertiesButton extends ImageButton {

	public PropertiesButton(Context context) {
		super(context);
		setImageResource(R.drawable.properties_button);
		setBackgroundResource(R.drawable.custom_tool_button);
	}

}
