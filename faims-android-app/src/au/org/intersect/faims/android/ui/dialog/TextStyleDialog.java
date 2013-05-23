package au.org.intersect.faims.android.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.nutiteq.GeometryTextStyle;

public class TextStyleDialog extends SettingsDialog {
	
	public static class Builder extends SettingsDialog.Builder {

		private GeometryTextStyle style;

		public Builder(Context context, GeometryTextStyle style) {
			super(context);
			this.style = style;
		}
		
		@Override
		public SettingsDialog createDialog() {
			final TextStyleDialog d = new TextStyleDialog(context);
			final GeometryTextStyle style = this.style;
			d.setStyle(style);
			
			setTitle("Style Settings");
			
			addRange("minZoom", "Min Zoom:", style.minZoom, 0, FaimsSettings.MAX_ZOOM);
			addTextField("color", "Text Color:", Integer.toHexString(style.color));
			addRange("size", "Text Size:", style.size, 0, 100);
			
			setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					try {
						int minZoom = d.parseRange("minZoom", 0, FaimsSettings.MAX_ZOOM);
						int color = d.parseColor("color");
						int size = d.parseRange("size", 0, 100);
						
						style.minZoom = minZoom;
						style.color = color;
						style.size = size;
					} catch (Exception e) {
						d.showError(e.getMessage());
					}
				}
			});
			
			setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// ignore
				}
			});
			
			return d;
		}
		
	}

	private GeometryTextStyle style;
	
	protected TextStyleDialog(Context context) {
		super(context);
	}

	public GeometryTextStyle getStyle() {
		return style;
	}

	public void setStyle(GeometryTextStyle style) {
		this.style = style;
	}

}
