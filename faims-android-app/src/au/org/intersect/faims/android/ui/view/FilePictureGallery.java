package au.org.intersect.faims.android.ui.view;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.constants.PictureConstants;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.managers.BitmapManager;
import au.org.intersect.faims.android.managers.CSSManager;
import au.org.intersect.faims.android.ui.dialog.FileAttachmentLabelDialog;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.google.inject.Inject;

public class FilePictureGallery extends CustomFileList {
	
	protected ArrayList<CustomImageView> galleryImages;
	protected LinearLayout galleriesLayout;
	
	@Inject
	BitmapManager bitmapManager;
	
	@Inject
	CSSManager cssManager;
	
	public FilePictureGallery(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public FilePictureGallery(Context context, FormInputDef attribute, String ref, boolean dynamic, boolean sync) {
		super(context, attribute, ref, dynamic, sync);
		FAIMSApplication.getInstance().injectMembers(this);
		
		HorizontalScrollView scrollView = new HorizontalScrollView(this.getContext());
		galleriesLayout = new LinearLayout(this.getContext());
	    galleriesLayout.setOrientation(LinearLayout.HORIZONTAL);
	    galleriesLayout.setGravity(Gravity.BOTTOM);
	    scrollView.addView(galleriesLayout);
	    addView(scrollView);
	    
		cssManager.addCSS(this, "file-gallery");
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
		annotations = null;
		certainties = null;
		annotationIcons = new ArrayList<ImageView>();
		certaintyIcons = new ArrayList<ImageView>();
		for (Picture picture : pictures) {
			addFilePicture(picture, FaimsSettings.DEFAULT_ANNOTATION, String.valueOf(FaimsSettings.DEFAULT_CERTAINTY));
		}
		updateIcons();
	}
	
	protected CustomImageView addGallery(Picture picture) {
		String path = picture.getUrl();
	
		LinearLayout galleryLayout = new LinearLayout(galleriesLayout.getContext());
		galleryLayout.setOrientation(LinearLayout.VERTICAL);
		
		final CustomImageView gallery = new CustomImageView(
				galleriesLayout.getContext());
		
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				PictureConstants.GALLERY_SIZE, PictureConstants.GALLERY_SIZE);
		
		// check if thumbnail exists for file
		File thumbnail = FileUtil.getThumbnailFileFor(new File(path));
		if (thumbnail != null && thumbnail.exists()) {
			setGalleryImage(gallery, thumbnail.getPath());
		} else {
			setGalleryImage(gallery, path);
		}
		
		int padding = (int) ScaleUtil.getDip(galleriesLayout.getContext(), PictureConstants.GALLERY_ITEM_PADDING);
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
		
		cssManager.addCSS(galleryLayout, "file-gallery");
		
		LinearLayout headerContainer = new LinearLayout(galleriesLayout.getContext());
		headerContainer.setOrientation(LinearLayout.VERTICAL);
		
		TextView textView = new TextView(
				galleriesLayout.getContext());
		String name = picture.getName() != null ? picture
				.getName() : new File(path).getName();
		textView.setText(FileUtil.getStrippedFileAttachmentName(name));
		textView.setGravity(Gravity.CENTER_HORIZONTAL);
		textView.setTextSize(15);
		textView.setWidth(PictureConstants.GALLERY_SIZE);
		textView.setPadding(padding, padding, padding, padding);
		
		FrameLayout galleryContainer = new FrameLayout(galleriesLayout.getContext());
		cssManager.addCSS(galleryContainer, "file-gallery-item");
		galleryContainer.addView(gallery);
		
		LinearLayout iconContainer = new LinearLayout(galleriesLayout.getContext());
		iconContainer.setGravity(Gravity.END);
		iconContainer.setOrientation(LinearLayout.HORIZONTAL);
		final FileAttachmentLabelDialog dialog = new FileAttachmentLabelDialog(getContext(), this, galleryImages.size());
		ImageView annotationImage = viewFactory.createAnnotationIcon();
		if (annotationEnabled) {
			iconContainer.addView(annotationImage);
			dialog.addAnnotationTab();
		}
		annotationIcons.add(annotationImage);
		ImageView certaintyImage = viewFactory.createCertaintyIcon();
		if (certaintyEnabled) {
			iconContainer.addView(certaintyImage);
			dialog.addCertaintyTab();
		}
		certaintyIcons.add(certaintyImage);
		headerContainer.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				dialog.show(galleryImages.indexOf(gallery));
				return true;
			}
		});
		
		headerContainer.setBackgroundResource(R.drawable.label_selector);
		headerContainer.addView(textView);
		headerContainer.addView(iconContainer);
		
		galleryLayout.addView(headerContainer);
		galleryLayout.addView(galleryContainer);
		
		galleryImages.add(gallery);
		galleriesLayout.addView(galleryLayout);
		
		return gallery;
	}

	protected void longSelectImage(View v) {	
	}
	
	protected void setGalleryImage(CustomImageView gallery, String path) {
		if(path != null && new File(path).exists()) {
			gallery.setImageBitmap(bitmapManager.createBitmap(path, PictureConstants.THUMBNAIL_IMAGE_SIZE, PictureConstants.THUMBNAIL_IMAGE_SIZE));
		}
	}
	
	public void addFile(String value, String annotation, String certainty) {
		addFileView(value);
		super.addFile(value, annotation, certainty);
	}
	
	public void addFileView(String filePath) {
		Picture picture = new Picture(filePath, null, filePath);
		addGallery(picture);
	}
	
	public void addFilePicture(Picture picture, String annotation, String certainty) {
		addGallery(picture);
		super.addFile(picture.getId(), annotation, certainty);
	}
	
	public void removeGalleryItem(CustomImageView view) {
		int index = galleryImages.indexOf(view);
		galleriesLayout.removeViewAt(index);
		annotations.remove(index);
		annotationIcons.remove(index);
		certainties.remove(index);
		certaintyIcons.remove(index);
		galleryImages.remove(view);
		notifySave();
	}
	
	@Override
	public void reload() {
		if (reloadPairs == null) return;
		populateAndKeepPictureURL(reloadOldPairs, reloadPairs);
		setAnnotations(reloadAnnotations);
		setCertainties(reloadCertainties);
		save();
		reloadOldPairs = null;
		reloadPairs = null;
		reloadAnnotations = null;
		reloadCertainties = null;
	}
	
	public void populateAndKeepPictureURL(List<NameValuePair> oldPairs, List<NameValuePair> newPairs) {
		if (oldPairs == null) return;
		ArrayList<Picture> pictures = new ArrayList<Picture>();
		for (int i = 0; i < oldPairs.size(); i++) {
			NameValuePair oldPair = oldPairs.get(i);
			NameValuePair newPair = newPairs.get(i);
			Picture picture = new Picture(newPair.getName(), null, oldPair.getName());
			pictures.add(picture);
		}
		populateImages(pictures);
	}
	
	public String getGalleryFileName(CustomImageView gallery) {
		return new File(gallery.getPicture().getId()).getName();
	}

}
