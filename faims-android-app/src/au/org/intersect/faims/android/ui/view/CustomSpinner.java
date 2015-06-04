package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.managers.CSSManager;
import au.org.intersect.faims.android.util.Compare;

import com.google.inject.Inject;

public class CustomSpinner extends Spinner implements ICustomView {
	
	@Inject
	AutoSaveManager autoSaveManager;
	
	@Inject
	BeanShellLinker linker;
	
	@Inject
	CSSManager cssManager;

	private String ref;
	private boolean dynamic;
	protected String currentValue;
	protected float certainty;
	protected float currentCertainty;
	protected String annotation;
	protected String currentAnnotation;
	protected boolean dirty;
	protected String dirtyReason;
	protected boolean annotationEnabled;
	protected boolean certaintyEnabled;
	protected FormInputDef inputDef;

	private String selectCallback;
	private String focusCallback;
	private String blurCallback;
	
	private ImageView annotationIcon;
	private ImageView certaintyIcon;
	
	public CustomSpinner(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public CustomSpinner(Context context, FormInputDef inputDef, String ref, boolean dynamic) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		this.inputDef = inputDef;
		this.ref = ref;
		this.dynamic = dynamic;
		reset();
		cssManager.addCSS(this, "dropdown");
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
		NameValuePair pair = (NameValuePair) getSelectedItem();
		if (pair == null) return null;
		return pair.getValue();
	}

	@Override
	public void setValue(String value) {
		if (value != null) {
			for (int i = 0; i < getAdapter().getCount(); ++i) {
				NameValuePair pair = (NameValuePair) getItemAtPosition(i);
				if (value.equalsIgnoreCase(pair.getValue())) {
					setSelection(i);
					break;
				}
			}
		} else {
			setSelection(0);
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
	public boolean isDirty() {
		return dirty;
	}
	
	@Override
	public void setDirty(boolean value) {
		this.dirty = value;
	}
	
	@Override
	public void setDirtyReason(String value) {
		this.dirtyReason = value;
	}

	@Override
	public String getDirtyReason() {
		return dirtyReason;
	}
	
	@Override
	public void reset() {
		dirty = false;
		dirtyReason = null;
		setValue(null);
		setCertainty(1);
		setAnnotation("");
		save();
	}

	@Override
	public boolean hasChanges() {
		return !Compare.equal(getValue(), currentValue) || 
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
	
	public List<NameValuePair> getPairs() {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		if (getAdapter() != null) {
			for (int i = 0; i < getAdapter().getCount(); ++i) {
				NameValuePair pair = (NameValuePair) getItemAtPosition(i);
				if (pair.getValue() != null) {
					pairs.add(pair);
				}
			}
		}
		return pairs;
	}
	
	public void setPairs(List<NameValuePair> pairs) {
		populate(pairs);
	}
	
	public void populateWithNull(List<NameValuePair> pairs) {
		if (pairs == null) return;
		List<NameValuePair> newPairs = new ArrayList<NameValuePair>();
		newPairs.add(new NameValuePair("", null));
		newPairs.addAll(pairs);
		populate(newPairs);
	}
	
	public void populate(List<NameValuePair> pairs) {
		if (pairs == null) return;
		ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(
				getContext(),
				android.R.layout.simple_spinner_dropdown_item, pairs);
		setAdapter(arrayAdapter);
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
	
	protected void notifySave() {
		if (getAttributeName() != null && hasChanges()) {
			autoSaveManager.save();
		}
	}
	
	@Override
	public boolean hasAttributeChanges(
			HashMap<String, ArrayList<Attribute>> attributes) {
		return Compare.compareAttributeValue(this, attributes);
	}
	
	@Override
	public String getClickCallback() {
		return null;
	}

	public void setClickCallback(String code) {
		setSelectCallback(code);
	}

	@Override
	public String getSelectCallback() {
		return selectCallback;
	}

	@Override
	public void setSelectCallback(String code) {
		if (code == null) return;
		selectCallback = code;
		setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(
					AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				linker.execute(selectCallback);
			}

			@Override
			public void onNothingSelected(
					AdapterView<?> arg0) {
				linker.execute(selectCallback);
			}

		});
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
