package au.org.intersect.faims.android.ui.view;

import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.widget.DatePicker;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.util.Compare;
import au.org.intersect.faims.android.util.DateUtil;

import com.google.inject.Inject;
import com.nativecss.NativeCSS;

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
	private FormInputDef attribute;

	private CustomDatePickerOnDateChangedListener customChangeListener;
	
	public CustomDatePicker(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public CustomDatePicker(Context context, FormInputDef attribute, String ref, boolean dynamic) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		this.attribute = attribute;
		this.ref = ref;
		this.dynamic = dynamic;
		reset();
		customChangeListener = new CustomDatePickerOnDateChangedListener();
		init(0, 0, 0, customChangeListener);
		DateUtil.setDatePicker(this);
		NativeCSS.addCSSClass(this, "date-picker");
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
	public boolean isDynamic() {
		return dynamic;
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
	public String getValue() {
		return DateUtil.getDate(this);
	}
	
	@Override
	public void setValue(String value) {
		DateUtil.setDatePicker(this, value);
		notifySave();
	}

	@Override
	public void reset() {
		dirty = false;
		dirtyReason = null;
		DateUtil.setDatePicker(this);
		setCertainty(1);
		setAnnotation("");
		save();
	}

	@Override
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
