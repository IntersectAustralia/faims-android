package au.org.intersect.faims.android.ui.view;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import au.org.intersect.faims.android.two.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.ui.dialog.FileGalleryPreviewDialog;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.google.inject.Inject;

public class VideoGallery extends FilePictureGallery {
	
	public static final String VIDEO_PREVIEW = "Video Preview";

	@Inject
	AutoSaveManager autoSaveManager;
	
	public VideoGallery(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public VideoGallery(Context context, FormInputDef attribute, boolean sync, String ref, boolean dynamic) {
		super(context, attribute, ref, dynamic, sync);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	@Override
	protected void setGalleryImage(CustomImageView gallery, String path) {
		if(path != null && new File(path).exists()) {
			if (path.contains(FileUtil.THUMBNAIL_EXT)) {
				super.setGalleryImage(gallery, path);
			} else {
				Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(
						path, MediaStore.Images.Thumbnails.MINI_KIND);
				gallery.setImageBitmap(thumbnail);
			}
		}
	}
	
	@Override
	protected void longSelectImage(View v) {
		previewVideo(v);
	}
	
	private void previewVideo(View v) {
		final CustomImageView selectedImageView = (CustomImageView) v;
		String path = selectedImageView.getPicture().getUrl();
		File file = new File(path);
		
		final FileGalleryPreviewDialog dialog = new FileGalleryPreviewDialog(this.getContext());
		
		ArrayList<NameValuePair> metadata = new ArrayList<NameValuePair>();
		metadata.add(new NameValuePair("File name", file.getName()));
		
		if (file.exists()) {
			VideoView videoView = new VideoView(this.getContext());
			videoView.setVideoPath(path);
			videoView.setMediaController(new MediaController(this.getContext()));
			videoView.requestFocus();
			videoView.start();
			
			dialog.addVideoPreview(videoView);
			
			metadata.add(new NameValuePair("File size", file.length() + "bytes"));
			Date lastModifiedDate = new Date(file.lastModified());
			metadata.add(new NameValuePair("Picture date", lastModifiedDate.toString()));
			
			MediaPlayer player = new MediaPlayer();
			try {
				player.setDataSource(path);
				player.prepare();
				long duration = player.getDuration();
				metadata.add(new NameValuePair("Video duration", duration / 1000 + " seconds"));
				player.release();
			} catch (Exception e) {
				FLog.e("error obtaining video file duration", e);
			}
		} else {
			
			LinearLayout layout = new LinearLayout(getContext());
			layout.setBackgroundColor(getResources().getColor(R.color.color_grey));
			
			TextView warning = new TextView(getContext());
			warning.setText(linker.getPreviewText(VIDEO_PREVIEW, file));
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
					FaimsSettings.DEFAULT_ANNOTATION : getAnnotations().get(galleryImages.indexOf(selectedImageView)));
			metadata.add(new NameValuePair("Annotation", annotation));
		}
		if (certaintyEnabled) {
			String certainty = (getCertainties().get(galleryImages.indexOf(selectedImageView)) == null ?
					String.valueOf(FaimsSettings.DEFAULT_CERTAINTY) : getCertainties().get(galleryImages.indexOf(selectedImageView)));
			metadata.add(new NameValuePair("Certainty", certainty));
		}
		dialog.addFileMetadataTab(metadata);
		
		dialog.addActionsTab("Remove Video", new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				removeGalleryItem(selectedImageView);
				dialog.dismiss();
			}
		});
		
		dialog.show();
	}
}
