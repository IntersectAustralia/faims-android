package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import au.org.intersect.faims.android.two.R;

public class LockButton extends ToggleToolButton {
	
	public LockButton(Context context) {
		super(context);
		setLabel("Lock");
		setNormalState(R.drawable.lock_button);
		setMutatedSelectedState(R.drawable.lock_button);
	}
	
	@Override
	public void updateChecked() {
		setLabel(isChecked() ? "Unlock" : "Lock");
	}
	
}
