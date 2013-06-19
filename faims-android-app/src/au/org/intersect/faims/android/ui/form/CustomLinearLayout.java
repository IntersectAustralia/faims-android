package au.org.intersect.faims.android.ui.form;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.widget.LinearLayout;
import au.org.intersect.faims.android.ui.form.styling.FaimsStyling;
import au.org.intersect.faims.android.ui.form.styling.StyleUtils;

public class CustomLinearLayout extends LinearLayout implements FaimsStyling{
	
	private String attributeName;
	private String attributeType;
	private String ref;
	private float certainty = 1;
	private float currentCertainty = 1;
	private String annotation = "";
	private String currentAnnotation = "";
	private boolean dirty;
	private String dirtyReason;
	
	public CustomLinearLayout(Context context) {
		super(context);
	}
	
	public CustomLinearLayout(Context context, List<Map<String, String>> styleMappings) {
		super(context);
		this.setOrientation(LinearLayout.VERTICAL);
		applyStyle(styleMappings);
	}

	public CustomLinearLayout(Context context, String attributeName, String attributeType, String ref) {
		super(context);
		this.attributeName = attributeName;
		this.attributeType = attributeType;
		this.ref = ref;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public String getAttributeType() {
		return attributeType;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public float getCertainty() {
		return certainty;
	}

	public void setCertainty(float certainty) {
		this.certainty = certainty;
	}

	public float getCurrentCertainty() {
		return currentCertainty;
	}

	public void setCurrentCertainty(float currentCertainty) {
		this.currentCertainty = currentCertainty;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public String getCurrentAnnotation() {
		return currentAnnotation;
	}

	public void setCurrentAnnotation(String currentAnnotation) {
		this.currentAnnotation = currentAnnotation;
	}

	@Override
	public void applyStyle(List<Map<String, String>> styleMappings) {
		if(!styleMappings.isEmpty()){
			LayoutParams layoutParams = this.getLayoutParams() != null ? (LayoutParams) this.getLayoutParams() : new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
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

	public boolean isDirty() {
		return dirty;
	}
	
	public void setDirty(boolean value) {
		this.dirty = value;
	}
	
	public void setDirtyReason(String value) {
		this.dirtyReason = value;
	}

	public String getDirtyReason() {
		return dirtyReason;
	}
}
