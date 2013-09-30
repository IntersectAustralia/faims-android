package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import au.org.intersect.faims.android.data.FormAttribute;

public class FileListGroup extends CustomCheckBoxGroup implements ICustomFileView {
	
	private boolean sync;

	public FileListGroup(Context context) {
		super(context);
	}

	public FileListGroup(Context context, FormAttribute attribute, boolean sync, String ref) {
		super(context, attribute, ref);
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
		addView(checkBox);
		this.invalidate();
	}

}
