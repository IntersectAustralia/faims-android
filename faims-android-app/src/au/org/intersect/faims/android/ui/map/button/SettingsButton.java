package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.widget.ImageButton;
import au.org.intersect.faims.android.R;

public class SettingsButton extends ImageButton {

	public SettingsButton(Context context) {
		super(context);
		setImageResource(R.drawable.style_button);
		setBackgroundResource(R.drawable.custom_tool_button);
	}

}
