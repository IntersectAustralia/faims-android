package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import au.org.intersect.faims.android.two.R;

public class BreakButton extends ToggleToolButton {

	public BreakButton(Context context) {
		super(context);
		setLabel("Break");
		setNormalState(R.drawable.tools_break);
		setSelectedState(R.drawable.tools_break_s);
	}
	
	@Override
	public void updateChecked() {
		setLabel(isChecked() ? "Join" : "Break");
	}
	
}
