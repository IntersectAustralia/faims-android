package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.widget.ImageView;

public class CustomImageView extends ImageView{

	private Picture picture;

	public CustomImageView(Context context){
		super(context);
	}

	public Picture getPicture() {
		return picture;
	}

	public void setPicture(Picture picture) {
		this.picture = picture;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof CustomImageView){
			CustomImageView other = (CustomImageView) o;
			return getPicture().equals(other.getPicture());
		}
		return super.equals(o);
	}
}
