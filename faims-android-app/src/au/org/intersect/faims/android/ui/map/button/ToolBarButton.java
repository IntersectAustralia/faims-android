package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.map.ToolsBarView;
import au.org.intersect.faims.android.util.ScaleUtil;

public class ToolBarButton extends RelativeLayout {
	
	private static final int TEXT_COLOR = 0x88FFFFFF;
	
	class ToolBarToggleButton extends ToggleImageButton {

		public ToolBarToggleButton(Context context) {
			super(context);
			setLayoutParams(new LayoutParams((int) ScaleUtil.getDip(context, ToolsBarView.BAR_HEIGHT), (int) ScaleUtil.getDip(context, ToolsBarView.BAR_HEIGHT)));
			setBackgroundResource(R.drawable.custom_button);
		}

		@Override
		protected void updateButtonState() {
			setImageResource(isChecked() ? selectedState : normalState);
		}
		
	}
	
	private int selectedState;
	private int normalState;
	private ToggleImageButton button;
	private TextView label;
	private RelativeLayout labelHolder;

	public ToolBarButton(Context context) {
		super(context);
		setLayoutParams(new LayoutParams((int) ScaleUtil.getDip(context, ToolsBarView.BAR_HEIGHT), (int) ScaleUtil.getDip(context, ToolsBarView.BAR_HEIGHT)));
		
		labelHolder = new RelativeLayout(context);
		labelHolder.setLayoutParams(new LayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)));
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
		
		this.button = new ToolBarToggleButton(context);
		addView(button);
	}
	
	public void setLabel(String value) {
		this.label.setText(value);
	}
	
	public void setSelectedState(int resourceId) {
		selectedState = resourceId;
		button.updateButtonState();
	}
	
	public void setNormalState(int resourceId) {
		normalState = resourceId;
		button.updateButtonState();
	}
	
	public boolean isChecked() {
		return button.isChecked();
	}
	
	public void setChecked(boolean value) {
		button.setChecked(value);
	}
	
	@Override
	public void setOnClickListener(OnClickListener l) {
		button.setOnClickListener(l);
	}
	
	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		button.setOnLongClickListener(l);
	}

}
