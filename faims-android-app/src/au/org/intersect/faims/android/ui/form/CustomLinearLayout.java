package au.org.intersect.faims.android.ui.form;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.widget.LinearLayout;
import au.org.intersect.faims.android.ui.form.styling.FaimsStyling;
import au.org.intersect.faims.android.ui.form.styling.StyleUtils;

public class CustomLinearLayout extends LinearLayout implements FaimsStyling {
	
	public CustomLinearLayout(Context context) {
		super(context);
	}
	
	public CustomLinearLayout(Context context, List<Map<String, String>> styleMappings) {
		super(context);
		this.setOrientation(LinearLayout.VERTICAL);
		applyStyle(styleMappings);
	}

	@Override
	public void applyStyle(List<Map<String, String>> styleMappings) {
		if(!styleMappings.isEmpty()){
			LayoutParams layoutParams = this.getLayoutParams() != null ? (LayoutParams) this.getLayoutParams() : new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			for (Map<String, String> styles : styleMappings) {
				if (!styles.isEmpty()) {
					for (Entry<String, String> attribute : styles.entrySet()) {
						if ("orientation".equals(attribute.getKey())) {
							if ("vertical".equals(attribute.getValue())) {
								this.setOrientation(LinearLayout.VERTICAL);
							} else if ("horizontal".equals(attribute.getValue())) {
								this.setOrientation(LinearLayout.HORIZONTAL);
							}
						} else if ("layout_width".equals(attribute.getKey())) {
							layoutParams.width = StyleUtils.getLayoutParamsValue(attribute.getValue());
						} else if ("layout_height".equals(attribute.getKey())) {
							layoutParams.height = StyleUtils.getLayoutParamsValue(attribute.getValue());
						} else if ("layout_weight".equals(attribute.getKey())) {
							layoutParams.weight = StyleUtils.getLayoutParamsValue(attribute.getValue());
						}
					}
					this.setLayoutParams(layoutParams);
				}
			}
		}
		
	}
	
}
