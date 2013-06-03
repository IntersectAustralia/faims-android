package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.location.Location;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.util.MeasurementUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.projections.EPSG3857;

public class AzimuthTool extends HighlightTool {
	
	private class AzimuthToolCanvas extends ToolCanvas {

		private MapPos tp1;
		private MapPos tp2;
		private float angle;
		private float textX;
		private float textY;
		private RectF rectF;
		private MapPos tp3;
		private float startAngle;

		public AzimuthToolCanvas(Context context) {
			super(context);
		}
		
		@Override
		public void onDraw(Canvas canvas) {
			if (tp1 != null && tp2 != null) {
				// line to point
				canvas.drawLine((float) tp1.x, (float) tp1.y, (float) tp2.x, (float) tp2.y, paint);
				// angle
				canvas.drawArc(rectF, startAngle-90, angle, true, paint);
				
				canvas.drawText(MeasurementUtil.displayAsDegrees(angle), textX, textY, textPaint);
			}
		}

		@Override
		public void clear() {
			tp1 = tp2 = null;
			invalidate();
		}

		public void drawAzimuthFrom(MapPos p1, MapPos p2) {
			this.tp1 = GeometryUtil.transformVertex(p1, AzimuthTool.this.mapView, true);
			this.tp2 = GeometryUtil.transformVertex(p2, AzimuthTool.this.mapView, true);
			
			MapPos pp1 = GeometryUtil.convertToWgs84(p1);
			MapPos pp2 = GeometryUtil.convertToWgs84(p2);
			MapPos p3 = new EPSG3857().fromWgs84(pp1.x, pp1.y + Math.abs(pp2.y - pp1.y));
			
			this.tp3 = GeometryUtil.transformVertex(p3, AzimuthTool.this.mapView, true);
			
			this.angle = AzimuthTool.this.computeAzimuth(pp1, pp2);
			
			float dx = (float) (tp2.x - tp1.x);
			float dy = (float) (tp2.y - tp1.y);
			float d = (float) Math.sqrt(dx * dx + dy * dy) / 2;
			
			this.rectF = new RectF((float) tp1.x - d, (float) tp1.y - d, (float) tp1.x + d, (float) tp1.y + d);
			
			// note: angle between two vectors
			this.startAngle = computeAngleBetween(new MapPos(0, -1), new MapPos(tp3.x - tp1.x, tp3.y - tp1.y));
			
			float offset = ScaleUtil.getDip(this.getContext(), DEFAULT_OFFSET);
			
			textX = (float) tp2.x + offset;
			textY = (float) tp2.y + offset;
			
			this.invalidate();
		}
		
		private float computeAngleBetween(MapPos v1, MapPos v2) {
			float angle = (float) (Math.acos(dot(v1, v2) / (length(v1) * length(v2))) * 180 / Math.PI);
			if (v2.x < 0) return -angle;
			return angle;
		}
		
		private float dot(MapPos p1, MapPos p2) {
			return (float) (p1.x * p2.x + p1.y * p2.y);
		}
		
		private float length(MapPos p) {
			return (float) Math.sqrt(p.x * p.x + p.y * p.y);
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
		
		canvas.drawAzimuthFrom(p1, p2);
		canvas.setColor(mapView.getDrawViewColor());
		canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
		canvas.setTextSize(mapView.getDrawViewTextSize());
	}
	
	private float computeAzimuth(MapPos p1, MapPos p2) {
		Location l1 = new Location("");
		l1.setLatitude(p1.y);
		l1.setLongitude(p1.x);
		
		Location l2 = new Location("");
		l2.setLatitude(p2.y);
		l2.setLongitude(p2.x);
		
		return (l1.bearingTo(l2) + 360) % 360;
	}

}
