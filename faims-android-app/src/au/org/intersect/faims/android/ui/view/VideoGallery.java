package au.org.intersect.faims.android.ui.view;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.Module;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.util.Compare;

import com.google.inject.Inject;

public class VideoGallery extends PictureGallery implements ICustomFileView {

	@Inject
	AutoSaveManager autoSaveManager;
	
	private boolean sync;

	private List<NameValuePair> reloadPairs;

	public VideoGallery(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public VideoGallery(Context context, FormInputDef attribute, String ref, boolean dynamic) {
		super(context, attribute, ref, dynamic, true);
		FAIMSApplication.getInstance().injectMembers(this);
		this.sync = attribute.sync;
	}
	
	public boolean getSync() {
		return sync;
	}
	
	@Override
	protected void setGalleryImage(CustomImageView gallery, String path) {
		if(path != null && new File(path).exists()) {
			Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(
					path, MediaStore.Images.Thumbnails.MINI_KIND);
			gallery.setImageBitmap(thumbnail);
		}
	}
	
	@Override
	protected void longSelectImage(View v) {
		previewVideo(v);
	}
	
	private void previewVideo(View v) {
		final CustomImageView selectedImageView = (CustomImageView) v;
		String path = selectedImageView.getPicture().getUrl();
		if (!new File(path).exists()) return;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());

		builder.setTitle("Video Preview");

		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		layout.setOrientation(LinearLayout.VERTICAL);

		builder.setView(layout);
		VideoView videoView = new VideoView(this.getContext());
		videoView.setVideoPath(path);
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
		dirty = false;
		dirtyReason = null;
		
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
		notifySave();
	}
	
	@Override
	public boolean hasAttributeChanges(
			Collection<? extends Attribute> attributes) {
		return Compare.compareAttributeValues(this, attributes);
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
