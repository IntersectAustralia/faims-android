package au.org.intersect.faims.android.ui.view;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;

import com.google.inject.Inject;
import com.nativecss.NativeCSS;

public class CustomButton extends Button implements IView {
	
	@Inject
	BeanShellLinker linker;

	private static final long DELAY = 1000;
	
	private String ref;
	private boolean dynamic;

	private long timestamp;

	private String delayClickCallback;
	private String clickCallback;
	private String focusCallback;
	private String blurCallback;

	public CustomButton(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public CustomButton(Context context, String ref, boolean dynamic) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
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
	
	public String getDelayClickCallback() {
		return delayClickCallback;
	}

	public void setDelayClickCallback(String code) {
		delayClickCallback = code;
		setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				CustomButton button = (CustomButton) v;
				if (button.canClick()) {
					linker.execute(delayClickCallback);
					button.clicked();
				}
			}
		});
	}
	
	@Override
	public String getClickCallback() {
		return clickCallback;
	}

	@Override
	public void setClickCallback(String code) {
		if (code == null) return;
		clickCallback = code;
		setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				linker.execute(clickCallback);
			}
		});
	}
	
	@Override
	public String getSelectCallback() {
		return null;
	}

	@Override
	public void setSelectCallback(String code) {
	}

	@Override
	public String getFocusCallback() {
		return focusCallback;
	}
	
	@Override
	public String getBlurCallback() {
		return blurCallback;
	}
	
	@Override
	public void setFocusBlurCallbacks(String focusCode, String blurCode) {
		if (focusCode == null && blurCode == null) return;
		focusCallback = focusCode;
		blurCallback = blurCode;
		setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					linker.execute(focusCallback);
				} else {
					linker.execute(blurCallback);
				}
			}
		});
	}

}
