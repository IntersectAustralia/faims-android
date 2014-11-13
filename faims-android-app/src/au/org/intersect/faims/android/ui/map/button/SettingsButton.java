package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import au.org.intersect.faims.android.two.R;

public class SettingsButton extends ToolButton {

	public SettingsButton(Context context) {
		super(context);
		setLabel("Style");
		setImageResource(R.drawable.style_button);
	}

}
