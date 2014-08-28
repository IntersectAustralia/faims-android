package au.org.intersect.faims.android.ui.view;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.ui.dialog.FileAttachmentLabelDialog;

import com.google.inject.Inject;

public class FileListGroup extends CustomFileList {
	
	public static class FileTextItem extends TextView {

		private String value;
		
		public FileTextItem(Context context) {
			super(context);
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
		
	}
	
	@Inject
	AutoSaveManager autoSaveManager;
	
	public FileListGroup(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}

	public FileListGroup(Context context, FormInputDef attribute, boolean sync, String ref, boolean dynamic) {
		super(context, attribute, ref, dynamic, sync);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	@Override
	public void addFile(String filePath, String annotation, String certainty) {
		FrameLayout layout = createFileListItem(filePath, annotationEnabled, certaintyEnabled, this.getChildCount());
		addView(layout);
		this.invalidate();
		super.addFile(filePath, annotation, certainty);
	}
	
	private FrameLayout createFileListItem(String value, boolean annotation, boolean certainty, final int index) {
		FrameLayout layout = new FrameLayout(getContext());
		
		Button buttonOverlay = new Button(getContext());
		buttonOverlay.setBackgroundColor(Color.TRANSPARENT);
		buttonOverlay.setBackgroundResource(R.drawable.label_selector);
		layout.addView(buttonOverlay);

		LinearLayout innerLayout = new LinearLayout(getContext());
		
		FileTextItem textView = new FileTextItem(getContext());
		textView.setText(new File(value).getName());
		textView.setValue(value);
		LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		textParams.weight = 0.80F;
		innerLayout.addView(textView, textParams);
		if (annotation) {
			ImageView annotationImage = viewFactory.createAnnotationIcon();
			LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			innerLayout.addView(annotationImage, imageParams);			
		}
		if (certainty) {
			ImageView certaintyImage = viewFactory.createCertaintyIcon();
			LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			innerLayout.addView(certaintyImage, imageParams);			
		}
		layout.addView(innerLayout);
		final FileAttachmentLabelDialog labelDialog = createLabelDialog(index);
		buttonOverlay.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				labelDialog.show();
				return false;
			}
		});
		return layout;
	}
	
	private FileAttachmentLabelDialog createLabelDialog(int index) {
		FileAttachmentLabelDialog dialog = new FileAttachmentLabelDialog(getContext(), FileListGroup.this, index);
		if (annotationEnabled) {
			dialog.addAnnotationTab();
		}
		if (certaintyEnabled) {
			dialog.addCertaintyTab();
		}
		return dialog;
	}

	@Override
	protected void populate(List<NameValuePair> pairs) {
		if (pairs == null) return;
		removeAllViews();
		for (NameValuePair pair : pairs) {
			FrameLayout layout = createFileListItem(pair.getValue(), annotationEnabled, certaintyEnabled, pairs.indexOf(pair));
			addView(layout);
		}
	}
	
	@Override
	public List<?> getValues() {
		return getPairs();
	}
	
	private FileTextItem getItemTextView(FrameLayout frameLayout) {
		for (int i = 0; i < frameLayout.getChildCount(); ++i) {
			View view = frameLayout.getChildAt(i);
			if (view instanceof LinearLayout) {
				LinearLayout layout = (LinearLayout) view;
				for (int j = 0; j < layout.getChildCount(); ++j) {
					View layoutView = layout.getChildAt(j);
					if (layoutView instanceof FileTextItem) {
						return (FileTextItem) layoutView;
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public List<NameValuePair> getPairs() {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();

		for (int i = 0; i < getChildCount(); ++i) {
			View view = getChildAt(i);
			if (view instanceof FrameLayout) {
				FileTextItem item = getItemTextView((FrameLayout) view);
				if (item != null) {
					String fileText = item.getValue();
					pairs.add(new NameValuePair(fileText, fileText));
				}
			}
		}
		return pairs;
	}
	
	@Override
	public void reset() {
		dirty = false;
		dirtyReason = null;

		removeAllViews();
		
		annotations = null;
		certainties = null;
		save();
	}

}
