package au.org.intersect.faims.android.ui.view;

import java.util.List;

import android.content.Context;
import android.widget.Spinner;
import au.org.intersect.faims.android.data.FormAttribute;

public class CustomSpinner extends Spinner implements ICustomView {

	private String ref;
	private String currentValue;
	private float certainty;
	private float currentCertainty;
	private String annotation;
	private String currentAnnotation;
	protected boolean dirty;
	protected String dirtyReason;
	private boolean annotationEnabled;
	private boolean certaintyEnabled;
	private FormAttribute attribute;
	protected boolean ignoreSelectOnce;
	
	public CustomSpinner(Context context) {
		super(context);
	}
	
	public CustomSpinner(Context context, FormAttribute attribute, String ref) {
		super(context);
		this.attribute = attribute;
		this.ref = ref;
		reset();
	}
	
	public boolean ignoresSelectEvents() {
		return ignoreSelectOnce;
	}
	
	public void setIgnoreSelectEvents(boolean value) {
		ignoreSelectOnce = value;
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
		NameValuePair pair = (NameValuePair) getSelectedItem();
		if (pair == null) return null;
		return pair.getValue();
	}

	public void setValue(String value) {
		for (int i = 0; i < getAdapter().getCount(); ++i) {
			NameValuePair pair = (NameValuePair) getItemAtPosition(i);
			if (value.equalsIgnoreCase(pair.getValue())) {
				setSelection(i);
				break;
			}
		}
	}

	public float getCertainty() {
		return certainty;
	}

	public void setCertainty(float certainty) {
		this.certainty = certainty;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
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
	
	public void reset() {
		dirty = false;
		dirtyReason = null;
		setSelection(0);
		setCertainty(1);
		setAnnotation("");
		save();
	}

	public boolean hasChanges() {
		if (attribute.readOnly) return false;
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
}
