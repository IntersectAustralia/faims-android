package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import au.org.intersect.faims.android.R;

public class ClearButton extends ToolButton {

	public ClearButton(Context context) {
		super(context);
		setLabel("Clear");
		setImageResource(R.drawable.clear_button);
	}

}
