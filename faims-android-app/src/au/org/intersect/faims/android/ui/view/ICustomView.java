package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.widget.ImageView;
import au.org.intersect.faims.android.data.Attribute;

public interface ICustomView extends IView {

	public String getAttributeName();
	public String getAttributeType();
	public String getValue();
	public void setValue(String value);
	public float getCertainty();
	public void setCertainty(float certainty);
	public String getAnnotation();
	public void setAnnotation(String annotation);
	public boolean hasChanges();
	public boolean hasAttributeChanges(HashMap<String, ArrayList<Attribute>>  attributes);
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
	public void setAnnotationIcon(ImageView annotationIcon);
	public void setCertaintyIcon(ImageView certaintyIcon);
}
