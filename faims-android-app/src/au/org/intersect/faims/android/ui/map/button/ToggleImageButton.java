package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.view.View;
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
	
	@Override
	public void setOnClickListener(final OnClickListener l) {
		super.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				setChecked(!isChecked());
				l.onClick(arg0);
			}
			
		});
	}

}
