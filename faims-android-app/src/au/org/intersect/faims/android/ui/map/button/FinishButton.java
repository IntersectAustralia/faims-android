package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.widget.ImageButton;
import au.org.intersect.faims.android.R;

public class FinishButton extends ImageButton {

	public FinishButton(Context context) {
		super(context);
		setImageResource(R.drawable.finish_button);
		setBackgroundResource(R.drawable.custom_tool_button);
	}

}
