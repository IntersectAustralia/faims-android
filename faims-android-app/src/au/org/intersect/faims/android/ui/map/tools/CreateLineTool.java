package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.layers.Layer;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.StyleSet;

public class CreateLineTool extends BaseGeometryTool {
	
	public static final String NAME = "Create Line";
	
	private int color = 0xFFFFFFFF;
	private float size = 0.3f;
	private float pickingSize = 0.3f;
	private float width = 0.1f;
	private float pickingWidth = 0.1f;

	private MapButton createButton;

	private ArrayList<Integer> pointsList;

	private MapButton clearButton;

	public CreateLineTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		createButton = createCreateButton(context);
		clearButton = createClearButton(context);
		
		pointsList = new ArrayList<Integer>();
		
		updateLayout();
	}
	
	@Override
	protected void updateLayout() {
		layout.removeAllViews();
		layout.addView(settingsButton);
		layout.addView(selectLayerButton);
		if (createButton != null) layout.addView(createButton);
		if (clearButton != null) layout.addView(clearButton);
		layout.addView(selectedLayer);
	}
	
	@Override
	public void activate() {
		super.activate();
		clearPoints();
	}
	
	@Override
	public void deactivate() {
		super.deactivate();
		clearPoints();
	}
	
	@Override
	protected void setSelectedLayer(Layer layer) {
		super.setSelectedLayer(layer);
		clearPoints();
	}
	
	private void clearPoints() {
		if (pointsList.isEmpty()) return;
		
		CanvasLayer layer = (CanvasLayer) mapView.getSelectedLayer();
		if (layer == null) {
			showError(context, "No layer selected");
			return;
		}
		
		mapView.clearGeometryList(layer, pointsList);
		
		pointsList.clear();
	}
	
	private void drawLine() {
		CanvasLayer layer = (CanvasLayer) mapView.getSelectedLayer();
		if (layer == null) {
			showError(context, "No layer selected");
			return;
		}
		
		if (pointsList.size() < 2) {
			showError(context, "Line requires atleast 2 points");
			return;
		}
		
		// convert points to map positions
		ArrayList<MapPos> positions = new ArrayList<MapPos>();
		for (Integer id : pointsList) {
			Point p = (Point) mapView.getGeometry(id);
			positions.add((new EPSG3857().toWgs84(p.getMapPos().x, p.getMapPos().y)));
		}
		
		mapView.drawLine(layer, positions, createLineStyleSet(color, width, pickingWidth));
		
		clearPoints();
	}
	
	private MapButton createClearButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Clear Points");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clearPoints();
			}
			
		});
		return button;
	}
	
	private MapButton createCreateButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Create Line");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				drawLine();
			}
			
		});
		
		return button;
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
				final SeekBar sizeBar = addSlider(context, layout, "Size:", getSize());
				final SeekBar pickingSizeBar = addSlider(context, layout, "Picking Size:", getPickingSize());
				final SeekBar widthBar = addSlider(context, layout, "Width:", width);
				final SeekBar pickingWidthBar = addSlider(context, layout, "Picking Width:", pickingWidth);
				
				builder.setView(layout);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = parseColor(colorSetter.getText().toString());
							float size = parseSize(sizeBar.getProgress());
							float pickingSize = parseSize(pickingSizeBar.getProgress());
							float width = parseSize(widthBar.getProgress());
							float pickingWidth = parseSize(pickingWidthBar.getProgress());
							
							CreateLineTool.this.color = color;
							CreateLineTool.this.setSize(size);
							CreateLineTool.this.setPickingSize(pickingSize);
							CreateLineTool.this.width = width;
							CreateLineTool.this.pickingWidth = pickingWidth;
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
			showError(context, "No layer selected");
			return;
		}
		
		pointsList.add(mapView.drawPoint(layer, (new EPSG3857()).toWgs84(x, y), createPointStyleSet(color, getSize(), getPickingSize())));
	}
	
	private StyleSet<LineStyle> createLineStyleSet(int c, float w,
			float pw) {
		StyleSet<LineStyle> lineStyleSet = new StyleSet<LineStyle>();
		LineStyle style = LineStyle.builder().setColor(c).setWidth(w).setPickingWidth(pw).build();
		lineStyleSet.setZoomStyle(0, style);
		return lineStyleSet;
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
	
	public float getWidth() {
		return width;
	}

	public void setWidth(float width) {
		this.width = width;
	}

	public float getPickingWidth() {
		return pickingWidth;
	}

	public void setPickingWidth(float pickingWidth) {
		this.pickingWidth = pickingWidth;
	}
	
}
