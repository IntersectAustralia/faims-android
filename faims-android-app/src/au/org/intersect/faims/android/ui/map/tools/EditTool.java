package au.org.intersect.faims.android.ui.map.tools;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Point;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.style.PointStyle;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import au.org.intersect.faims.android.exceptions.MapException;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.form.MapToggleButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

public class EditTool extends MapTool {

	protected static final int HIGHLIGHT_COLOR = 0x9900FFFF;

	protected static final float HIGHLIGHT_SIZE = 1f;
	
	private MapButton styleButton;

	public EditTool(Context context, CustomMapView mapView, String name) {
		super(context, mapView, name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public View getUI() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	
	private MapButton createStyleButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Style Point");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				final Point point = (Point) mapView.getSelectedGeometry();
				if (point == null) {
					FLog.e("No point selected to style");
					showError(context, "No point selected to style");
					return;
				}
				
				final PointStyle style = point.getStyleSet().getFirstZoomStyle();
				
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Style Settings");
				
				LinearLayout layout = new LinearLayout(context);
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				
				final EditText colorSetter = addSetter(context, layout, "Color:", Integer.toHexString(style.color.colorInt));
				final SeekBar sizeBar = addSlider(context, layout, "Size:", 0);
				final SeekBar pickingSizeBar = addSlider(context, layout, "Picking Size:", style.pickingSize);
				
				builder.setView(layout);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = parseColor(colorSetter.getText().toString());
							float size = parseSize(sizeBar.getProgress());
							float pickingSize = parseSize(pickingSizeBar.getProgress());
							
							mapView.restylePoint(point, createPointStyleSet(color, size, pickingSize));
						} catch (Exception e) {
							showError(context, e.getMessage());
						}
					}
				});
				
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// ignore
					}
				});
				
				builder.create().show();
			}
				
		});
		
		return button;
	}
	
	private void drawHighlight() {
		CanvasLayer layer = (CanvasLayer) mapView.getSelectedLayer();
		if (layer == null) {
			FLog.e("No layer selected");
			showError(context, "No layer selected");
			return;
		}
		
		Point point = (Point) mapView.getSelectedGeometry();
		if (point == null) {
			FLog.e("No point selected to style");
			showError(context, "No point selected to style");
			return;
		}
		MapPos p = point.getMapPos();
		CreatePointTool.this.highlightId = mapView.drawPoint(layer, (new EPSG3857()).toWgs84(p.x, p.y), createPointStyleSet(HIGHLIGHT_COLOR, HIGHLIGHT_SIZE, HIGHLIGHT_SIZE));
	}
	
	private void clearHighlight() {
		if (highlightId > 0) {
			mapView.clearGeometry(highlightId);
			highlightId = 0;
		}
	}
	
	private int parseColor(String value) throws Exception {
		Integer color = (int) Long.parseLong(value, 16);
		if (color == null) {
			throw new MapException("Invalid color specified");
		}
		return color;
	}
	
	private float parseSize(int value) throws Exception {
		if (value < 0 || value > 100) {
			throw new MapException("Invalid size");
		}
		
		return ((float) value) / 100;
	}
	

	
	private MapToggleButton createLockButton(final Context context) {
		final MapToggleButton button = new MapToggleButton(context);
		button.setText("Lock Geometry");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Point point = (Point) mapView.getSelectedGeometry();
				if (point == null) {
					FLog.e("No point selected to style");
					showError(context, "No point selected to style");
					return;
				}
				
				mapView.drawGeometrOverlay(point);
			}
			
		});
		return button;
	}
	*/
}
