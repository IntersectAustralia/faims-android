package au.org.intersect.faims.android.ui.map.button;

import android.content.Context;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.util.ScaleUtil;

public class ToolButton extends RelativeLayout {
	
	private static final float BUTTON_SIZE = 55.0f;
	private static final int TEXT_COLOR = 0x88FFFFFF;
	
	private ImageButton button;
	private TextView label;
	private RelativeLayout labelHolder;

	public ToolButton(Context context) {
		super(context);
		
		this.button = new ImageButton(context);
		button.setLayoutParams(new LayoutParams((int) ScaleUtil.getDip(context, BUTTON_SIZE), (int) ScaleUtil.getDip(context, BUTTON_SIZE)));
		button.setBackgroundResource(R.drawable.custom_tool_button);
		addView(button);
		
		setLayoutParams(new LayoutParams((int) ScaleUtil.getDip(context, BUTTON_SIZE), (int) ScaleUtil.getDip(context, BUTTON_SIZE)));
		
		labelHolder = new RelativeLayout(context);
		labelHolder.setLayoutParams(new LayoutParams((int) ScaleUtil.getDip(context, BUTTON_SIZE), (int) ScaleUtil.getDip(context, BUTTON_SIZE)));
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
	
	public void setLabel(String value) {
		this.label.setText(value);
	}
	
	@Override
	public void setOnClickListener(OnClickListener l) {
		button.setOnClickListener(l);
	}
	
	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		button.setOnLongClickListener(l);
	}
	
	public void setImageResource(int resId) {
		button.setImageResource(resId);
	}

}
