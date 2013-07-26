package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.widget.ImageButton;
import au.org.intersect.faims.android.R;

public class PlotGPSButton extends ImageButton {

	public PlotGPSButton(Context context) {
		super(context);
		setImageResource(R.drawable.plot_gps_button);
		setBackgroundResource(R.drawable.custom_tool_button);
	}

}
