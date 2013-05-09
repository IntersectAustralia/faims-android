package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.widget.Button;

public class CustomToggleButton extends Button {

	private boolean selected;

	public CustomToggleButton(Context context) {
		super(context);
	}
	
	public void toggle() {
		selected = !selected;
		updateButton();
	}
	
	public void setToggle(boolean value) {
		selected = value;
		updateButton();
	}
	
	public boolean isToggle() {
		return selected;
	}
	
	private void updateButton() {
		getBackground().clearColorFilter();
		if (!selected) {
			getBackground().setColorFilter(Color.WHITE, Mode.MULTIPLY);
		}
	}

}
