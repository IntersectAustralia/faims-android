package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.widget.LinearLayout;
import android.widget.ToggleButton;
import au.org.intersect.faims.android.R;

public class EyeButton extends ToggleButton {

	private boolean highlight;
	private Drawable blueEye;
	private Drawable greyEye;
	private Drawable blackEye;
	
	public EyeButton(Context context) {
		super(context);
		setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		setText("");
		setTextOn("");
		setTextOff("");
		
		setFocusable(false);
		
		blueEye = context.getResources().getDrawable(R.drawable.layer_invisible);
		blackEye = context.getResources().getDrawable(R.drawable.layer_visible);
		greyEye = context.getResources().getDrawable(R.drawable.layer_visible);
		greyEye = greyEye.mutate();
		greyEye.setColorFilter(0x4CFFFFFF, Mode.MULTIPLY);
	}
	
	public void setHighlight(boolean value) {
		this.highlight = value;
		updateButtonState();
	}
	
	@Override
	public void setChecked(boolean value) {
		super.setChecked(value);
		updateButtonState();
	}

	protected void updateButtonState() {
		if (highlight) {
			if (isChecked()) {
				setBackgroundDrawable(blackEye);
			} else {
				setBackgroundDrawable(blueEye);
			}
		} else {
			if (isChecked()) {
				setBackgroundDrawable(blackEye);
			} else {
				setBackgroundDrawable(greyEye);
			}
		}
	}

}
