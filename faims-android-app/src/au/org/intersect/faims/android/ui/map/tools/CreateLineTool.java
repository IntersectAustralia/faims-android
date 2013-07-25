package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;
import java.util.LinkedList;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.ui.dialog.LineStyleDialog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.projections.EPSG3857;

public class CreateLineTool extends SettingsTool {
	
	public static final String NAME = "Create Line";

	private MapButton createButton;

	private LinkedList<Point> pointsList;

	private MapButton undoButton;
	
	private GeometryStyle style;
	
	private LineStyleDialog styleDialog;

	private MapButton plotButton;

	public CreateLineTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		style = GeometryStyle.defaultLineStyle();
		
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
		
		if (createButton != null) layout.addView(createButton);
		if (undoButton != null) layout.addView(undoButton);
		if (plotButton != null) layout.addView(plotButton);
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
	public void onLayersChanged() {
		clearPoints();
		super.onLayersChanged();
	}

	private void showNotCanvasLayerError() {
		clearPoints();
		showError("The selected layer is not canvas layer");
	}
	
	private void clearLastPoint() {
		if (pointsList.isEmpty()) return;
		
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
		
		try {
			mapView.clearGeometryList(pointsList);
		} catch (Exception e) {
			FLog.e("error clearing points", e);
			showError(e.getMessage());
		}
		
		pointsList.clear();
	}
	
	private void drawLine() {
		if(!(mapView.getSelectedLayer() instanceof CanvasLayer)) {
			showNotCanvasLayerError();
			return;
		}
		
		if (pointsList.size() < 2) {
			showError("Line requires at least 2 points");
			return;
		}
		
		// convert points to map positions
		ArrayList<MapPos> positions = new ArrayList<MapPos>();
		for (Point p : pointsList) {
			positions.add((new EPSG3857().toWgs84(p.getMapPos().x, p.getMapPos().y)));
		}
		
		try {
			mapView.notifyGeometryCreated(mapView.drawLine(mapView.getSelectedLayer(), positions, createLineStyle()));
		} catch (Exception e) {
			FLog.e("error drawing line", e);
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
				MapPos gpsPoint = CreateLineTool.this.mapView.getCurrentPosition();
				if (gpsPoint != null) {
					if(!(mapView.getSelectedLayer() instanceof CanvasLayer)) {
						showNotCanvasLayerError();
						return;
					}
					
					// make point color solid
					try {
						pointsList.add(mapView.drawPoint(mapView.getVertexLayerId(), gpsPoint, createGuidePointStyle()));
					} catch (Exception e) {
						FLog.e("error drawing point", e);
						showError(e.getMessage());
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
				LineStyleDialog.Builder builder = new LineStyleDialog.Builder(context, style);
				styleDialog = (LineStyleDialog) builder.create();
				styleDialog.show();
			}
				
		});
		
		return button;
	}
	
	@Override
	public void onMapClicked(double x, double y, boolean z) {
		if(!(mapView.getSelectedLayer() instanceof CanvasLayer)) {
			showNotCanvasLayerError();
			return;
		}
		
		// make point color solid
		try {
			pointsList.add(mapView.drawPoint(mapView.getVertexLayerId(), (new EPSG3857()).toWgs84(x, y), createGuidePointStyle()));
		} catch (Exception e) {
			FLog.e("error drawing point", e);
			showError(e.getMessage());
		}
	}
	
	private GeometryStyle createLineStyle() {
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
