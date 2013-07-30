package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.widget.ImageButton;
import au.org.intersect.faims.android.R;

public class MapNorthView extends ImageButton {
	
	private float lastRotation;

	public MapNorthView(Context context) {
		super(context);
		setImageResource(R.drawable.north_arrow);
		setBackgroundResource(R.drawable.north_view_button);
		setMapRotation(0);
	}
	
	public void setMapRotation(float value) {
		if (lastRotation == value) return;
		lastRotation = value;
		this.setRotation(value);
		invalidate();
	}

}
