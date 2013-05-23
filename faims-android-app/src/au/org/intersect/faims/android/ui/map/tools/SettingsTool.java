package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.widget.LinearLayout;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

public abstract class SettingsTool extends MapTool {
	
	protected LinearLayout layout;
	
	protected MapButton settingsButton;
	
	public SettingsTool(Context context, CustomMapView mapView, String name) {
		super(context, mapView, name);
		
		layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		container.addView(layout);
		
		settingsButton = createSettingsButton(context);
		
		updateLayout();
	}
	
	protected void updateLayout() {
		if (layout != null) {
			layout.removeAllViews();
			layout.addView(settingsButton);
		}
	}
	
	protected abstract MapButton createSettingsButton(final Context context);

}
