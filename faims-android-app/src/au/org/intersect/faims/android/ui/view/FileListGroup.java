package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.util.Compare;

import com.google.inject.Inject;

public class FileListGroup extends CustomCheckBoxGroup implements ICustomFileView {
	
	@Inject
	AutoSaveManager autoSaveManager;
	
	private boolean sync;

	private List<NameValuePair> reloadPairs;

	public FileListGroup(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}

	public FileListGroup(Context context, FormAttribute attribute, boolean sync, String ref) {
		super(context, attribute, ref);
		FAIMSApplication.getInstance().injectMembers(this);
		this.sync = sync;
	}
	
	public boolean getSync() {
		return this.sync;
	}
	
	@Override
	public void reset() {
		dirty = false;
		dirtyReason = null;
		
		removeAllViews();
		
		setCertainty(1);
		setAnnotation("");
		save();
	}

	public void addFile(String value) {
		CustomCheckBox checkBox = new CustomCheckBox(this.getContext());
		checkBox.setText(value);
		checkBox.setValue(value);
		checkBox.setChecked(true);
		checkBox.setOnClickListener(customListener);
		addView(checkBox);
		this.invalidate();
		autoSaveManager.save();
	}

	@Override
	public boolean hasFileAttributeChanges(Module module,
			Collection<? extends Attribute> attributes) {
		return Compare.compareFileAttributeValues(this, attributes, module);
	}
	
	@Override
	public void setReloadPairs(List<NameValuePair> pairs) {
		this.reloadPairs = pairs;
	}
	
	@Override
	public void reload() {
		if (reloadPairs == null) return;
		List<NameValuePair> pairs = getPairs();
		List<NameValuePair> newPairs = new ArrayList<NameValuePair>();
		List<String> values = new ArrayList<String>();
		for (NameValuePair p : pairs) {
			boolean addedPair = false;
			for (NameValuePair r : reloadPairs) {
				if (Compare.equal(p.getName(), r.getName())) {
					newPairs.add(new NameValuePair(r.getValue(), r.getValue()));
					values.add(r.getValue());
					addedPair = true;
					break;
				}
			}
			if (!addedPair) {
				newPairs.add(p);
			}
		}
		setPairs(newPairs);
		for (String value : values) {
			setValue(value);
		}
		reloadPairs = null;
	}

}
