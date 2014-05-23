package au.org.intersect.faims.android.ui.view;

import java.util.List;

public interface ICustomView {

	public String getAttributeName();
	public String getAttributeType();
	public String getRef();
	public String getValue();
	public void setValue(String value);
	public float getCertainty();
	public void setCertainty(float certainty);
	public String getAnnotation();
	public void setAnnotation(String annotation);
	public boolean hasChanges();
	public void reset();
	public boolean isDirty();
	public void setDirty(boolean dirty);
	public String getDirtyReason();
	public void setDirtyReason(String reason);
	public void save();
	public List<?> getValues();
	public void setValues(List<?> values);
	public boolean getAnnotationEnabled();
	public void setAnnotationEnabled(boolean enabled);
	public boolean getCertaintyEnabled();
	public void setCertaintyEnabled(boolean enabled);
	
}
