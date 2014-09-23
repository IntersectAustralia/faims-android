package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.util.Compare;

import com.google.inject.Inject;
import com.nativecss.NativeCSS;

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
	
	@Inject
	BeanShellLinker linker;
	
	private String ref;
	private boolean dynamic;
	
	protected List<NameValuePair> currentValues;
	protected float certainty;
	protected float currentCertainty;
	protected String annotation;
	protected String currentAnnotation;
	protected boolean dirty;
	protected String dirtyReason;
	protected boolean annotationEnabled;
	protected boolean certaintyEnabled;
	protected FormInputDef inputDef;

	protected OnClickListener listener;
	protected CheckBoxGroupOnClickListener customListener;

	private String clickCallback;
	private String focusCallback;
	private String blurCallback;
	
	private ImageView annotationIcon;
	private ImageView certaintyIcon;

	public CustomCheckBoxGroup(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		this.customListener = new CheckBoxGroupOnClickListener();
	}
	
	public CustomCheckBoxGroup(Context context, FormInputDef inputDef, String ref, boolean dynamic) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		
		setLayoutParams(new LayoutParams(
	                LayoutParams.MATCH_PARENT,
	                LayoutParams.MATCH_PARENT));
	    setOrientation(LinearLayout.VERTICAL);
	    
		this.inputDef = inputDef;
		this.ref = ref;
		this.dynamic = dynamic;		
	    this.customListener = new CheckBoxGroupOnClickListener();
	    NativeCSS.addCSSClass(this, "checkbox-group");
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
		updateCertaintyIcon(certainty);
		notifySave();
	}
	
	private void updateCertaintyIcon(float certainty) {
		if (certaintyIcon != null) {
			if (certainty != FaimsSettings.DEFAULT_CERTAINTY) {
				certaintyIcon.setImageResource(R.drawable.certainty_entered);
			} else {
				certaintyIcon.setImageResource(R.drawable.certainty);
			}
		}
	}
	
	@Override
	public void setCertaintyIcon(ImageView certaintyIcon) {
		this.certaintyIcon = certaintyIcon;
	}

	@Override
	public String getAnnotation() {
		return annotation;
	}

	@Override
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
		updateAnnotationIcon(annotation);
		notifySave();
	}
	
	private void updateAnnotationIcon(String annotation) {
		if (annotationIcon != null && annotation != null) {
			if (!FaimsSettings.DEFAULT_ANNOTATION.equals(annotation)) {
				annotationIcon.setImageResource(R.drawable.annotation_entered);
			} else {
				annotationIcon.setImageResource(R.drawable.annotation);
			}
		}
	}
	
	@Override
	public void setAnnotationIcon(ImageView annotationIcon) {
		this.annotationIcon = annotationIcon;
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
	@Override
	public boolean hasChanges() {
		return !(Compare.compareValues((List<NameValuePair>) getValues(), currentValues)) || 
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
		if (values == null) return;
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
		if (pairs == null) return;
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
		if (getAttributeName() != null && hasChanges()) {
			autoSaveManager.save();
		}
	}

	@Override
	public boolean hasAttributeChanges(
			HashMap<String, ArrayList<Attribute>>  attributes) {
		return Compare.compareAttributeValues(this, attributes);
	}

	@Override
	public String getClickCallback() {
		return clickCallback;
	}

	@Override
	public void setClickCallback(String code) {
		if (code == null) return;
		clickCallback = code;
		setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				linker.execute(clickCallback);
			}
		});
	}
	
	@Override
	public String getSelectCallback() {
		return null;
	}

	@Override
	public void setSelectCallback(String code) {
	}

	@Override
	public String getFocusCallback() {
		return focusCallback;
	}
	
	@Override
	public String getBlurCallback() {
		return blurCallback;
	}
	
	@Override
	public void setFocusBlurCallbacks(String focusCode, String blurCode) {
		if (focusCode == null && blurCode == null) return;
		focusCallback = focusCode;
		blurCallback = blurCode;
		setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					linker.execute(focusCallback);
				} else {
					linker.execute(blurCallback);
				}
			}
		});
	}

}
