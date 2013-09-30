package au.org.intersect.faims.android.ui.form;

import android.content.Context;

public class FileListGroup extends CustomCheckBoxGroup implements ICustomFileView {
	
	private boolean sync;

	public FileListGroup(Context context) {
		super(context);
	}

	public FileListGroup(Context context, String attributeName,
			String attributeType, boolean sync, String ref) {
		super(context, attributeName, attributeType, ref);
		this.sync = sync;
	}
	
	public boolean getSync() {
		return this.sync;
	}
	
	@Override
	public void reset() {
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
	}

}
