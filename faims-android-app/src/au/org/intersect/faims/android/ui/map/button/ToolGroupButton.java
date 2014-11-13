package au.org.intersect.faims.android.ui.map.button;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.two.R;
import au.org.intersect.faims.android.ui.map.ToolsBarView;
import au.org.intersect.faims.android.util.ScaleUtil;

public class ToolGroupButton extends RelativeLayout {

	private static final int BAR_COLOR = 0x88000000;
	private static final int TEXT_COLOR = 0x88FFFFFF;
	
	private ArrayList<ToolBarButton> buttons;
	private ToolBarButton selectedButton;
	private PopupWindow popupMenu;
	private LinearLayout popupLayout;
	private LinearLayout anchor;
	private RelativeLayout holder;
	private TextView label;
	private ImageView image;

	public ToolGroupButton(Context context) {
		super(context);
		setLayoutParams(new LayoutParams((int) ScaleUtil.getDip(context, ToolsBarView.BAR_HEIGHT), (int) ScaleUtil.getDip(context, ToolsBarView.BAR_HEIGHT)));
		
		holder = new RelativeLayout(context);
		holder.setLayoutParams(new LayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)));
		addView(holder);
		
		this.label = new TextView(context);
		label.setTextSize(10);
		label.setTextColor(TEXT_COLOR);
		RelativeLayout.LayoutParams labelParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		labelParams.alignWithParent = true;
		labelParams.addRule(RelativeLayout.ALIGN_TOP);
		labelParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
		label.setLayoutParams(labelParams);
		holder.addView(label);
		
		image = new ImageView(this.getContext());
		image.setImageResource(R.drawable.dropdown_ic_arrow_normal_holo_light);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.alignWithParent = true;
		params.addRule(RelativeLayout.ALIGN_BOTTOM);
		params.addRule(RelativeLayout.ALIGN_RIGHT);
		image.setLayoutParams(params);
		holder.addView(image);
		
		buttons = new ArrayList<ToolBarButton>();
	}
	
	public void setLabel(String value) {
		label.setText(value);
	}
	
	public void setAnchorView(LinearLayout view) {
		this.anchor = view;
	}
	
	public void addButton(final ToolBarButton button) {
		buttons.add(button);
		button.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				showMenu(button);
				return true;
			}
			
		});
		if (selectedButton == null)
			selectedButton = button;
		update();
	}
	
	private void showMenu(ToolBarButton button) {
		update();
		
		popupLayout = new LinearLayout(this.getContext());
		popupLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		popupLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		popupLayout.setBackgroundColor(BAR_COLOR);
		popupLayout.setOrientation(LinearLayout.VERTICAL);
		for (ToolBarButton b : buttons) {
			if (selectedButton == b) continue;
			popupLayout.addView(b);
		}
		
		int w = button.getWidth();
		int h = button.getHeight();
		
		int count = 0;
		for(int i = 0; i < popupLayout.getChildCount(); i++){
			if (popupLayout.getChildAt(i).getVisibility() == View.VISIBLE) count++;
		}

		popupMenu = new PopupWindow(popupLayout, w, h * count, true);
		popupMenu.setOutsideTouchable(true);
		popupMenu.setTouchable(true);
		popupMenu.setBackgroundDrawable(new BitmapDrawable(getContext().getResources()));
		popupMenu.showAsDropDown(anchor, this.getLeft(), 0);
	}
	
	public void setSelectedButton(ToolBarButton button) {
		selectedButton = button;
		update();
	}
	
	private void update() {
		if (popupMenu != null) {
			popupMenu.dismiss();
			popupMenu = null;
		}
		
		if (popupLayout != null) {
			popupLayout.removeAllViews();
			popupLayout = null;
		}
		
		this.removeAllViews();
		
		if (selectedButton != null) {	
			this.addView(selectedButton);
			this.addView(holder);
		}
	}

	public List<ToolBarButton> getButtons() {
		return buttons;
	}
	
	public boolean isChecked() {
		return selectedButton != null ? selectedButton.isChecked() : false;
	}

	public void setChecked(boolean checked) {
		for (ToolBarButton button : buttons) {
			button.setChecked(false);
		}
		
		if (selectedButton != null) {
			selectedButton.setChecked(checked);
		}
		
		update();
	}

}
