package au.org.intersect.faims.android.ui.view;

import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.widget.TimePicker;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.util.Compare;
import au.org.intersect.faims.android.util.DateUtil;

import com.google.inject.Inject;

public class CustomTimePicker extends TimePicker implements ICustomView {

	private class CustomDatePickerOnDateChangedListener implements OnTimeChangedListener {

		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
			notifySave();
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
	
	private CustomDatePickerOnDateChangedListener customChangeListener;
	
	public CustomTimePicker(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public CustomTimePicker(Context context, FormAttribute attribute, String ref) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		this.attribute = attribute;
		this.ref = ref;
		reset();
		customChangeListener = new CustomDatePickerOnDateChangedListener();
		setOnTimeChangedListener(customChangeListener);
		DateUtil.setTimePicker(this);
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
		notifySave();
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
		notifySave();
	}

	public void reset() {
		dirty = false;
		dirtyReason = null;
		DateUtil.setTimePicker(this);
		setCertainty(1);
		setAnnotation("");
		save();
	}

	public boolean hasChanges() {
		return !Compare.equal(getValue(), currentValue) || 
				!Compare.equal(getCertainty(), currentCertainty) ||
				!Compare.equal(getAnnotation(), currentAnnotation);
	}
	
	@Override
	public void save() {
		currentValue = getValue();
		currentCertainty = getCertainty();
		currentAnnotation = getAnnotation();
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
