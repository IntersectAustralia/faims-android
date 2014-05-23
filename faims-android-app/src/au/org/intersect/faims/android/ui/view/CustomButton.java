package au.org.intersect.faims.android.ui.view;

import android.content.Context;
import android.widget.Button;

public class CustomButton extends Button {

	private static final long DELAY = 1000;
	private long timestamp;

	public CustomButton(Context context) {
		super(context);
	}
	
	public void clicked() {
		this.timestamp = System.currentTimeMillis();
	}
	
	public boolean canClick() {
		return System.currentTimeMillis() > timestamp + DELAY;
	}

}
