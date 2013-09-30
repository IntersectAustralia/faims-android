package au.org.intersect.faims.android.ui.form;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.log.FLog;

public class VideoGallery extends PictureGallery implements ICustomFileView {

	private boolean sync;

	public VideoGallery(Context context) {
		super(context);
	}
	
	public VideoGallery(Context context, String ref, FormAttribute attribute) {
		super(context, ref, attribute, true);
		this.sync = attribute.sync;
	}
	
	public boolean getSync() {
		return sync;
	}
	
	@Override
	protected void setGalleryImage(CustomImageView gallery, String path) {
		Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(
				path, MediaStore.Images.Thumbnails.MINI_KIND);
		gallery.setImageBitmap(thumbnail);
	}
	
	@Override
	protected void longSelectImage(View v) {
		previewVideo(v);
	}
	
	private void previewVideo(View v) {
		final CustomImageView selectedImageView = (CustomImageView) v;
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());

		builder.setTitle("Video Preview");

		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		layout.setOrientation(LinearLayout.VERTICAL);

		builder.setView(layout);
		VideoView videoView = new VideoView(this.getContext());
		videoView.setVideoPath(selectedImageView.getPicture().getUrl());
		videoView.setMediaController(new MediaController(this.getContext()));
		videoView.requestFocus();
		videoView.start();
		layout.addView(videoView, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));

		builder.setNegativeButton("Done",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub

					}
				});
		builder.setPositiveButton("View Metadata",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						AlertDialog.Builder builder = new AlertDialog.Builder(
								VideoGallery.this.getContext());

						builder.setTitle("Video Preview");

						LinearLayout layout = new LinearLayout(VideoGallery.this.getContext());
						layout.setLayoutParams(new LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.MATCH_PARENT));
						layout.setOrientation(LinearLayout.VERTICAL);

						builder.setView(layout);
						TextView text = new TextView(VideoGallery.this.getContext());
						text.setText(getVideoMetaData(selectedImageView.getPicture().getUrl()));
						layout.addView(text);
						builder.setPositiveButton("Done",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// TODO Auto-generated method stub

									}
								});
						builder.create().show();

					}

				});
		builder.create().show();
	}

	private String getVideoMetaData(String path) {
		File videoFile = new File(path);

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Video Metadata:");
		stringBuilder.append("\n");
		stringBuilder.append("File name: " + videoFile.getName());
		stringBuilder.append("\n");
		stringBuilder.append("File size: " + videoFile.length() + " bytes");
		stringBuilder.append("\n");
		Date lastModifiedDate = new Date(videoFile.lastModified());
		stringBuilder.append("Video date: " + lastModifiedDate.toString());
		MediaPlayer player = new MediaPlayer();
		try {
			player.setDataSource(path);
			player.prepare();
			long duration = player.getDuration();
			stringBuilder.append("\n");
			stringBuilder.append("Video duration: " + duration / 1000
					+ " seconds");
			player.release();
		} catch (Exception e) {
			FLog.e("error obtaining video file duration", e);
		}
		return stringBuilder.toString();
	}

	@Override
	public void reset() {
		removeSelectedImages();
		galleriesLayout.removeAllViews();
		galleryImages = new ArrayList<CustomImageView>();
		
		setCertainty(1);
		setAnnotation("");
		save();
	}

	public void addVideo(String value) {
		Picture picture = new Picture(value, null, value);
		addSelectedImage(addGallery(picture));
	}

}
