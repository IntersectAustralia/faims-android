package au.org.intersect.faims.android.ui.map;

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
import au.org.intersect.faims.android.R;

public class ToolGroupButton extends RelativeLayout {

	private static final int BAR_COLOR = 0x88000000;
	public static final int BUTTON_SIZE = 65;
	public static final int OFFSET = 20;
	
	private ArrayList<ToolBarButton> buttons;
	private ToolBarButton selectedButton;
	private PopupWindow popupMenu;
	private LinearLayout popupLayout;
	private LinearLayout anchor;
	private ImageView dropDownIndicator;

	public ToolGroupButton(Context context) {
		super(context);
		setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		buttons = new ArrayList<ToolBarButton>();
		
		dropDownIndicator = new ImageView(this.getContext());
		dropDownIndicator.setImageResource(R.drawable.dropdown_ic_arrow_normal_holo_light);
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
		
		popupMenu = new PopupWindow(popupLayout, w, h * popupLayout.getChildCount(), true);
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
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.alignWithParent = true;
			params.addRule(RelativeLayout.ALIGN_BOTTOM);
			params.rightMargin = this.getRight() - this.getLeft();
			dropDownIndicator.setLayoutParams(params);
			this.addView(dropDownIndicator);
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
