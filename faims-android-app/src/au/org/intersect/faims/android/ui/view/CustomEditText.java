package au.org.intersect.faims.android.ui.view;

import java.util.List;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.util.Compare;

import com.google.inject.Inject;

public class CustomEditText extends EditText implements ICustomView {
	
	private class CustomEditTextTextWatcher implements TextWatcher {

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			notifySave();
		}

		@Override
		public void afterTextChanged(Editable s) {
		}
		
	}
	
	@Inject
	AutoSaveManager autoSaveManager;

	private String ref;
	private String currentValue;
	private float certainty;
	private float currentCertainty;
	private String annotation;
	private String currentAnnotation;
	private boolean dirty;
	private String dirtyReason;
	private boolean annotationEnabled;
	private boolean certaintyEnabled;
	private FormAttribute attribute;
	
	private CustomEditTextTextWatcher customTextWatcher;
	
	public CustomEditText(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}

	public CustomEditText(Context context, FormAttribute attribute, String ref) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		this.attribute = attribute;
		this.ref = ref;
		reset();
		customTextWatcher = new CustomEditTextTextWatcher();
		addTextChangedListener(customTextWatcher);
	}

	public String getAttributeName() {
		return attribute.name;
	}

	public String getAttributeType() {
		return attribute.type;
	}

	public String getRef() {
		return ref;
	}
	
	public String getValue() {
		return getText().toString();
	}
	
	public void setValue(String value) {
		setText(value);
		notifySave();
	}

	public float getCertainty() {
		return certainty;
	}

	public void setCertainty(float certainty) {
		this.certainty = certainty;
		notifySave();
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
		notifySave();
	}
	
	public boolean isDirty() {
		return dirty;
	}
	
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	
	public String getDirtyReason() {
		return dirtyReason;
	}
	
	public void setDirtyReason(String reason) {
		this.dirtyReason = reason;
	}

	public void reset() {
		dirty = false;
		dirtyReason = null;
		setValue("");
		setCertainty(1);
		setAnnotation("");
		save();
	}

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
		if (hasChanges()) {
			autoSaveManager.save();
		}
	}
	
}
