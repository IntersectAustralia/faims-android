package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import au.org.intersect.faims.android.R;

public class PlotGPSButton extends ToolButton {

	public PlotGPSButton(Context context) {
		super(context);
		setLabel("Plot");
		setImageResource(R.drawable.plot_gps_button);
	}

}
