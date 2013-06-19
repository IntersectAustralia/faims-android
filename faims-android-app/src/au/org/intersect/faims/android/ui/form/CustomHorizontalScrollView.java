package au.org.intersect.faims.android.ui.form;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.HorizontalScrollView;

public class CustomHorizontalScrollView extends HorizontalScrollView{

	private String attributeName;
	private String attributeType;
	private String ref;
	private CustomImageView selectedImageView;
	private List<CustomImageView> selectedImageViews;
	private List<CustomImageView> imageViews;
	private float certainty = 1;
	private float currentCertainty = 1;
	private String annotation = "";
	private String currentAnnotation = "";
	private boolean isMulti;
	private boolean dirty;
	private String dirtyReason;

	public CustomHorizontalScrollView(Context context) {
		super(context);
	}

	public CustomHorizontalScrollView(Context context, String attributeName, String attributeType, String ref, boolean isMulti) {
		super(context);
		this.attributeName = attributeName;
		this.attributeType = attributeType;
		this.ref = ref;
		this.isMulti = isMulti;
	}
	
	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public String getAttributeType() {
		return attributeType;
	}

	public void setAttributeType(String attributeType) {
		this.attributeType = attributeType;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public CustomImageView getSelectedImageView() {
		return selectedImageView;
	}

	public void setSelectedImageView(CustomImageView selectedImageView) {
		this.selectedImageView = selectedImageView;
	}

	public List<CustomImageView> getImageViews() {
		return imageViews;
	}

	public void setImageViews(List<CustomImageView> imageViews) {
		this.imageViews = imageViews;
	}
	
	public void addImageView(CustomImageView imageView){
		this.imageViews.add(imageView);
	}

	public float getCertainty() {
		return certainty;
	}

	public void setCertainty(float certainty) {
		this.certainty = certainty;
	}

	public float getCurrentCertainty() {
		return currentCertainty;
	}

	public void setCurrentCertainty(float currentCertainty) {
		this.currentCertainty = currentCertainty;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public String getCurrentAnnotation() {
		return currentAnnotation;
	}

	public void setCurrentAnnotation(String currentAnnotation) {
		this.currentAnnotation = currentAnnotation;
	}

	public List<CustomImageView> getSelectedImageViews() {
		return selectedImageViews;
	}

	public void setSelectedImageViews(List<CustomImageView> imageViews){
		for(CustomImageView customImageView : imageViews){
			addSelectedImageView(customImageView);
		}
	}

	public void addSelectedImageView(CustomImageView imageView){
		if(this.selectedImageViews == null){
			this.selectedImageViews = new ArrayList<CustomImageView>();
		}
		this.selectedImageViews.add(imageView);
	}
	
	public void removeSelectedImageView(CustomImageView imageView){
		if(this.selectedImageViews != null){
			this.selectedImageViews.remove(imageView);
		}
	}
	
	public void removeSelectedImageViews(){
		if(this.selectedImageViews != null){
			this.selectedImageViews.clear();
		}
		this.selectedImageViews = null;
	}

	public boolean isMulti() {
		return isMulti;
	}

	public boolean isDirty() {
		return dirty;
	}
	
	public void setDirty(boolean value) {
		this.dirty = value;
	}
	
	public void setDirtyReason(String value) {
		this.dirtyReason = value;
	}

	public String getDirtyReason() {
		return dirtyReason;
	}
}
