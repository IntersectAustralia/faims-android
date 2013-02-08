package au.org.intersect.faims.android.ui.form;

import java.util.List;

import android.content.Context;
import android.widget.HorizontalScrollView;

public class CustomHorizontalScrollView extends HorizontalScrollView{

	private String attributeName;
	private String attributeType;
	private String ref;
	private CustomImageView selectedImageView;
	private List<CustomImageView> imageViews;
	private float certainty = 1;
	private float currentCertainty = 1;
	private String annotation = "";
	private String currentAnnotation = "";

	public CustomHorizontalScrollView(Context context) {
		super(context);
	}

	public CustomHorizontalScrollView(Context context, String attributeName, String attributeType, String ref) {
		super(context);
		this.attributeName = attributeName;
		this.attributeType = attributeType;
		this.ref = ref;
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
}
