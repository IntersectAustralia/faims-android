package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.widget.ImageButton;
import au.org.intersect.faims.android.R;

public class LayerManagerButton extends ImageButton {

	public LayerManagerButton(Context context) {
		super(context);
		setImageResource(R.drawable.layers_management);
		setBackgroundResource(R.drawable.custom_button);
	}

}
