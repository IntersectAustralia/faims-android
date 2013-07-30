package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.LinearLayout;
import android.widget.ToggleButton;
import au.org.intersect.faims.android.R;

public class RestrictedButton extends ToggleButton {

	private Drawable lock;
	private Drawable unlock;
	
	public RestrictedButton(Context context) {
		super(context);
		setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		setText("");
		setTextOn("");
		setTextOff("");
		
		setFocusable(false);
		
		lock = context.getResources().getDrawable(R.drawable.restricted_button);
		unlock = context.getResources().getDrawable(R.drawable.unrestricted_button);
	}
	
	@Override
	public void setChecked(boolean value) {
		super.setChecked(value);
		updateButtonState();
	}

	protected void updateButtonState() {
		if (isChecked()) {
			setBackgroundDrawable(lock);
		} else {
			setBackgroundDrawable(unlock);
		}
	}

}
