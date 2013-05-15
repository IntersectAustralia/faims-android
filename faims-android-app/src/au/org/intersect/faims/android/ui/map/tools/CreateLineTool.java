package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;
import java.util.LinkedList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import au.org.intersect.faims.android.data.GeometryStyle;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.CustomPoint;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.layers.Layer;
import com.nutiteq.projections.EPSG3857;

public class CreateLineTool extends BaseGeometryTool {
	
	public static final String NAME = "Create Line";
	
	private int color = 0xAA00FF00;
	private float size = 0.2f;
	private float pickingSize = 0.6f;
	private float width = 0.05f;
	private float pickingWidth = 0.3f;
	private boolean showPoints;

	private MapButton createButton;

	private LinkedList<CustomPoint> pointsList;

	private MapButton undoButton;

	public CreateLineTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		createButton = createCreateButton(context);
		undoButton = createUndoButton(context);
		
		pointsList = new LinkedList<CustomPoint>();
		
		updateLayout();
	}
	
	@Override
	protected void updateLayout() {
		if (layout != null) {
			layout.removeAllViews();
			layout.addView(settingsButton);
		}
		
		if (selectLayerButton != null) layout.addView(selectLayerButton);
		if (createButton != null) layout.addView(createButton);
		if (undoButton != null) layout.addView(undoButton);
		if (selectedLayer != null) layout.addView(selectedLayer);
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
		clearPoints();
		super.setSelectedLayer(layer);
	}
	
	private void showLayerNotFoundError() {
		clearPoints();
		super.setSelectedLayer(null);
		showError("No layer selected");
	}
	
	private void clearLastPoint() {
		if (pointsList.isEmpty()) return;
		
		CanvasLayer layer = (CanvasLayer) mapView.getSelectedLayer();
		if (layer == null) {
			showLayerNotFoundError();
			return;
		}
		
		CustomPoint p = pointsList.removeLast();
		
		try {
			mapView.clearGeometry(p);
		} catch (Exception e) {
			FLog.e("error clearing point", e);
			showError(e.getMessage());
		}
	}
	
	private void clearPoints() {
		if (pointsList.isEmpty()) return;
		
		CanvasLayer layer = (CanvasLayer) lastLayerSelected;
		if (layer == null) {
			showLayerNotFoundError();
			return;
		}
		
		try {
			mapView.clearGeometryList(pointsList);
		} catch (Exception e) {
			FLog.e("error clearing points", e);
			showError(e.getMessage());
		}
		
		pointsList.clear();
	}
	
	private void drawLine() {
		CanvasLayer layer = (CanvasLayer) mapView.getSelectedLayer();
		if (layer == null) {
			showLayerNotFoundError();
			return;
		}
		
		if (pointsList.size() < 2) {
			showError("Line requires at least 2 points");
			return;
		}
		
		// convert points to map positions
		ArrayList<MapPos> positions = new ArrayList<MapPos>();
		for (CustomPoint p : pointsList) {
			positions.add((new EPSG3857().toWgs84(p.getMapPos().x, p.getMapPos().y)));
		}
		
		try {
			mapView.drawLine(layer, positions, createLineStyle());
		} catch (Exception e) {
			FLog.e("error drawing line", e);
			showError(e.getMessage());
		}
		
		clearPoints();
	}
	
	private MapButton createUndoButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Undo");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clearLastPoint();
			}
			
		});
		return button;
	}
	
	private MapButton createCreateButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Finish");
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
				
				final EditText colorSetter = addSetter(context, layout, "Line Color:", Integer.toHexString(color));
				final SeekBar sizeBar = addSlider(context, layout, "Point Size:", size);
				final SeekBar pickingSizeBar = addSlider(context, layout, "Point Picking Size:", pickingSize);
				final SeekBar widthBar = addSlider(context, layout, "Line Width:", width);
				final SeekBar pickingWidthBar = addSlider(context, layout, "Line Picking Width:", pickingWidth);
				final CheckBox showPointsBox = addCheckBox(context, layout, "Show Points on Line:", showPoints);
				
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
							boolean showPoints = showPointsBox.isChecked();
							
							CreateLineTool.this.color = color;
							CreateLineTool.this.setSize(size);
							CreateLineTool.this.setPickingSize(pickingSize);
							CreateLineTool.this.width = width;
							CreateLineTool.this.pickingWidth = pickingWidth;
							CreateLineTool.this.showPoints = showPoints;
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
			showLayerNotFoundError();
			return;
		}
		
		// make point color solid
		try {
			pointsList.add(mapView.drawPoint(layer, (new EPSG3857()).toWgs84(x, y), createGuidePointStyle()));
		} catch (Exception e) {
			FLog.e("error drawing point", e);
			showError(e.getMessage());
		}
	}
	
	private GeometryStyle createLineStyle() {
		GeometryStyle style = new GeometryStyle();
		style.pointColor = color;
		style.size = size;
		style.pickingSize = pickingSize;
		style.lineColor = color;
		style.width = width;
		style.pickingWidth = pickingWidth;
		style.showPoints = showPoints;
		return style;
	}
	
	private GeometryStyle createGuidePointStyle() {
		GeometryStyle style = new GeometryStyle();
		style.pointColor = color | 0xFF000000;
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
