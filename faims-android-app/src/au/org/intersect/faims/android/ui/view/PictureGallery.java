package au.org.intersect.faims.android.ui.view;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.data.Attribute;
import au.org.intersect.faims.android.data.FormInputDef;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.managers.AutoSaveManager;
import au.org.intersect.faims.android.util.Compare;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.google.inject.Inject;
import com.nativecss.NativeCSS;

public class PictureGallery extends HorizontalScrollView implements ICustomView {


	class PictureGalleryOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v)
		{
			selectImage(v);
			if(imageListener != null) {
				imageListener.onClick(v);
			}
			notifySave();
		}
		
	}
	
	class PictureGalleryInternalOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v)
		{
			selectImage(v);
		}
		
	}
	
	@Inject
	AutoSaveManager autoSaveManager;
	
	@Inject
	BeanShellLinker linker;
	
	protected static final int GALLERY_SIZE = 400;
	private static final int GALLERY_ITEM_PADDING = 10;
	
	private String ref;
	private boolean dynamic;
	protected List<NameValuePair> currentValues;
	protected float certainty;
	protected float currentCertainty;
	protected String annotation;
	protected String currentAnnotation;
	protected boolean isMulti;
	protected boolean dirty;
	protected String dirtyReason;
	
	protected List<CustomImageView> selectedImages;
	protected ArrayList<CustomImageView> galleryImages;
	protected LinearLayout galleriesLayout;
	
	protected OnClickListener imageListener;
	protected PictureGalleryInternalOnClickListener internalListener;
	protected PictureGalleryOnClickListener customListener;

	private boolean annotationEnabled;
	private boolean certaintyEnabled;

	private FormInputDef inputDef;

	private String selectCallback;
	private String focusCallback;
	private String blurCallback;

	public PictureGallery(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		this.internalListener = new PictureGalleryInternalOnClickListener();
		this.customListener = new PictureGalleryOnClickListener();
		NativeCSS.addCSSClass(this, "gallery");
	}

	public PictureGallery(Context context, FormInputDef inputDef, String ref, boolean dynamic, boolean isMulti) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		this.inputDef = inputDef;
		this.ref = ref;
		this.dynamic = dynamic;
		this.isMulti = isMulti;
		
		galleriesLayout = new LinearLayout(this.getContext());
	    galleriesLayout.setOrientation(LinearLayout.HORIZONTAL);
	    galleriesLayout.setGravity(Gravity.BOTTOM);
	    galleryImages = new ArrayList<CustomImageView>();
		addView(galleriesLayout);		
		this.internalListener = new PictureGalleryInternalOnClickListener();
		this.customListener = new PictureGalleryOnClickListener();
		NativeCSS.addCSSClass(this, "gallery");
		reset();
	}

	@Override
	public String getAttributeName() {
		return inputDef.name;
	}

	@Override
	public String getAttributeType() {
		return inputDef.type;
	}

	@Override
	public String getRef() {
		return ref;
	}
	
	@Override
	public boolean isDynamic() {
		return dynamic;
	}
	
	@Override
	public float getCertainty() {
		return certainty;
	}

	@Override
	public void setCertainty(float certainty) {
		this.certainty = certainty;
		notifySave();
	}

	@Override
	public String getAnnotation() {
		return annotation;
	}

	@Override
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
		notifySave();
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
	@Override
	public boolean hasChanges() {
		return !(Compare.compareValues((List<NameValuePair>) getValues(), currentValues)) || 
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
            	((FrameLayout)view.getParent()).getChildAt(1).setVisibility(View.VISIBLE);
            } else {
            	((FrameLayout)view.getParent()).getChildAt(1).setVisibility(View.GONE);
            }
        }
		updateImageListeners();
	}
	
	protected void updateImageListeners() {
		if (galleryImages == null || galleryImages.size() == 0) return;
		
		for(CustomImageView image : galleryImages)
		{
			image.setOnClickListener(this.customListener);
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
		notifySave();
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
		if (values == null) return;
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
		notifySave();
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
		if (pairs == null) return;
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
		
		int padding = (int) ScaleUtil.getDip(galleriesLayout.getContext(), GALLERY_ITEM_PADDING);
		gallery.setPadding(padding, padding, padding, padding);
		gallery.setLayoutParams(layoutParams);
		gallery.setPicture(picture);
		gallery.setOnClickListener(this.customListener);
		gallery.setOnLongClickListener(new OnLongClickListener() {
	
			@Override
			public boolean onLongClick(View v) {
				longSelectImage(v);
				return true;
			}
		});
		
		NativeCSS.addCSSClass(galleryLayout, "gallery");
		NativeCSS.addCSSClass(gallery, "gallery-item");
		
		TextView textView = new TextView(
				galleriesLayout.getContext());
		String name = picture.getName() != null ? picture
				.getName() : new File(path).getName();
		textView.setText(name);
		textView.setGravity(Gravity.CENTER_HORIZONTAL);
		textView.setTextSize(15);
		textView.setWidth(GALLERY_SIZE);
		textView.setPadding(padding, padding, padding, padding);
		galleryLayout.addView(textView);
		
		FrameLayout imageContainer = new FrameLayout(galleriesLayout.getContext());
		NativeCSS.addCSSClass(imageContainer, "gallery-item");
		
		imageContainer.addView(gallery);
		View border = new View(galleriesLayout.getContext());
		border.setBackgroundResource(R.drawable.gallery_selection_border);
		border.setVisibility(View.GONE);
		imageContainer.addView(border);
		
		galleryLayout.addView(imageContainer);
		
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
		if(path != null && new File(path).exists()) {
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
	
	protected void notifySave() {
		if (getAttributeName() != null && hasChanges()) {
			autoSaveManager.save();
		}
	}
	
	@Override
	public boolean hasAttributeChanges(
			HashMap<String, ArrayList<Attribute>> attributes) {
		return Compare.compareAttributeValue(this, attributes);
	}
	
	@Override
	public String getClickCallback() {
		return null;
	}

	@Override
	public void setClickCallback(final String code) {
		setSelectCallback(code);
	}

	@Override
	public String getSelectCallback() {
		return selectCallback;
	}

	@Override
	public void setSelectCallback(String code) {
		if (code == null) return;
		selectCallback = code;
		setImageListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				linker.execute(selectCallback);
			}

		});
	}

	@Override
	public String getFocusCallback() {
		return focusCallback;
	}
	
	@Override
	public String getBlurCallback() {
		return blurCallback;
	}
	
	@Override
	public void setFocusBlurCallbacks(String focusCode, String blurCode) {
		if (focusCode == null && blurCode == null) return;
		focusCallback = focusCode;
		blurCallback = blurCode;
		setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					linker.execute(focusCallback);
				} else {
					linker.execute(blurCallback);
				}
			}
		});
	}
	
}
