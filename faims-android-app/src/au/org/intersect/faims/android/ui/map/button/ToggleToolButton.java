package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.two.R;
import au.org.intersect.faims.android.util.ScaleUtil;

public class ToggleToolButton extends RelativeLayout {
	
	private class ToggleToolImageButton extends ToggleImageButton {

		public ToggleToolImageButton(Context context) {
			super(context);
			setLayoutParams(new LayoutParams((int) ScaleUtil.getDip(context, BUTTON_SIZE), (int) ScaleUtil.getDip(context, BUTTON_SIZE)));
		}

		@Override
		protected void updateButtonState() {
			setImageDrawable(isChecked() ? selectedState : normalState);
			setBackgroundResource(getBackgroundResId());
		}
		
	}
	
	private static final float BUTTON_SIZE = 55.0f;
	private static final int TEXT_COLOR = 0x88FFFFFF;
	
	private Drawable selectedState;
	private Drawable normalState;
	private ToggleToolImageButton button;
	private TextView label;
	private RelativeLayout labelHolder;

	public ToggleToolButton(Context context) {
		super(context);
		
		int size = (int) ScaleUtil.getDip(context, getSize());
		
		this.button = new ToggleToolImageButton(context);
		button.setLayoutParams(new LayoutParams(size, size));
		button.updateButtonState();
		addView(button);
		
		setLayoutParams(new LayoutParams(size, size));
		
		labelHolder = new RelativeLayout(context);
		labelHolder.setLayoutParams(new LayoutParams(size, size));
		addView(labelHolder);
		
		this.label = new TextView(context);
		label.setTextSize(10);
		label.setTextColor(TEXT_COLOR);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.alignWithParent = true;
		params.addRule(RelativeLayout.ALIGN_BOTTOM);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		label.setLayoutParams(params);
		labelHolder.addView(label);
	}
	
	protected float getSize() {
		return BUTTON_SIZE;
	}
	
	protected int getBackgroundResId() {
		return R.drawable.custom_tool_button;
	}
	
	public void setLabel(String value) {
		this.label.setText(value);
	}
	
	public void setSelectedState(int resourceId) {
		selectedState = getContext().getResources().getDrawable(resourceId);
		button.updateButtonState();
	}
	
	public void setMutatedSelectedState(int resourceId) {
		selectedState = getContext().getResources().getDrawable(resourceId);
		selectedState = selectedState.mutate();
		selectedState.setColorFilter(new PorterDuffColorFilter(0xFF00ddff,Mode.MULTIPLY));
		button.updateButtonState();
	}
	
	public void setNormalState(int resourceId) {
		normalState = getContext().getResources().getDrawable(resourceId);
		button.updateButtonState();
	}
	
	public boolean isChecked() {
		return button.isChecked();
	}
	
	public void setChecked(boolean value) {
		button.setChecked(value);
	}
	
	public void updateChecked() {
		
	}
	
	@Override
	public void setOnClickListener(final OnClickListener l) {
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				l.onClick(v);
				updateChecked();
			}
			
		});
	}
	
	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		button.setOnLongClickListener(l);
	}

}
