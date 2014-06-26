package au.org.intersect.faims.android.ui.view;

import java.util.List;

import android.content.Context;
import android.widget.DatePicker;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.util.Compare;
import au.org.intersect.faims.android.util.DateUtil;

import com.google.inject.Inject;

public class CustomDatePicker extends DatePicker implements ICustomView {
	
	private class CustomDatePickerOnDateChangedListener implements OnDateChangedListener {

		@Override
		public void onDateChanged(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			notifySave();
		}
		
	}
	
	@Inject
	AutoSaveManager autoSaveManager;

	private String ref;
	private String currentValue;
	private float certainty;
	private float currentCertainty;
	private boolean dirty;
	private String dirtyReason;
	private boolean annotationEnabled;
	private boolean certaintyEnabled;
	private FormAttribute attribute;

	private CustomDatePickerOnDateChangedListener customChangeListener;
	
	public CustomDatePicker(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public CustomDatePicker(Context context, FormAttribute attribute, String ref) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		this.attribute = attribute;
		this.ref = ref;
		reset();
		customChangeListener = new CustomDatePickerOnDateChangedListener();
		init(0, 0, 0, customChangeListener);
		DateUtil.setDatePicker(this);
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
		return DateUtil.getDate(this);
	}
	
	public void setValue(String value) {
		DateUtil.setDatePicker(this, value);
		notifySave();
	}

	public void reset() {
		dirty = false;
		dirtyReason = null;
		setValue(DateUtil.getCurrentTimestampGMT());
		setCertainty(1);
		save();
	}

	public boolean hasChanges() {
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
	
	protected void notifySave() {
		if (hasChanges()) {
			autoSaveManager.save();
		}
	}
	
}
