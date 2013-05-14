package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.location.Location;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CustomPoint;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.util.MeasurementUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

public class AzimuthTool extends SelectTool {
	
	private class AzimuthToolCanvas extends ToolCanvas {

		private MapPos tp1;
		private MapPos tp2;
		private float angle;
		private float textX;
		private float textY;
		private RectF rectF;
		private MapPos tp3;

		public AzimuthToolCanvas(Context context) {
			super(context);
		}
		
		@Override
		public void onDraw(Canvas canvas) {
			if (tp1 != null && tp2 != null) {
				// north line
				canvas.drawLine((float) tp1.x, (float) tp1.y, (float) tp1.x, (float) tp3.y, paint);
				// line to point
				canvas.drawLine((float) tp1.x, (float) tp1.y, (float) tp2.x, (float) tp2.y, paint);
				// angle
				canvas.drawArc(rectF, 0, angle, true, paint);
				
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
			this.tp3 = GeometryUtil.transformVertex(new MapPos(p1.x, p2.y), AzimuthTool.this.mapView, true);
			this.angle = AzimuthTool.this.computeAzimuth(GeometryUtil.convertToWgs84(p1), GeometryUtil.convertToWgs84(p2));
			
			float dx = (float) (tp1.x - tp2.x) / 2;
			float dy = (float) (tp1.y - tp2.y) / 2;
			
			this.rectF = new RectF((float) tp1.x - dx, (float) tp1.y - dy, (float) tp1.x + dx, (float) tp1.y + dy);
			
			if (tp1.x > tp2.x) {
				MapPos t = tp2;
				tp2 = tp1;
				tp1 = t;
			}
			
			float midX = (float) (tp1.x + tp2.x) / 2;
			float midY = (float) (tp1.y + tp2.y) / 2;
			
			float offset = ScaleUtil.getDip(this.getContext(), DEFAULT_OFFSET);
			
			textX = midX + offset;
			
			if (tp1.y > tp2.y){
				textY = midY + offset;
			} else {
				textY = midY - offset;
			}
			
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
				if ((element instanceof CustomPoint) && (mapView.getSelection().size() < 2)) {
					CustomPoint p = (CustomPoint) element;
					
					if (mapView.hasSelection(p)) {
						mapView.removeSelection(p);
					} else {
						mapView.addSelection(p);
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
		if (mapView.getSelection().size() < 2) return;
		
		MapPos p1 = ((CustomPoint) mapView.getSelection().get(0)).getMapPos();
		MapPos p2 = ((CustomPoint) mapView.getSelection().get(1)).getMapPos();
		
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
		
		return l1.bearingTo(l2);
	}

}
