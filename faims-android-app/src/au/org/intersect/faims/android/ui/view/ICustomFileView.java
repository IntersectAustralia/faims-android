package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.data.NameValuePair;

public interface ICustomFileView {
	
	public boolean getSync();
	public boolean hasFileAttributeChanges(Module module, HashMap<String, ArrayList<Attribute>>  attributes);
	public void reload();
	public void setReloadPairs(List<NameValuePair> pairs);

}
