package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.widget.LinearLayout;
import au.org.intersect.faims.android.two.R;

public class EyeButton extends ToggleImageButton {

	private boolean highlight;
	private Drawable blueEye;
	private Drawable greyEye;
	private Drawable blackEye;
	
	public EyeButton(Context context) {
		super(context);
		setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		
		setFocusable(false);
		
		blueEye = context.getResources().getDrawable(R.drawable.layer_invisible);
		blackEye = context.getResources().getDrawable(R.drawable.layer_visible);
		greyEye = context.getResources().getDrawable(R.drawable.layer_visible);
		greyEye = greyEye.mutate();
		greyEye.setColorFilter(0x4CFFFFFF, Mode.MULTIPLY);
		
		setBackgroundResource(R.drawable.custom_button);
	}
	
	public void setHighlight(boolean value) {
		this.highlight = value;
		updateButtonState();
	}
	
	@Override
	protected void updateButtonState() {
		if (highlight) {
			if (isChecked()) {
				setImageDrawable(blackEye);
			} else {
				setImageDrawable(blueEye);
			}
		} else {
			if (isChecked()) {
				setImageDrawable(blackEye);
			} else {
				setImageDrawable(greyEye);
			}
		}
	}

}
