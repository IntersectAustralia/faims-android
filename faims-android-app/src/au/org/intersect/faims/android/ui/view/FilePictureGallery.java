package au.org.intersect.faims.android.ui.view;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.dialog.FileAttachmentLabelDialog;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nativecss.NativeCSS;

public class FilePictureGallery extends CustomFileList {

	protected static final int GALLERY_SIZE = 400;
	private static final int GALLERY_ITEM_PADDING = 10;
	
	protected ArrayList<CustomImageView> galleryImages;
	protected LinearLayout galleriesLayout;
	
	public FilePictureGallery(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public FilePictureGallery(Context context, FormInputDef attribute, String ref, boolean dynamic, boolean sync) {
		super(context, attribute, ref, dynamic, sync);
		
		HorizontalScrollView scrollView = new HorizontalScrollView(this.getContext());
		galleriesLayout = new LinearLayout(this.getContext());
	    galleriesLayout.setOrientation(LinearLayout.HORIZONTAL);
	    galleriesLayout.setGravity(Gravity.BOTTOM);
	    scrollView.addView(galleriesLayout);
	    addView(scrollView);
	    
		NativeCSS.addCSSClass(this, "file-gallery");
		reset();
	}
	
	@Override
	public void reset() {
		dirty = false;
		dirtyReason = null;
		
		if (galleriesLayout != null) {
			galleriesLayout.removeAllViews();
		}
		galleryImages = new ArrayList<CustomImageView>();
		
		annotations = null;
		annotationIcons = new ArrayList<ImageView>();
		certainties = null;
		certaintyIcons = new ArrayList<ImageView>();
		save();
	}

	@Override
	public List<?> getValues() {
		if (galleryImages != null && !galleryImages.isEmpty()) {
			List<NameValuePair> pictures = new ArrayList<NameValuePair>();
			for (CustomImageView imageView : galleryImages) {
				pictures.add(new NameValuePair(imageView.getPicture().getId(), "true"));
			}
			return pictures;
		}
		return null;
	}
	
	@Override
	public List<NameValuePair> getPairs() {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		for (CustomImageView imageView : galleryImages) {
			String name = imageView.getPicture().getId();
			String value = name; // id == url
			pairs.add(new NameValuePair(name, value));
		}
		return pairs;
	}
	
	@Override
	public void populate(List<NameValuePair> pairs) {
		if (pairs == null) return;
		ArrayList<Picture> pictures = new ArrayList<Picture>();
		for (NameValuePair pair : pairs) {
			Picture picture = new Picture(pair.getName(), null, pair.getValue());
			pictures.add(picture);
		}
		populateImages(pictures);
	}
	
	public void populateImages(List<Picture> pictures) {
		if (galleriesLayout != null) {
			galleriesLayout.removeAllViews();
		}
		galleryImages = new ArrayList<CustomImageView>();	
		
		for (Picture picture : pictures) {
			addFileView(picture.getUrl());
		}
	}
	
	protected CustomImageView addGallery(Picture picture) {
		String path = picture.getUrl();
	
		LinearLayout galleryLayout = new LinearLayout(galleriesLayout.getContext());
		galleryLayout.setOrientation(LinearLayout.VERTICAL);
		
		final CustomImageView gallery = new CustomImageView(
				galleriesLayout.getContext());
		
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				GALLERY_SIZE, GALLERY_SIZE);
		
		// check if thumbnail exists for file
		File thumbnail = FileUtil.getThumbnailFileFor(new File(path));
		if (thumbnail != null && thumbnail.exists()) {
			setGalleryImage(gallery, thumbnail.getPath());
		} else {
			setGalleryImage(gallery, path);
		}
		
		int padding = (int) ScaleUtil.getDip(galleriesLayout.getContext(), GALLERY_ITEM_PADDING);
		int topPadding = padding * 3; // room for annotation/certainty icons
		gallery.setPadding(padding, topPadding, padding, padding);
		gallery.setLayoutParams(layoutParams);
		gallery.setPicture(picture);
		gallery.setBackgroundResource(R.drawable.label_selector);
		gallery.setOnLongClickListener(new OnLongClickListener() {
	
			@Override
			public boolean onLongClick(View v) {
				longSelectImage(v);
				return true;
			}
		});
		
		NativeCSS.addCSSClass(galleryLayout, "file-gallery");
		
		TextView textView = new TextView(
				galleriesLayout.getContext());
		String name = picture.getName() != null ? picture
				.getName() : new File(path).getName();
		textView.setText(name);
		textView.setBackgroundResource(R.drawable.label_selector);
		textView.setGravity(Gravity.CENTER_HORIZONTAL);
		textView.setTextSize(15);
		textView.setWidth(GALLERY_SIZE);
		textView.setPadding(padding, padding, padding, padding);
		
		FrameLayout galleryContainer = new FrameLayout(galleriesLayout.getContext());
		NativeCSS.addCSSClass(galleryContainer, "file-gallery-item");
		galleryContainer.addView(gallery);
		
		LinearLayout iconContainer = new LinearLayout(galleriesLayout.getContext());
		iconContainer.setGravity(Gravity.END);
		iconContainer.setOrientation(LinearLayout.HORIZONTAL);
		final FileAttachmentLabelDialog dialog = new FileAttachmentLabelDialog(getContext(), this, galleryImages.size());
		if (annotationEnabled) {
			ImageView annotationImage = viewFactory.createAnnotationIcon();
			iconContainer.addView(annotationImage);
			dialog.addAnnotationTab();
			annotationIcons.add(annotationImage);
		}
		if (certaintyEnabled) {
			ImageView certaintyImage = viewFactory.createCertaintyIcon();
			iconContainer.addView(certaintyImage);
			dialog.addCertaintyTab();
			certaintyIcons.add(certaintyImage);
		}
		textView.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				dialog.show();
				return true;
			}
		});
		galleryContainer.addView(iconContainer);
		
		galleryLayout.addView(textView);
		galleryLayout.addView(galleryContainer);
		
		galleryImages.add(gallery);
		galleriesLayout.addView(galleryLayout);
		
		return gallery;
	}

	protected void longSelectImage(View v) {	
	}
	
	protected void setGalleryImage(CustomImageView gallery, String path) {
		if(path != null && new File(path).exists()) {
			gallery.setImageBitmap(decodeFile(new File(path), GALLERY_SIZE, GALLERY_SIZE));
		}
	}
	
	public Bitmap decodeFile(File f, int WIDTH, int HIGHT) {
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
	
	public void addFile(String value, String annotation, String certainty) {
		addFileView(value);
		super.addFile(value, annotation, certainty);
	}
	
	public void addFileView(String filePath) {
		Picture picture = new Picture(filePath, null, filePath);
		addGallery(picture);
	}

}
