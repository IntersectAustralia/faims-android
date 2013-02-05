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
}
