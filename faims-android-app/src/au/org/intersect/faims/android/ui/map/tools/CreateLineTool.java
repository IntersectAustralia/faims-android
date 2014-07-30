package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.ui.dialog.LineStyleDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.button.FinishButton;
import au.org.intersect.faims.android.ui.map.button.PlotGPSButton;
import au.org.intersect.faims.android.ui.map.button.SettingsButton;
import au.org.intersect.faims.android.ui.map.button.ToolBarButton;
import au.org.intersect.faims.android.ui.map.button.UndoButton;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.projections.EPSG3857;

public class CreateLineTool extends SettingsTool {
	
	public static final String NAME = "Create Line";

	private FinishButton createButton;

	private LinkedList<Point> pointsList;

	private UndoButton undoButton;
	
	private GeometryStyle style;
	
	private LineStyleDialog styleDialog;

	private PlotGPSButton plotButton;

	public CreateLineTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		style = GeometryStyle.defaultLineStyle();
		
		createButton = createCreateButton(context);
		RelativeLayout.LayoutParams createParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		createParams.alignWithParent = true;
		createParams.addRule(RelativeLayout.ALIGN_RIGHT);
		createParams.addRule(RelativeLayout.ALIGN_BOTTOM);
		createParams.bottomMargin = (int) ScaleUtil.getDip(context, BOTTOM_MARGIN); 
		createButton.setLayoutParams(createParams);
		undoButton = createUndoButton(context);
		RelativeLayout.LayoutParams undoParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		undoParams.alignWithParent = true;
		undoParams.addRule(RelativeLayout.ALIGN_RIGHT);
		undoParams.topMargin = (int) ScaleUtil.getDip(context,TOP_MARGIN);
		undoButton.setLayoutParams(undoParams);
		plotButton = createPlotButton(context);
		RelativeLayout.LayoutParams plotGPSParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		plotGPSParams.alignWithParent = true;
		plotGPSParams.addRule(RelativeLayout.ALIGN_LEFT);
		plotGPSParams.addRule(RelativeLayout.ALIGN_BOTTOM);
		plotGPSParams.bottomMargin = (int) ScaleUtil.getDip(context, BOTTOM_MARGIN);
		plotButton.setLayoutParams(plotGPSParams);
		
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
		if (plotButton != null) layout.addView(plotButton);
		if (undoButton != null) layout.addView(undoButton);
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
	
	private PlotGPSButton createPlotButton(final Context context) {
		PlotGPSButton button = new PlotGPSButton(context);
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
	
	private UndoButton createUndoButton(final Context context) {
		UndoButton button = new UndoButton(context);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clearLastPoint();
			}
			
		});
		return button;
	}
	
	private FinishButton createCreateButton(final Context context) {
		FinishButton button = new FinishButton(context);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				drawLine();
			}
			
		});
		
		return button;
	}

	@Override
	protected SettingsButton createSettingsButton(final Context context) {
		SettingsButton button = new SettingsButton(context);
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
	
	@Override
	public ToolBarButton getButton(Context context) {
		ToolBarButton button = new ToolBarButton(context);
		button.setLabel("Line");
		button.setSelectedState(R.drawable.tools_line_s);
		button.setNormalState(R.drawable.tools_line);
		return button;
	}

	public void setStyle(GeometryStyle style) {
		this.style = style;
	}

	public void saveToJSON(JSONObject json) {
		try {
			json.put("name", name);
			JSONObject styleSettings = new JSONObject();
			style.saveToJSON(styleSettings);
			json.put("style", styleSettings);
		} catch (JSONException e) {
			FLog.e("Couldn't serialize CreateLineTool", e);
		}
	}
}
