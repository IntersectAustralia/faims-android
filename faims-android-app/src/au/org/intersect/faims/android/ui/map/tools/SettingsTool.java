package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.button.SettingsButton;
import au.org.intersect.faims.android.util.ScaleUtil;

public abstract class SettingsTool extends MapTool {
	
	protected RelativeLayout layout;
	
	protected SettingsButton settingsButton;
	
	protected ArrayList<View> buttons;
	
	protected static final int HEIGHT = 65;
	
	protected static final int TOP_MARGIN = 85;

	protected static final int BOTTOM_MARGIN = 85;
	
	public SettingsTool(Context context, CustomMapView mapView, String name) {
		super(context, mapView, name);
		
		layout = new RelativeLayout(context);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.leftMargin = (int) ScaleUtil.getDip(context, 10);
		params.rightMargin = (int) ScaleUtil.getDip(context, 10);
		layout.setLayoutParams(params);
		container.addView(layout);
		buttons = new ArrayList<View>();
		
		settingsButton = createSettingsButton(context);
		if(settingsButton != null){
			RelativeLayout.LayoutParams settingsParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			settingsParams.alignWithParent = true;
			settingsParams.addRule(RelativeLayout.ALIGN_LEFT);
			settingsParams.topMargin = (int) ScaleUtil.getDip(context, TOP_MARGIN);
			settingsButton.setLayoutParams(settingsParams);
			buttons.add(settingsButton);
		}
		updateLayout();
	}
	
	protected void updateLayout() {
		if (layout != null) {
			layout.removeAllViews();
			if(settingsButton != null){
				layout.addView(settingsButton);
			}
		}
	}

	protected abstract SettingsButton createSettingsButton(final Context context);

}
