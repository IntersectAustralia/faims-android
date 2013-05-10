package au.org.intersect.faims.android.ui.map.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.geometry.VectorElement;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.StyleSet;

public class CreatePointTool extends BaseGeometryTool {
	
	public static final String NAME = "Create Point";
	
	private int color = 0xFFFF0000;
	private float size = 0.2f;
	private float pickingSize = 0.4f;

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
				
				LinearLayout layout = new LinearLayout(context);
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				
				final EditText colorSetter = addSetter(context, layout, "Color:", Integer.toHexString(color));
				final SeekBar sizeBar = addSlider(context, layout, "Size:", size);
				final SeekBar pickingSizeBar = addSlider(context, layout, "Picking Size:", pickingSize);
				
				builder.setView(layout);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = parseColor(colorSetter.getText().toString());
							float size = parseSize(sizeBar.getProgress());
							float pickingSize = parseSize(pickingSizeBar.getProgress());
							
							CreatePointTool.this.color = color;
							CreatePointTool.this.size = size;
							CreatePointTool.this.pickingSize = pickingSize;
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
	
	@Override
	public void onMapClicked(double x, double y, boolean z) {
		CanvasLayer layer = (CanvasLayer) mapView.getSelectedLayer();
		if (layer == null) {
			setSelectedLayer(null);
			showError(context, "No layer selected");
			return;
		}
		
		try {
			mapView.drawPoint(layer, (new EPSG3857()).toWgs84(x, y), createPointStyleSet(color, size, pickingSize));
		} catch (Exception e) {
			FLog.e("error drawing point", e);
			showError(context, e.getMessage());
		}
	}

	private StyleSet<PointStyle> createPointStyleSet(int c, float s,
			float ps) {
		StyleSet<PointStyle> pointStyleSet = new StyleSet<PointStyle>();
		PointStyle style = PointStyle.builder().setColor(c).setSize(s).setPickingSize(ps).build();
		pointStyleSet.setZoomStyle(0, style);
		return pointStyleSet;
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
