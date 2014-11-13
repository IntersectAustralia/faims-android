package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import au.org.intersect.faims.android.two.R;

public class DeleteButton extends ToolButton {

	public DeleteButton(Context context) {
		super(context);
		setLabel("Delete");
		setImageResource(R.drawable.delete_button);
	}

}
