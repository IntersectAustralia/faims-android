package au.org.intersect.faims.android.ui.view;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.data.FormAttribute;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.util.Compare;

public class PictureGallery extends HorizontalScrollView implements ICustomView {

	class PictureGalleryOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v)
		{
			if(listener != null) {
				listener.onClick(v);
			}
			if(imageListener != null) {
				imageListener.onClick(v);
			}
		}
		
	}
	
	protected static final int GALLERY_SIZE = 400;
	
	private String ref;
	private List<NameValuePair> currentValues;
	private float certainty;
	private float currentCertainty;
	private String annotation;
	private String currentAnnotation;
	private boolean isMulti;
	protected boolean dirty;
	protected String dirtyReason;
	
	protected List<CustomImageView> selectedImages;
	protected ArrayList<CustomImageView> galleryImages;
	protected LinearLayout galleriesLayout;
	
	protected PictureGalleryOnClickListener pictureGalleryListener;
	protected OnClickListener listener;
	protected OnClickListener imageListener;

	private boolean annotationEnabled;

	private boolean certaintyEnabled;

	private FormAttribute attribute;

	public PictureGallery(Context context) {
		super(context);
		this.pictureGalleryListener = new PictureGalleryOnClickListener();
	}

	public PictureGallery(Context context, FormAttribute attribute, String ref, boolean isMulti) {
		super(context);
		this.pictureGalleryListener = new PictureGalleryOnClickListener();
		this.attribute = attribute;
		this.ref = ref;
		this.isMulti = isMulti;
		
		galleriesLayout = new LinearLayout(this.getContext());
	    galleriesLayout.setOrientation(LinearLayout.HORIZONTAL);
	    galleryImages = new ArrayList<CustomImageView>();
		addView(galleriesLayout);
		
		reset();
	}

	@Override
	public String getAttributeName() {
		return attribute.name;
	}

	@Override
	public String getAttributeType() {
		return attribute.type;
	}

	@Override
	public String getRef() {
		return ref;
	}
	
	@Override
	public float getCertainty() {
		return certainty;
	}

	@Override
	public void setCertainty(float certainty) {
		this.certainty = certainty;
	}

	@Override
	public String getAnnotation() {
		return annotation;
	}

	@Override
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public boolean isMulti() {
		return isMulti;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}
	
	@Override
	public void setDirty(boolean value) {
		this.dirty = value;
	}
	
	@Override
	public void setDirtyReason(String value) {
		this.dirtyReason = value;
	}

	@Override
	public String getDirtyReason() {
		return dirtyReason;
	}
	
	@Override
	public void reset() {
		dirty = false;
		dirtyReason = null;
		removeSelectedImages();
		setCertainty(1);
		setAnnotation("");
		save();
	}
	
	@SuppressWarnings("unchecked")
	private boolean compareValues() {
		List<NameValuePair> values = (List<NameValuePair>) getValues();
		if (values == null && currentValues == null) return true;
		if (values == null && currentValues != null) return false;
		if (values != null && currentValues == null) return false;
		if (values.size() != currentValues.size()) return false;
			
		for (int i = 0; i < values.size(); i++) {
			boolean hasValue = false;
			for (int j = 0; j < currentValues.size(); j++) {
				if (values.get(i).equals(currentValues.get(j))) {
					hasValue = true;
					break;
				}
			}
			if (!hasValue) return false;
		}
		
		return true;
	}
	
	@Override
	public boolean hasChanges() {
		if (attribute.readOnly) return false;
		return !(compareValues()) || 
				!Compare.equal(getAnnotation(), currentAnnotation) || 
				!Compare.equal(getCertainty(), currentCertainty);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void save() {
		currentValues = (List<NameValuePair>) getValues();
		currentCertainty = getCertainty();
		currentAnnotation = getAnnotation();
	}
	
	protected void updateImages() {
		if (galleryImages == null) return;
		
		for (CustomImageView view : galleryImages) {
            if (selectedImages != null && selectedImages.contains(view)) {
                view.setBackgroundColor(Color.BLUE);
            } else {
                view.setBackgroundColor(Color.LTGRAY);
            }
        }
		updateImageListeners();
	}
	
	protected void updateImageListeners() {
		if (galleryImages == null) return;
		
		for(CustomImageView image : galleryImages)
		{
			image.setOnClickListener(this.pictureGalleryListener);
		}
	}
	
	public List<CustomImageView> getSelectedImages() {
		return selectedImages;
	}
	
	public int getSelectionItem() {
		if (selectedImages != null && !selectedImages.isEmpty()) {
			return galleryImages.indexOf(selectedImages.get(0)); 
		}
		return -1;
	}
	
	public void setSelectionItem(int position) {
		if (galleryImages != null && galleryImages.size() > position) {
			addSelectedImage(galleryImages.get(position));
		}
		
		updateImages();
	}

	public void addSelectedImage(CustomImageView imageView){
		if (!isMulti) {
			removeSelectedImages();
		}
		
		if(selectedImages == null){
			selectedImages = new ArrayList<CustomImageView>();
		}
		
		if(!selectedImages.contains(imageView)){
			selectedImages.add(imageView);
		}
		
		updateImages();
	}
	
	public void addSelectedImages(List<CustomImageView> imageViews) {
		for(CustomImageView customImageView : imageViews){
			addSelectedImage(customImageView);
		}
	}
	
	public void removeSelectedImage(CustomImageView imageView){
		if(selectedImages != null && selectedImages.contains(imageView)){
			selectedImages.remove(imageView);
		}
		updateImages();
	}
	
	public void removeSelectedImages(){
		if(selectedImages != null){
			selectedImages.clear();
		}
		selectedImages = null;
		updateImages();
	}
	
	@Override
	public String getValue() {
		if(selectedImages != null && !selectedImages.isEmpty()){
			return getSelectedImages().get(0)
					.getPicture().getId();
		} 
		return null;
	}

	@Override
	public void setValue(String value) {
		for (CustomImageView image : galleryImages) {
			if (image.getPicture().getId().equals(value)) {
				addSelectedImage(image);
			}
		}
	}
	
	@Override
	public List<?> getValues() {
		if (selectedImages != null && !selectedImages.isEmpty()) {
			List<NameValuePair> selectedPictures = new ArrayList<NameValuePair>();
			for (CustomImageView imageView : selectedImages) {
				selectedPictures.add(new NameValuePair(imageView.getPicture().getId(), "true"));
			}
			return selectedPictures;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setValues(List<?> values) {
		List<NameValuePair> pairs = (List<NameValuePair>) values;
		for (NameValuePair pair : pairs) {
			for (CustomImageView imageView : galleryImages) {
				if (imageView.getPicture().getId().equals(pair.getName())) {
					if ("true".equals(pair.getValue())) {
						addSelectedImage(imageView);
					} else {
						removeSelectedImage(imageView);
					}
				}
			}
		}
	}
	
	public List<NameValuePair> getPairs() {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		for (CustomImageView imageView : galleryImages) {
			String name = imageView.getPicture().getId();
			String value = name; // id == url
			pairs.add(new NameValuePair(name, value));
		}
		return pairs;
	}
	
	public void setPairs(List<NameValuePair> pairs) {
		ArrayList<Picture> pictures = new ArrayList<Picture>();
		for (NameValuePair pair : pairs) {
			Picture picture = new Picture(pair.getName(), null, pair.getValue());
			pictures.add(picture);
		}
		populate(pictures);
	}

	public void populate(List<Picture> pictures) {
		removeSelectedImages();
		
		galleriesLayout.removeAllViews();
		galleryImages = new ArrayList<CustomImageView>();
		
		for (Picture picture : pictures) {
			addGallery(picture);
		}
	}
	
	protected CustomImageView addGallery(Picture picture) {
		String path = picture.getUrl();
	
		LinearLayout galleryLayout = new LinearLayout(
				galleriesLayout.getContext());
		galleryLayout.setOrientation(LinearLayout.VERTICAL);
		
		final CustomImageView gallery = new CustomImageView(
				galleriesLayout.getContext());
		
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				GALLERY_SIZE, GALLERY_SIZE);
		
		setGalleryImage(gallery, path);
		
		gallery.setBackgroundColor(Color.LTGRAY);
		gallery.setPadding(10, 10, 10, 10);
		gallery.setLayoutParams(layoutParams);
		gallery.setPicture(picture);
		this.listener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				selectImage(v);
			}
		};
		gallery.setOnLongClickListener(new OnLongClickListener() {
	
			@Override
			public boolean onLongClick(View v) {
				longSelectImage(v);
				return true;
			}
		});
		
		TextView textView = new TextView(
				galleriesLayout.getContext());
		String name = picture.getName() != null ? picture
				.getName() : new File(path).getName();
		textView.setText(name);
		textView.setGravity(Gravity.CENTER_HORIZONTAL);
		textView.setTextSize(20);
		galleryLayout.addView(textView);
		galleryLayout.addView(gallery);
		
		galleryImages.add(gallery);
		galleriesLayout.addView(galleryLayout);
		
		return gallery;
	}

	protected void selectImage(View v) {
		CustomImageView selectedImageView = (CustomImageView) v;
		
		if(isMulti){
    		if(getSelectedImages() != null && getSelectedImages().contains(selectedImageView)){
    			removeSelectedImage(selectedImageView);
    		}else{
				addSelectedImage(selectedImageView);
    		}
    	}else{
            addSelectedImage(selectedImageView);
    	}
		
		updateImages();
	}

	protected void longSelectImage(View v) {
		// TODO Auto-generated method stub
		
	}

	protected void setGalleryImage(CustomImageView gallery, String path) {
		if(path != null && new File(path).exists()){
			gallery.setImageURI(Uri.parse(path));
		}
	}
	
	@Override
	public boolean getAnnotationEnabled() {
		return annotationEnabled;
	}

	@Override
	public void setAnnotationEnabled(boolean enabled) {
		annotationEnabled = enabled;
	}

	@Override
	public boolean getCertaintyEnabled() {
		return certaintyEnabled;
	}

	@Override
	public void setCertaintyEnabled(boolean enabled) {
		certaintyEnabled = enabled;
	}
	
	public void setImageListener(OnClickListener imageListener)
	{
		this.imageListener = imageListener;
		updateImageListeners();
	}
}
