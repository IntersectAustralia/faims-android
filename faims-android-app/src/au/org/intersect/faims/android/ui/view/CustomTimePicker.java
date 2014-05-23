package au.org.intersect.faims.android.ui.view;

import java.util.List;

import android.content.Context;
import android.text.format.Time;
import android.widget.TimePicker;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.util.DateUtil;

public class CustomTimePicker extends TimePicker implements ICustomView {

	private String ref;
	private String currentValue;
	private float certainty;
	private float currentCertainty;
	private boolean dirty;
	private String dirtyReason;
	private boolean annotationEnabled;
	private boolean certaintyEnabled;
	private FormAttribute attribute;
	
	public CustomTimePicker(Context context) {
		super(context);
	}
	
	public CustomTimePicker(Context context, FormAttribute attribute, String ref) {
		super(context);
		this.attribute = attribute;
		this.ref = ref;
		reset();
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

	public float getCertainty() {
		return certainty;
	}

	public void setCertainty(float certainty) {
		this.certainty = certainty;
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
	
	public String getValue() {
		return DateUtil.getTime(this);
	}
	
	public void setValue(String value) {
		DateUtil.setTimePicker(this, value);
	}

	public void reset() {
		dirty = false;
		dirtyReason = null;
		Time now = new Time();
		now.setToNow();
		setCurrentHour(now.hour);
		setCurrentMinute(now.minute);
		setCertainty(1);
		save();
	}

	public boolean hasChanges() {
		if (attribute.readOnly) return false;
		return !Compare.equal(getValue(), currentValue) || 
				!Compare.equal(getCertainty(), currentCertainty);
	}
	
	@Override
	public void save() {
		currentValue = getValue();
		currentCertainty = getCertainty();
	}

	@Override
	public String getAnnotation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAnnotation(String annotation) {
		// TODO Auto-generated method stub
		
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
