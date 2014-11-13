package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.graphics.Canvas;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.button.ToolBarButton;
import au.org.intersect.faims.android.util.GeometryUtil;
import au.org.intersect.faims.android.util.MeasurementUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.google.inject.Inject;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.VectorElement;

public class LineDistanceTool extends HighlightTool {
	
	@Inject
	DatabaseManager databaseManager;
	
	private class LineDistanceToolCanvas extends ToolCanvas {
		
		private float textX;
		private float textY;
		
		private boolean showKm;
		
		public LineDistanceToolCanvas(Context context) {
			super(context);
		}

		@Override
		public void onDraw(Canvas canvas) {
			if (isDirty) {
				
				if (showKm) {
					canvas.drawText(MeasurementUtil.displayAsKiloMeters(LineDistanceTool.this.distance/1000), textX, textY, textPaint);
				} else {
					canvas.drawText(MeasurementUtil.displayAsMeters(LineDistanceTool.this.distance), textX, textY, textPaint);
				}
				
			}
		}

		public void drawDistance(Line line) {
			float offset = ScaleUtil.getDip(this.getContext(), DEFAULT_OFFSET);
			
			MapPos p = GeometryUtil.transformVertex(line.getVertexList().get(line.getVertexList().size()-1), LineDistanceTool.this.mapView, true);
			
			textX = (float) p.x + offset;
			textY = (float) p.y + offset;
			
			this.isDirty = true;
			invalidate();
		}
		
		public void setShowKm(boolean value) {
			showKm = value;
		}
		
	}
	
	public static final String NAME = "Line Distance";
	
	private LineDistanceToolCanvas canvas;

	protected SettingsDialog settingsDialog;
	
	private float distance;

	public LineDistanceTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		canvas = new LineDistanceToolCanvas(context);
		container.addView(canvas);
	}

	@Override 
	public void activate() {
		super.activate();
		canvas.clear();
	}
	
	@Override
	public void deactivate() {
		super.deactivate();
		canvas.clear();
	}
	
	@Override
	public void onLayersChanged() {
		super.onLayersChanged();
		canvas.clear();
	}
	
	@Override
	public void onMapChanged() {
		super.onMapChanged();
		drawDistance();
	}
	
	@Override
	protected void updateLayout() {
		super.updateLayout();
		if (canvas != null) layout.addView(canvas);
	}
	
	@Override
	protected void clearSelection() {
		super.clearSelection();
		canvas.clear();
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (element instanceof Geometry) {
			try {
				if ((element instanceof Line) && (mapView.getHighlights().size() < 1)) {
					Line p = (Line) element;
					
					if (mapView.hasHighlight(p)) {
						mapView.removeHighlight(p);
					} else {
						mapView.addHighlight(p);
					}
					
					calculateDistance();
					drawDistance();
				}
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}
	
	private void calculateDistance() {
		try {
			if (mapView.getHighlights().size() < 1) return;
			
			Line line = (Line) mapView.getHighlights().get(0);
			this.distance = databaseManager.spatialRecord().computeLineDistance(GeometryUtil.convertToWgs84(line.getVertexList()), mapView.getModuleSrid());
			
		} catch (Exception e) {
			FLog.e("error calculating line distance", e);
			showError("Error calculating line distance");
		}
	}
	
	private void drawDistance() {
		try {
			if (mapView.getHighlights().size() < 1) return;
			
			Line line = (Line) mapView.getHighlights().get(0);
			
			canvas.setColor(mapView.getDrawViewColor());
			canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
			canvas.setTextSize(mapView.getDrawViewTextSize());
			canvas.setShowKm(mapView.showKm());
			canvas.drawDistance(line);
		} catch (Exception e) {
			FLog.e("error drawing line distance", e);
		}
	}
	
	public ToolBarButton getButton(Context context) {
		ToolBarButton button = new ToolBarButton(context);
		button.setLabel("Line");
		button.setSelectedState(R.drawable.tools_distance_line_s);
		button.setNormalState(R.drawable.tools_distance_line);
		return button;
	}
	
}
