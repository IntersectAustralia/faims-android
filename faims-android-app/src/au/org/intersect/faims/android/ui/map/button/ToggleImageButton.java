package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.widget.ImageButton;

public abstract class ToggleImageButton extends ImageButton {

	private boolean checked;

	public ToggleImageButton(Context context) {
		super(context);
	}
	
	public boolean isChecked() {
		return checked;
	}
	
	public void setChecked(boolean checked) {
		this.checked = checked;
		updateButtonState();
	}
	
	protected abstract void updateButtonState();

}
