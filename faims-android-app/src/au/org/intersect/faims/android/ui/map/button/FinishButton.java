package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import au.org.intersect.faims.android.two.R;

public class FinishButton extends ToolButton {

	public FinishButton(Context context) {
		super(context);
		setLabel("Finish");
		setImageResource(R.drawable.finish_button);
	}

}
