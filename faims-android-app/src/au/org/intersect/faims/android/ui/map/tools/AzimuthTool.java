package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.util.MeasurementUtil;
import au.org.intersect.faims.android.util.ScaleUtil;
import au.org.intersect.faims.android.util.SpatialiteUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;

public class AzimuthTool extends HighlightTool {
	
	private class AzimuthToolCanvas extends ToolCanvas {

		private MapPos tp1;
		private MapPos tp2;
		private float angle;
		private float textX;
		private float textY;
		private RectF rectF;

		public AzimuthToolCanvas(Context context) {
			super(context);
		}
		
		@Override
		public void onDraw(Canvas canvas) {
			if (isDirty) {
				// line to point
				canvas.drawLine((float) tp1.x, (float) tp1.y, (float) tp2.x, (float) tp2.y, paint);
				// angle
				canvas.drawArc(rectF, AzimuthTool.this.mapView.getRotation()-90, angle, true, paint);
				// text
				canvas.drawText(MeasurementUtil.displayAsDegrees(angle), textX, textY, textPaint);
			}
		}

		public void drawAzimuthFrom(MapPos p1, MapPos p2) {
			
			MapPos pp1 = GeometryUtil.convertToWgs84(p1);
			MapPos pp2 = GeometryUtil.convertToWgs84(p2);
			
			this.tp1 = GeometryUtil.transformVertex(p1, AzimuthTool.this.mapView, true);
			this.tp2 = GeometryUtil.transformVertex(p2, AzimuthTool.this.mapView, true);
			
			this.angle = SpatialiteUtil.computeAzimuth(pp1, pp2);
			
			float dx = (float) (tp2.x - tp1.x);
			float dy = (float) (tp2.y - tp1.y);
			float d = (float) Math.sqrt(dx * dx + dy * dy) / 2;
			
			this.rectF = new RectF((float) tp1.x - d, (float) tp1.y - d, (float) tp1.x + d, (float) tp1.y + d);
			
			float offset = ScaleUtil.getDip(this.getContext(), DEFAULT_OFFSET);
			
			textX = (float) tp2.x + offset;
			textY = (float) tp2.y + offset;
			
			isDirty = true;
			this.invalidate();
		}
		
	}
	
	public static final String NAME = "Azimuth";
	
	private AzimuthToolCanvas canvas;

	public AzimuthTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		canvas = new AzimuthToolCanvas(context);
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
		drawAzimuth();
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
				if ((element instanceof Point) && (mapView.getHighlights().size() < 2)) {
					Point p = (Point) element;
					
					if (mapView.hasHighlight(p)) {
						mapView.removeHighlight(p);
					} else {
						mapView.addHighlight(p);
					}
					
					drawAzimuth();
				}
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}

	private void drawAzimuth() {
		if (mapView.getHighlights().size() < 2) return;
		
		MapPos p1 = ((Point) mapView.getHighlights().get(0)).getMapPos();
		MapPos p2 = ((Point) mapView.getHighlights().get(1)).getMapPos();
		
		canvas.setColor(mapView.getDrawViewColor());
		canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
		canvas.setTextSize(mapView.getDrawViewTextSize());
		canvas.drawAzimuthFrom(p1, p2);
	}

}
