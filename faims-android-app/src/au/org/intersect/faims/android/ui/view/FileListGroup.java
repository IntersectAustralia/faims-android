package au.org.intersect.faims.android.ui.view;

import android.content.Context;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.managers.AutoSaveManager;

import com.google.inject.Inject;

public class FileListGroup extends CustomCheckBoxGroup implements ICustomFileView {
	
	@Inject
	AutoSaveManager autoSaveManager;
	
	private boolean sync;

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

}
