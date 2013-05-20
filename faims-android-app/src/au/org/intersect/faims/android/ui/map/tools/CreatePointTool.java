package au.org.intersect.faims.android.ui.map.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.geometry.VectorElement;
import com.nutiteq.projections.EPSG3857;

public class CreatePointTool extends BaseGeometryTool {
	
	private static final int MAX_ZOOM = 18;
	
	public static final String NAME = "Create Point";
	
	private int color = 0xAAFF0000;
	private float size = 0.2f;
	private float pickingSize = 0.6f;
	private int minZoom = 12;

	public CreatePointTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
	}

	@Override
	protected MapButton createSettingsButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Style Tool");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Style Settings");
				
				ScrollView scrollView = new ScrollView(context);
				LinearLayout layout = new LinearLayout(context);
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				scrollView.addView(layout);
				
				final SeekBar zoomBar = addRange(context, layout, "Min Zoom:", minZoom, MAX_ZOOM);
				final EditText colorSetter = addEdit(context, layout, "Point Color:", Integer.toHexString(color));
				final SeekBar sizeBar = addSlider(context, layout, "Point Size:", size);
				final SeekBar pickingSizeBar = addSlider(context, layout, "Point Picking Size:", pickingSize);
				
				builder.setView(scrollView);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int minZoom = parseRange(zoomBar.getProgress(), MAX_ZOOM);
							int color = parseColor(colorSetter.getText().toString());
							float size = parseSlider(sizeBar.getProgress());
							float pickingSize = parseSlider(pickingSizeBar.getProgress());
							
							CreatePointTool.this.minZoom = minZoom;
							CreatePointTool.this.color = color;
							CreatePointTool.this.size = size;
							CreatePointTool.this.pickingSize = pickingSize;
						} catch (Exception e) {
							showError(e.getMessage());
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
	
	@Override
	public void onMapClicked(double x, double y, boolean z) {
		CanvasLayer layer = (CanvasLayer) mapView.getSelectedLayer();
		if (layer == null) {
			setSelectedLayer(null);
			showError("No layer selected");
			return;
		}
		
		try {
			mapView.drawPoint(layer, (new EPSG3857()).toWgs84(x, y), createPointStyle());
		} catch (Exception e) {
			FLog.e("error drawing point", e);
			showError(e.getMessage());
		}
	}

	private GeometryStyle createPointStyle() {
		GeometryStyle style = new GeometryStyle(minZoom);
		style.pointColor = color;
		style.size = size;
		style.pickingSize = pickingSize;
		return style;
	}

	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public float getSize() {
		return size;
	}

	public void setSize(float size) {
		this.size = size;
	}

	public float getPickingSize() {
		return pickingSize;
	}

	public void setPickingSize(float pickingSize) {
		this.pickingSize = pickingSize;
	}
	
}
