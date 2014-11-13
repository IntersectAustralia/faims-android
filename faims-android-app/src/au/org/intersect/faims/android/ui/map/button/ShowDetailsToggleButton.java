package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import au.org.intersect.faims.android.two.R;

public class ShowDetailsToggleButton extends ToggleToolButton {
	
	public ShowDetailsToggleButton(Context context) {
		super(context);
		setLabel("Details");
		setNormalState(R.drawable.show_details);
		setMutatedSelectedState(R.drawable.show_details);
	}
	
}
