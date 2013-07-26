package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.widget.ImageButton;
import au.org.intersect.faims.android.R;

public class UndoButton extends ImageButton {

	public UndoButton(Context context) {
		super(context);
		setImageResource(R.drawable.undo_button);
		setBackgroundResource(R.drawable.custom_tool_button);
	}

}
