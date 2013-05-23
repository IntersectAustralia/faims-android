package au.org.intersect.faims.android.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;

public class LineStyleDialog extends StyleDialog {

	public static class Builder extends StyleDialog.Builder {
		
		public Builder(Context context, GeometryStyle style) {
			super(context, style);
		}
		
		@Override
		public SettingsDialog createDialog() {
			final LineStyleDialog d = new LineStyleDialog(context);
			final GeometryStyle style = this.style;
			d.setStyle(style);
			
			setTitle("Style Settings");
			
			addRange("minZoom", "Min Zoom:", style.minZoom, 0, FaimsSettings.MAX_ZOOM);
			addTextField("color", "Line Color:", Integer.toHexString(style.lineColor));
			addSlider("size", "Point Size:", style.size);
			addSlider("pickingSize", "Point Picking Size:", style.pickingSize);
			addSlider("width", "Line Width:", style.width);
			addSlider("pickingWidth", "Line Picking Width:", style.pickingWidth);
			addCheckBox("showPoints", "Show Points on Line:", style.showPoints);
			
			if (this.positiveListener == null) {
				setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int minZoom = d.parseRange("minZoom", 0, FaimsSettings.MAX_ZOOM);
							int color = d.parseColor("color");
							float size = d.parseSlider("size");
							float pickingSize = d.parseSlider("pickingSize");
							float width = d.parseSlider("width");
							float pickingWidth = d.parseSlider("pickingWidth");
							boolean showPoints = d.parseCheckBox("showPoints");
							
							style.minZoom = minZoom;
							style.pointColor = color;
							style.lineColor = color;
							style.size = size;
							style.pickingSize = pickingSize;
							style.width = width;
							style.pickingWidth = pickingWidth;
							style.showPoints = showPoints;
						} catch (Exception e) {
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
	
	
	protected LineStyleDialog(Context context) {
		super(context);
	}

}
