package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.widget.ImageButton;
import android.widget.RelativeLayout.LayoutParams;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.map.LayerBarView;
import au.org.intersect.faims.android.util.ScaleUtil;

public class LayerManagerButton extends ImageButton {

	public LayerManagerButton(Context context) {
		super(context);
		setLayoutParams(new LayoutParams((int) ScaleUtil.getDip(context, LayerBarView.BAR_HEIGHT), (int) ScaleUtil.getDip(context, LayerBarView.BAR_HEIGHT)));
		setImageResource(R.drawable.layers_management);
		setBackgroundResource(R.drawable.custom_button);
	}

}
