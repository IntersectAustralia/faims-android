package au.org.intersect.faims.android.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.view.CustomFileList;

public class FileAttachmentLabelDialog extends LabelDialog {
	
	class FileLabelDialogClickListener implements OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (certaintySeekBar != null) {
				FileAttachmentLabelDialog.this.view.setCertainty(String.valueOf((float)certaintySeekBar.getProgress() / 100), index);
			}
			if (annotationText != null) {
				FileAttachmentLabelDialog.this.view.setAnnotation(annotationText.getText().toString(), index);
			}
		}
		
	}

	private CustomFileList view;
	int index;
	
	public FileAttachmentLabelDialog(Context context, CustomFileList view, int index) {
		super(context);
		
		this.view = view;
		this.index = index;
		
		setButton(DialogInterface.BUTTON_POSITIVE, getContext().getResources().getString(R.string.confirm_dialog_button), new FileLabelDialogClickListener());
	}
	
	@Override
	public void show() {
		if (tabs.size() == 0) {
			return;
		}
		updateCertainty();
		updateAnnotation();
		super.show();
	}
	
	@Override
	protected void updateCertainty() {
		if (index < view.getCertainties().size()) {
			if(view.getCertainties().get(index) != null) {
				updateCertainty(Float.valueOf(view.getCertainties().get(index)));
			} else {
				updateCertainty(1.0F);
			}
		}
	}
	
	@Override
	protected void updateAnnotation() {
		if (index < view.getAnnotations().size() && annotationText != null) {
			annotationText.setText(view.getAnnotations().get(index));
		}
	}

	
}
