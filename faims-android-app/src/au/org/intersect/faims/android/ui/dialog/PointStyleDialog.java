package au.org.intersect.faims.android.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;

public class PointStyleDialog extends StyleDialog {
	
	public static class Builder extends StyleDialog.Builder {
		
		public Builder(Context context, GeometryStyle style) {
			super(context, style);
		}
		
		@Override
		public SettingsDialog createDialog() {
			final PointStyleDialog d = new PointStyleDialog(context);
			final GeometryStyle style = this.style;
			d.setStyle(style);
			
			setTitle("Style Settings");
			
			addRange("minZoom", "Min Zoom:", style.minZoom, 0, FaimsSettings.MAX_ZOOM);
			addColorField("color", "Point Color:", Integer.toHexString(style.pointColor));
			addSlider("size", "Point Size:", style.size);
			addSlider("pickingSize", "Point Picking Size:", style.pickingSize);
			
			if (this.positiveListener == null) {
				setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int minZoom = d.parseRange("minZoom", 0, FaimsSettings.MAX_ZOOM);
							int color = d.parseColor("color");
							float size = d.parseSlider("size");
							float pickingSize = d.parseSlider("pickingSize");
							
							style.minZoom = minZoom;
							style.pointColor = color;
							style.size = size;
							style.pickingSize = pickingSize;
						} catch (Exception e) {
							FLog.e(e.getMessage(), e);
							d.showError(e.getMessage());
						}
					}
				});
			}
			
			if (this.negativeListener == null) {
				setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// ignore
					}
				});
			}
			
			return d;
		}
		
	}

	protected PointStyleDialog(Context context) {
		super(context);
	}

}
