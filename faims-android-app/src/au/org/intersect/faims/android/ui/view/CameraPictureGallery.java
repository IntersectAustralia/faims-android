package au.org.intersect.faims.android.ui.view;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.constants.PictureConstants;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.managers.BitmapManager;
import au.org.intersect.faims.android.ui.dialog.FileGalleryPreviewDialog;
import au.org.intersect.faims.android.util.BitmapUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.google.inject.Inject;

public class CameraPictureGallery extends FilePictureGallery {
	
	public static final String IMAGE_PREVIEW = "Image Preview";

	@Inject
	AutoSaveManager autoSaveManager;

	@Inject
	BeanShellLinker linker;
	
	@Inject
	BitmapManager bitmapManager;
	
	public CameraPictureGallery(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public CameraPictureGallery(Context context, FormInputDef attribute, boolean sync, String ref, boolean dynamic) {
		super(context, attribute, ref, dynamic, sync);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	@Override
	protected void longSelectImage(View v) {
		previewCameraPicture(v);
	}
	
	private void previewCameraPicture(View v) {
		final CustomImageView selectedImageView = (CustomImageView) v;
		String path = selectedImageView.getPicture().getUrl();
		File file = new File(path);
		
		final FileGalleryPreviewDialog dialog = new FileGalleryPreviewDialog(this.getContext());
		
		ArrayList<NameValuePair> metadata = new ArrayList<NameValuePair>();
		metadata.add(new NameValuePair("File name", getGalleryFileName(selectedImageView)));
		
		if (file.exists()) {
			ImageView imageView = new ImageView(getContext());
			
			// add bitmap to manager
			Bitmap bitmap = BitmapUtil.decodeFile(new File(path), PictureConstants.PREVIEW_IMAGE_SIZE, PictureConstants.PREVIEW_IMAGE_SIZE);
			imageView.setImageBitmap(bitmap);
			
			dialog.addCameraPreview(imageView);
			
			metadata.add(new NameValuePair("File size", file.length() + "bytes"));
			Date lastModifiedDate = new Date(file.lastModified());
			metadata.add(new NameValuePair("Picture date", lastModifiedDate.toString()));
		} else {
			
			LinearLayout layout = new LinearLayout(getContext());
			layout.setBackgroundColor(getResources().getColor(R.color.color_grey));
			
			TextView warning = new TextView(getContext());
			warning.setText(linker.getPreviewText(IMAGE_PREVIEW, file));
			int textPadding = (int) ScaleUtil.getDip(getContext(), 10);
			warning.setTextSize(textPadding);
			int padding = (int) ScaleUtil.getDip(getContext(), 50);
			warning.setPadding(textPadding, padding, textPadding, padding);
			warning.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL);
			layout.addView(warning);

			dialog.addCameraPreview(layout);
		}
		
		if (annotationEnabled) {
			String annotation = (getAnnotations().get(galleryImages.indexOf(selectedImageView)) == null ?
					"": getAnnotations().get(galleryImages.indexOf(selectedImageView)));
			metadata.add(new NameValuePair("Annotation", annotation));
		}
		if (certaintyEnabled) {
			String certainty = (getCertainties().get(galleryImages.indexOf(selectedImageView)) == null ?
					"" : getCertainties().get(galleryImages.indexOf(selectedImageView)));
			metadata.add(new NameValuePair("Certainty", certainty));
		}
		dialog.addFileMetadataTab(metadata);
		
		dialog.addActionsTab("Remove Image", new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				removeGalleryItem(selectedImageView);
				dialog.dismiss();
			}
		});
		
		dialog.show();
	}
}
