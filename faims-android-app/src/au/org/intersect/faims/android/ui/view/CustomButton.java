package au.org.intersect.faims.android.ui.view;

import com.nativecss.NativeCSS;

import android.content.Context;
import android.widget.Button;

public class CustomButton extends Button implements IView {

	private static final long DELAY = 1000;
	
	private String ref;
	private boolean dynamic;

	private long timestamp;

	public CustomButton(Context context) {
		super(context);
	}
	
	public CustomButton(Context context, String ref, boolean dynamic) {
		super(context);
		this.ref = ref;
		this.dynamic = dynamic;
		NativeCSS.addCSSClass(this, "button");
	}
	
	@Override
	public String getRef() {
		return ref;
	}
	
	@Override
	public boolean isDynamic() {
		return dynamic;
	}
	
	public void clicked() {
		this.timestamp = System.currentTimeMillis();
	}
	
	public boolean canClick() {
		return System.currentTimeMillis() > timestamp + DELAY;
	}

}
