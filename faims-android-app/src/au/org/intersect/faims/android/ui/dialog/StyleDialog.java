package au.org.intersect.faims.android.ui.dialog;

import android.content.Context;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;

public class StyleDialog extends SettingsDialog {
	
	public static class Builder extends SettingsDialog.Builder {

		protected GeometryStyle style;
		
		public Builder(Context context, GeometryStyle style) {
			super(context);
			this.style = style;
		}
		
	}

	protected GeometryStyle style;
	
	protected StyleDialog(Context context) {
		super(context);
	}
	
	public GeometryStyle getStyle() {
		return style;
	}

	public void setStyle(GeometryStyle style) {
		this.style = style;
	}

}
