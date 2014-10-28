package au.org.intersect.faims.android.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.view.CustomCheckBoxGroup;

public class CheckBoxGroupLabelDialog extends LabelDialog {
	
	class FileLabelDialogClickListener implements OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (certaintySeekBar != null) {
				CheckBoxGroupLabelDialog.this.view.setCertainty(String.valueOf((float)certaintySeekBar.getProgress() / 100), index);
			}
			if (annotationText != null) {
				CheckBoxGroupLabelDialog.this.view.setAnnotation(annotationText.getText().toString(), index);
			}
		}
		
	}

	private CustomCheckBoxGroup view;
	int index;
	
	public CheckBoxGroupLabelDialog(Context context, CustomCheckBoxGroup view, int index) {
		super(context);
		
		this.view = view;
		this.index = index;
		
		setButton(DialogInterface.BUTTON_POSITIVE, getContext().getResources().getString(R.string.confirm_dialog_button), new FileLabelDialogClickListener());
	}
	
	public void show(int index) {
		this.index = index;
		show();
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
		if (index < view.getAllCertainties().size()) {
			if(view.getAllCertainties().get(index) != null) {
				updateCertainty(Float.valueOf(view.getAllCertainties().get(index)));
			} else {
				updateCertainty(1.0F);
			}
		}
	}
	
	@Override
	protected void updateAnnotation() {
		if (index < view.getAllAnnotations().size() && annotationText != null) {
			annotationText.setText(view.getAllAnnotations().get(index));
		}
	}

}
