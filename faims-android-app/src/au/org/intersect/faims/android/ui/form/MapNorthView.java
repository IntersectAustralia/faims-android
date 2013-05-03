package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.widget.ImageView;
import au.org.intersect.faims.android.R;

public class MapNorthView extends ImageView {
	
	private float lastRotation;

	public MapNorthView(Context context) {
		super(context);
		setImageResource(R.drawable.small_north);
		setMapRotation(0);
	}
	
	public void setMapRotation(float value) {
		if (lastRotation == value) return;
		lastRotation = value;
		this.setRotation(value);
		invalidate();
	}

}
