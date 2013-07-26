package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import au.org.intersect.faims.android.R;

public class LockButton extends ToggleImageButton {

	private Drawable normalState;
	private Drawable selectedState;
	
	public LockButton(Context context) {
		super(context);
		normalState = context.getResources().getDrawable(R.drawable.lock_button);
		selectedState = context.getResources().getDrawable(R.drawable.lock_button);
		selectedState = selectedState.mutate();
		selectedState.setColorFilter(new PorterDuffColorFilter(0xFF00ddff,Mode.MULTIPLY));
		setBackgroundResource(R.drawable.custom_tool_button);
	}

	@Override
	protected void updateButtonState() {
		setImageDrawable(isChecked()? selectedState : normalState);
	}

}
