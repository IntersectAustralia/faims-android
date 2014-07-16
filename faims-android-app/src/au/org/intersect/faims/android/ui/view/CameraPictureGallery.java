package au.org.intersect.faims.android.ui.view;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.util.Compare;

import com.google.inject.Inject;

public class CameraPictureGallery extends PictureGallery implements ICustomFileView {

	@Inject
	AutoSaveManager autoSaveManager;
	
	private boolean sync;

	public CameraPictureGallery(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public CameraPictureGallery(Context context, FormAttribute attribute, String ref) {
		super(context, attribute, ref, true);
		FAIMSApplication.getInstance().injectMembers(this);
		this.sync = attribute.sync;
	}
	
	public boolean getSync() {
		return sync;
	}
	
	@Override
	protected void setGalleryImage(CustomImageView gallery, String path) {
		if(path != null && new File(path).exists()) {
			gallery.setImageBitmap(decodeFile(new File(path), GALLERY_SIZE, GALLERY_SIZE));
		}
	}
	
	@Override
	protected void longSelectImage(View v) {
		previewCameraPicture(v);
	}
	
	private void previewCameraPicture(View v) {
		CustomImageView selectedImageView = (CustomImageView) v;
		String path = selectedImageView.getPicture().getUrl();
		if (!new File(path).exists()) return;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());

		builder.setTitle("Image Preview");

		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);

		builder.setView(scrollView);
		ImageView imageView = new ImageView(this.getContext());
		
		imageView.setImageBitmap(decodeFile(new File(selectedImageView.getPicture().getUrl()), 500, 500));
		layout.addView(imageView);
		
		TextView text = new TextView(this.getContext());
		text.setText(getCameraMetaData(path));
		layout.addView(text);
		
		builder.setNeutralButton("Done", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// Nothing
			}

		});
		builder.create().show();
	}

	private String getCameraMetaData(String path) {
		File videoFile = new File(path);

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Picture Metadata:");
		stringBuilder.append("\n");
		stringBuilder.append("File name: " + videoFile.getName());
		stringBuilder.append("\n");
		stringBuilder.append("File size: " + videoFile.length() + " bytes");
		stringBuilder.append("\n");
		Date lastModifiedDate = new Date(videoFile.lastModified());
		stringBuilder.append("Picture date: " + lastModifiedDate.toString());
		return stringBuilder.toString();
	}
	
	public static Bitmap decodeFile(File f, int WIDTH, int HIGHT) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			// The new size we want to scale to
			final int REQUIRED_WIDTH = WIDTH;
			final int REQUIRED_HIGHT = HIGHT;
			// Find the correct scale value. It should be the power of 2.
			int scale = 1;
			while (o.outWidth / scale / 2 >= REQUIRED_WIDTH
					&& o.outHeight / scale / 2 >= REQUIRED_HIGHT)
				scale *= 2;

			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (Exception e) {
			FLog.e("error when decode the bitmap", e);
		}
		return null;
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

	public void addPicture(String value) {
		Picture picture = new Picture(value, null, value);
		addSelectedImage(addGallery(picture));
		autoSaveManager.save();
	}
	
	@Override
	public boolean hasAttributeChanges(
			Collection<? extends Attribute> attributes) {
		return Compare.compareAttributeValues(this, attributes);
	}

}
