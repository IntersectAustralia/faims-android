package au.org.intersect.faims.util;

import android.app.Dialog;

public class DialogRunnable implements Runnable{
	
	private Dialog d;
	
	public DialogRunnable(Dialog dialog) {
		this.d = dialog;
	}
	
	@Override
	public void run() {
		d.show();
	}

}
