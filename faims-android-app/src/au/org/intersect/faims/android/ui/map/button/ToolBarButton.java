package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.map.ToolsBarView;

public class ToolBarButton extends ToggleToolButton {
	
	public ToolBarButton(Context context) {
		super(context);
	}
	
	@Override
	protected float getSize() {
		return ToolsBarView.BAR_HEIGHT;
	}
	
	@Override
	protected int getBackgroundResId() {
		return R.drawable.custom_button;
	}

}
