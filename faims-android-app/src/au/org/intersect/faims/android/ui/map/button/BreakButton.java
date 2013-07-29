package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import au.org.intersect.faims.android.R;

public class BreakButton extends ToggleToolButton {

	public BreakButton(Context context) {
		super(context);
		setLabel("Break");
		setNormalState(R.drawable.lock_button);
		setMutatedSelectedState(R.drawable.lock_button);
	}
	
}
