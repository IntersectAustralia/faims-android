package au.org.intersect.faims.android.ui.view;

import java.util.Collection;

import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.Module;

public interface ICustomFileView {
	
	public boolean getSync();
	public boolean hasFileAttributeChanges(Module module, Collection<? extends Attribute> attributes);

}
