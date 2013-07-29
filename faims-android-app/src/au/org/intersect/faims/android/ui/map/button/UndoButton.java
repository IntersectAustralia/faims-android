package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import au.org.intersect.faims.android.R;

public class UndoButton extends ToolButton {

	public UndoButton(Context context) {
		super(context);
		setLabel("Undo");
		setImageResource(R.drawable.undo_button);
	}

}
