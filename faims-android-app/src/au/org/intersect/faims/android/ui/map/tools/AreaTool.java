package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.graphics.Canvas;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.button.ToolBarButton;
import au.org.intersect.faims.android.util.MeasurementUtil;
import au.org.intersect.faims.android.util.ScaleUtil;
import au.org.intersect.faims.android.util.SpatialiteUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.geometry.VectorElement;

public class AreaTool extends HighlightTool {
	
	private class AreaToolCanvas extends ToolCanvas {
		
		private float textX;
		private float textY;
		private boolean showKm;
		
		public AreaToolCanvas(Context context) {
			super(context);
		}

		@Override
		public void onDraw(Canvas canvas) {
			if (isDirty) {
				
				if (showKm) {
					canvas.drawText(MeasurementUtil.displayAsKiloMeters(AreaTool.this.area/(1000*1000)) + "\u00B2", textX, textY, textPaint);
				} else {
					canvas.drawText(MeasurementUtil.displayAsMeters(AreaTool.this.area) + "\u00B2", textX, textY, textPaint);
				}
				
			}
		}
		
		public void drawArea(Polygon polygon) {
			float offset = ScaleUtil.getDip(this.getContext(), DEFAULT_OFFSET);
			
			MapPos p = GeometryUtil.transformVertex(polygon.getVertexList().get(polygon.getVertexList().size()-1), AreaTool.this.mapView, true);
			
			textX = (float) p.x + offset;
			textY = (float) p.y + offset;
			
			this.isDirty = true;
			invalidate();
		}
		
		public void setShowKm(boolean value) {
			showKm = value;
		}
		
	}
	
	public static final String NAME = "Area";
	
	private AreaToolCanvas canvas;
	
	private float area;

	public AreaTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		canvas = new AreaToolCanvas(context);
		container.addView(canvas);
	}
	
	@Override 
	public void activate() {
		super.activate();
		canvas.clear();
		if (!mapView.isProperProjection()) {
			showError("This tool will not function properly as projection is not a projected coordinate system.");
		}
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
		drawArea();
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
				if ((element instanceof Polygon) && (mapView.getHighlights().size() < 1)) {
					Polygon p = (Polygon) element;
					
					if (mapView.hasHighlight(p)) {
						mapView.removeHighlight(p);
					} else {
						mapView.addHighlight(p);
					}
					
					calculateArea();
					drawArea();
				}
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}
	
	private void calculateArea() {
		try {
			if (mapView.getHighlights().size() < 1) return;
			Polygon polygon = (Polygon) mapView.getHighlights().get(0);
			area = (float) SpatialiteUtil.computePolygonArea((Polygon) GeometryUtil.convertGeometryToWgs84(polygon), mapView.getModuleSrid());
		} catch (Exception e) {
			FLog.e("error computing area of polygon", e);
			showError("Error computing area of polygon");
		}
	}
	
	private void drawArea() {
		try {
			if (mapView.getHighlights().size() < 1) return;
			
			Polygon polygon = (Polygon) mapView.getHighlights().get(0);
			if (polygon == null) return;
			
			canvas.setColor(mapView.getDrawViewColor());
			canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
			canvas.setTextSize(mapView.getDrawViewTextSize());
			canvas.setShowKm(mapView.showKm());
			canvas.drawArea(polygon);
		} catch (Exception e) {
			FLog.e("error drawing area", e);
		}
	}
	
	public ToolBarButton getButton(Context context) {
		ToolBarButton button = new ToolBarButton(context);
		button.setLabel("Area");
		button.setMutatedSelectedState(R.drawable.tools_area);
		button.setNormalState(R.drawable.tools_area);
		return button;
	}

}
