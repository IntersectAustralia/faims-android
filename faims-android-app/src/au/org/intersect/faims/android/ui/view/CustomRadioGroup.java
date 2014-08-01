package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.util.Compare;

import com.google.inject.Inject;
import com.nativecss.NativeCSS;

public class CustomRadioGroup extends LinearLayout implements ICustomView {
	
	class RadioGroupOnChangeListener implements OnCheckedChangeListener {

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			if(listener != null) {
				listener.onCheckedChanged(group, checkedId);
			}
			notifySave();
		}

	}
	
	@Inject
	AutoSaveManager autoSaveManager;

	private String ref;
	private boolean dynamic;
	private String currentValue;
	private float certainty;
	private float currentCertainty;
	private String annotation;
	private String currentAnnotation;
	private boolean dirty;
	private String dirtyReason;
	private boolean annotationEnabled;
	private boolean certaintyEnabled;
	private FormInputDef inputDef;
	
	private OnCheckedChangeListener listener;
	private RadioGroupOnChangeListener customListener;

	public CustomRadioGroup(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		this.customListener = new RadioGroupOnChangeListener();
	}
	
	public CustomRadioGroup(Context context, FormInputDef inputDef, String ref, boolean dynamic) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		
		setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
		setOrientation(LinearLayout.VERTICAL);
		
		this.inputDef = inputDef;
		this.ref = ref;
		this.dynamic = dynamic;
		this.customListener = new RadioGroupOnChangeListener();
		NativeCSS.addCSSClass(this, "radio-group");
		reset();
	}

	@Override
	public String getAttributeName() {
		return inputDef.name;
	}

	@Override
	public String getAttributeType() {
		return inputDef.type;
	}

	@Override
	public String getRef() {
		return ref;
	}
	
	@Override
	public boolean isDynamic() {
		return dynamic;
	}
	
	@Override
	public String getValue() {
		if (getChildCount() == 0) return null;
		
		HorizontalScrollView horizontalScrollView = (HorizontalScrollView) getChildAt(0);
		RadioGroup rg = (RadioGroup) horizontalScrollView.getChildAt(0);
		for (int i = 0; i < rg.getChildCount(); ++i) {
			View view = rg.getChildAt(i);
			if (view instanceof CustomRadioButton) {
				CustomRadioButton rb = (CustomRadioButton) view;
				if (rb.isChecked()) {
					return rb.getValue();
				}
			}
		}
		return null;
	}

	@Override
	public void setValue(String value) {
		if (getChildCount() == 0) return;
		
		HorizontalScrollView horizontalScrollView = (HorizontalScrollView) getChildAt(0);
		RadioGroup rg = (RadioGroup) horizontalScrollView.getChildAt(0);
		List<CustomRadioButton> buttons = new ArrayList<CustomRadioButton>();
		for (int i = 0; i < rg.getChildCount(); ++i) {
			View view = rg.getChildAt(i);
			if (view instanceof CustomRadioButton) {
				buttons.add((CustomRadioButton) view);
			}
		}
		rg.removeAllViews();
		for (CustomRadioButton rb : buttons) {
			CustomRadioButton radioButton = new CustomRadioButton(
					rg.getContext());
			radioButton.setText(rb.getText());
			radioButton.setValue(rb.getValue());
			if (rb.getValue().toString()
					.equalsIgnoreCase(value)) {
				radioButton.setChecked(true);
			}
			rg.addView(radioButton);
		}
		notifySave();
	}

	@Override
	public float getCertainty() {
		return certainty;
	}

	@Override
	public void setCertainty(float certainty) {
		this.certainty = certainty;
		notifySave();
	}

	@Override
	public String getAnnotation() {
		return annotation;
	}

	@Override
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
		notifySave();
	}

	@Override
	public void reset() {
		dirty = false;
		dirtyReason = null;
		setValue("");
		setCertainty(1);
		setAnnotation("");
		save();
	}
	
	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	@Override
	public String getDirtyReason() {
		return dirtyReason;
	}

	@Override
	public void setDirtyReason(String reason) {
		this.dirtyReason = reason;
	}
	
	@Override
	public boolean hasChanges() {
		return !(Compare.equal(getValue(), currentValue)) || 
				!Compare.equal(getAnnotation(), currentAnnotation) || 
				!Compare.equal(getCertainty(), currentCertainty);
	}

	@Override
	public void save() {
		currentValue = getValue();
		currentCertainty = getCertainty();
		currentAnnotation = getAnnotation();
	}

	@Override
	public List<?> getValues() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setValues(List<?> values) {
		// TODO Auto-generated method stub
		
	}

	public void populate(List<NameValuePair> pairs) {
		removeAllViews();
		HorizontalScrollView scrollView = new HorizontalScrollView(this.getContext());
		addView(scrollView);
		RadioGroup rg = new RadioGroup(this.getContext());
		rg.setOrientation(LinearLayout.HORIZONTAL);
		rg.setOnCheckedChangeListener(customListener);
		scrollView.addView(rg);
		for (NameValuePair pair : pairs) {
			CustomRadioButton radioButton = new CustomRadioButton(this.getContext());
			radioButton.setText(pair.getName());
			radioButton.setValue(pair.getValue());
			rg.addView(radioButton);
		}
	}
	
	@Override
	public boolean getAnnotationEnabled() {
		return annotationEnabled;
	}

	@Override
	public void setAnnotationEnabled(boolean enabled) {
		annotationEnabled = enabled;
	}

	@Override
	public boolean getCertaintyEnabled() {
		return certaintyEnabled;
	}

	@Override
	public void setCertaintyEnabled(boolean enabled) {
		certaintyEnabled = enabled;
	}

	public void setOnCheckChangedListener(OnCheckedChangeListener l)
	{
		this.listener = l;
	}
	
	protected void notifySave() {
		if (getAttributeName() != null && hasChanges()) {
			autoSaveManager.save();
		}
	}

	@Override
	public boolean hasAttributeChanges(
			Collection<? extends Attribute> attributes) {
		return Compare.compareAttributeValue(this, attributes);
	}
}
