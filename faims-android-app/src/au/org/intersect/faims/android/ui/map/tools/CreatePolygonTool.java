package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;
import java.util.LinkedList;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.ui.dialog.PolygonStyleDialog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.layers.Layer;
import com.nutiteq.projections.EPSG3857;

public class CreatePolygonTool extends BaseGeometryTool {
	
	public static final String NAME = "Create Polygon";
	
	private MapButton createButton;

	private LinkedList<Point> pointsList;

	private MapButton undoButton;
	
	private GeometryStyle style;
	
	private PolygonStyleDialog styleDialog;

	private MapButton plotButton;

	public CreatePolygonTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		style = GeometryStyle.defaultPolygonStyle();
		
		createButton = createCreateButton(context);
		undoButton = createUndoButton(context);
		plotButton = createPlotButton(context);
		
		pointsList = new LinkedList<Point>();
		
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
		if (plotButton != null) layout.addView(plotButton);
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
		
		Point p = pointsList.removeLast();
		
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
	
	private void drawPolygon() {
		CanvasLayer layer = (CanvasLayer) mapView.getSelectedLayer();
		if (layer == null) {
			showLayerNotFoundError();
			return;
		}
		
		if (pointsList.size() < 3) {
			showError("Polygon requires at least 3 points");
			return;
		}
		
		// convert points to map positions
		ArrayList<MapPos> positions = new ArrayList<MapPos>();
		for (Point p : pointsList) {
			positions.add((new EPSG3857().toWgs84(p.getMapPos().x, p.getMapPos().y)));
		}
		
		try {
			mapView.drawPolygon(layer, positions, createPolygonStyle());
		} catch (Exception e) {
			FLog.e("error drawing polygon", e);
			showError(e.getMessage());
		}
		
		clearPoints();
	}
	
	private MapButton createPlotButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Plot GPS");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				MapPos gpsPoint = CreatePolygonTool.this.mapView.getCurrentPosition();
				if (gpsPoint != null) {
					CanvasLayer layer = (CanvasLayer) mapView.getSelectedLayer();
					if (layer == null) {
						showLayerNotFoundError();
						return;
					}
					
					try {
						pointsList.add(mapView.drawPoint(layer, gpsPoint, createGuidePointStyle()));
					} catch (Exception e) {
						FLog.e("error drawing point", e);
					}
				} else {
					showError("No GPS Signal");
				}
			}
			
		});
		return button;
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
				drawPolygon();
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
				PolygonStyleDialog.Builder builder = new PolygonStyleDialog.Builder(context, style);
				styleDialog = (PolygonStyleDialog) builder.create();
				styleDialog.show();
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
		
		try {
			pointsList.add(mapView.drawPoint(layer, (new EPSG3857()).toWgs84(x, y), createGuidePointStyle()));
		} catch (Exception e) {
			FLog.e("error drawing point", e);
		}
	}
	
	private GeometryStyle createPolygonStyle() {
		return style.cloneStyle();
	}
	
	private GeometryStyle createGuidePointStyle() {
		GeometryStyle s = new GeometryStyle(style.minZoom);
		s.pointColor = style.pointColor | 0xFF000000;
		s.size = style.size;
		s.pickingSize = style.pickingSize;
		return s;
	}

	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
	}
}
