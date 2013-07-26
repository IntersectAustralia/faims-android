package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.view.ViewGroup.LayoutParams;
import au.org.intersect.faims.android.R;

public class ToolBarButton extends ToggleImageButton {
	
	private int selectedState;
	private int normalState;

	public ToolBarButton(Context context) {
		super(context);
		setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		setBackgroundResource(R.drawable.custom_button);
	}
	
	public void setSelectedState(int resourceId) {
		selectedState = resourceId;
		updateButtonState();
	}
	
	public void setNormalState(int resourceId) {
		normalState = resourceId;
		updateButtonState();
	}
	
	@Override
	protected void updateButtonState() {
		setImageResource(isChecked() ? selectedState : normalState);
	}

}
