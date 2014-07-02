package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.util.Compare;

import com.google.inject.Inject;

public class CustomCheckBoxGroup extends LinearLayout implements ICustomView {
	
	class CheckBoxGroupOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			if(listener != null) {
				listener.onClick(v);
			}
			notifySave();
		}
		
	}
	
	@Inject
	AutoSaveManager autoSaveManager;
	
	private String ref;
	private List<NameValuePair> currentValues;
	private float certainty;
	private float currentCertainty;
	private String annotation;
	private String currentAnnotation;
	protected boolean dirty;
	protected String dirtyReason;
	private boolean annotationEnabled;
	private boolean certaintyEnabled;
	private FormAttribute attribute;

	private OnClickListener listener;
	private CheckBoxGroupOnClickListener customListener;

	public CustomCheckBoxGroup(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		this.customListener = new CheckBoxGroupOnClickListener();
	}
	
	public CustomCheckBoxGroup(Context context, FormAttribute attribute, String ref) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		
		setLayoutParams(new LayoutParams(
	                LayoutParams.MATCH_PARENT,
	                LayoutParams.MATCH_PARENT));
	    setOrientation(LinearLayout.VERTICAL);
		
		this.attribute = attribute;
		this.ref = ref;
		reset();
	    this.customListener = new CheckBoxGroupOnClickListener();
	}

	@Override
	public String getAttributeName() {
		return attribute.name;
	}

	@Override
	public String getAttributeType() {
		return attribute.type;
	}

	@Override
	public String getRef() {
		return ref;
	}
	
	@Override
	public String getValue() {
		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);

			if (view instanceof CustomCheckBox) {
				CustomCheckBox cb = (CustomCheckBox) view;
				if (cb.isChecked()) {
					return cb.getValue();
				}
			}
		}
		return null;
	}

	@Override
	public void setValue(String value) {
		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);
			if (view instanceof CustomCheckBox) {
				CustomCheckBox cb = (CustomCheckBox) view;
				if (cb.getValue()
						.toString()
						.equalsIgnoreCase(value)) {
					cb.setChecked(true);
					break;
				}
			}
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
		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);
			if (view instanceof CustomCheckBox) {
				CustomCheckBox cb = (CustomCheckBox) view;
				cb.setChecked(false);
			}
		}
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
	
	@SuppressWarnings("unchecked")
	private boolean compareValues() {
		List<NameValuePair> values = (List<NameValuePair>) getValues();
		if (values == null && currentValues == null) return true;
		if (values == null && currentValues != null) return false;
		if (values != null && currentValues == null) return false;
		if (values.size() != currentValues.size()) return false;
			
		for (int i = 0; i < values.size(); i++) {
			boolean hasValue = false;
			for (int j = 0; j < currentValues.size(); j++) {
				if (values.get(i).equals(currentValues.get(j))) {
					hasValue = true;
					break;
				}
			}
			if (!hasValue) return false;
		}
		
		return true;
	}
	
	@Override
	public boolean hasChanges() {
		return !(compareValues()) || 
				!Compare.equal(getAnnotation(), currentAnnotation) || 
				!Compare.equal(getCertainty(), currentCertainty);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void save() {
		currentValues = (List<NameValuePair>) getValues();
		currentCertainty = getCertainty();
		currentAnnotation = getAnnotation();
	}

	@Override
	public List<?> getValues() {
		List<NameValuePair> values = new ArrayList<NameValuePair>();

		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);

			if (view instanceof CustomCheckBox) {
				CustomCheckBox cb = (CustomCheckBox) view;
				if (cb.isChecked()) {
					values.add(new NameValuePair(cb.getValue(), "true"));
				}
			}
		}
		
		return values;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setValues(List<?> values) {
		List<NameValuePair> pairs = (List<NameValuePair>) values;
		for (NameValuePair pair : pairs) {
			for (int i = 0; i < getChildCount(); ++i) {
				View view = getChildAt(i);
				if (view instanceof CustomCheckBox) {
					CustomCheckBox cb = (CustomCheckBox) view;
					if (cb.getValue()
							.toString()
							.equalsIgnoreCase(pair.getName())) {
						cb.setChecked("true".equals(pair.getValue()));
						break;
					}
				}
			}
		}
		notifySave();
	}
	
	public List<NameValuePair> getPairs() {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();

		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);

			if (view instanceof CustomCheckBox) {
				CustomCheckBox cb = (CustomCheckBox) view;
				String name = cb.getText().toString();
				String value = cb.getValue();
				pairs.add(new NameValuePair(name, value));
			}
		}
		
		return pairs;
	}
	
	public void setPairs(List<NameValuePair> pairs) {
		populate(pairs);
	}

	public void populate(List<NameValuePair> pairs) {
		removeAllViews();
		for (NameValuePair pair : pairs) {
			CustomCheckBox checkBox = new CustomCheckBox(this.getContext());
			checkBox.setText(pair.getName());
			checkBox.setValue(pair.getValue());
			checkBox.setOnClickListener(customListener);
			addView(checkBox);
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
	
	@Override
	public void setOnClickListener(OnClickListener l)
	{
		this.listener = l;
	}
	
	protected void notifySave() {
		if (hasChanges()) {
			autoSaveManager.save();
		}
	}

}
