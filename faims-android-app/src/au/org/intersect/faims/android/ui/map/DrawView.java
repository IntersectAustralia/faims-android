package au.org.intersect.faims.android.ui.map;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryData;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.util.MeasurementUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;

public class DrawView extends View {
	
	protected static final float SCALE_FACTOR = 40.0f;

	protected static final float STROKE_SCALE = 8.0f;
	
	protected static final float TEXT_SCALE = 24.0f;
	
	protected static final float DEFAULT_OFFSET = 20.0f;

	protected Paint paint = new Paint();
	protected Paint textPaint = new Paint();
	
	protected List<Geometry> geometryList;

	protected int color = Color.CYAN;

	protected float strokeSize = 0.5f;
	
	protected float textSize = 0.7f;
	
	protected CustomMapView mapView;

	private boolean showDetail;

	private boolean showDecimal;

	public DrawView(Context context) {
		super(context);
		
		updatePaint();
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		if (geometryList == null) return;
		
		for (Geometry geom : geometryList) {
			drawGeometryOverlay(geom, canvas);
		}
		
		if (showDetail) {
			for (Geometry geom : geometryList) {
				drawGeometryInfo(geom, canvas);
			}
		}
	}
	
	private void drawGeometryOverlay(Geometry geom, Canvas canvas) {
		if (geom instanceof Point) {
			drawPointOverlay((Point) geom, canvas);
		} else if (geom instanceof Line) {
			drawLineOverlay((Line) geom, canvas);
		} else if (geom instanceof Polygon) {
			drawPolygonOverlay((Polygon) geom, canvas);
		}
	}
	
	private void drawGeometryInfo(Geometry geom, Canvas canvas) {
		if (geom instanceof Point) {
			drawPointInfo((Point) geom, canvas);
		} else if (geom instanceof Line) {
			drawLineInfo((Line) geom, canvas);
		} else if (geom instanceof Polygon) {
			drawPolygonInfo((Polygon) geom, canvas);
		}
	}
	
	private void drawPointOverlay(Point point, Canvas canvas) {
		GeometryData data = (GeometryData) point.userData;
		float size = ScaleUtil.getDip(this.getContext(), data.style.size * SCALE_FACTOR);
		MapPos p = transformPoint(point.getMapPos());
		canvas.drawCircle((float) p.x, (float) p.y, size, paint);
	}
	
	private void drawLineOverlay(Line line, Canvas canvas) {
		MapPos lp = null;
		for (MapPos p : line.getVertexList()) {
			p = transformPoint(p);
			if (lp != null) {
				GeometryData data = (GeometryData) line.userData;
				float size = ScaleUtil.getDip(this.getContext(), data.style.width * SCALE_FACTOR);
				paint.setStrokeWidth(size);
				canvas.drawLine((float) lp.x, (float) lp.y, (float) p.x, (float) p.y, paint);
			}
			lp = p;
		}
		paint.setStrokeWidth(ScaleUtil.getDip(this.getContext(), strokeSize * STROKE_SCALE));
	}
	
	private void drawPolygonOverlay(Polygon polygon, Canvas canvas) {
		MapPos lp = null;
		for (MapPos p : polygon.getVertexList()) {
			p = transformPoint(p);
			if (lp != null) {
				canvas.drawLine((float) lp.x, (float) lp.y, (float) p.x, (float) p.y, paint);
			}
			lp = p;
		}
		MapPos p = polygon.getVertexList().get(0);
		p = transformPoint(p);
		canvas.drawLine((float) lp.x, (float) lp.y, (float) p.x, (float) p.y, paint);
	}
	
	private void drawPointInfo(Point point, Canvas canvas) {
		try {
			GeometryData data = (GeometryData) point.userData;
			float offset = getPosOffset(data.style);
			MapPos p = transformPoint(point.getMapPos());
			drawPosInfo(p, pointToText(projectPoint(point.getMapPos())), offset, -offset, canvas);
		} catch (Exception e) {
			FLog.e("error drawing point info", e);
		}
	}
	
	private void drawLineInfo(Line line, Canvas canvas) {
		try {
			for (MapPos p : line.getVertexList()) {
				MapPos tp = transformPoint(p);
				GeometryData data = (GeometryData) line.userData;
				float offset = getPosOffset(data.style);
				drawPosInfo(tp, pointToText(projectPoint(p)), offset, -offset, canvas);
			}
		} catch (Exception e) {
			FLog.e("error drawing line info", e);
		}
	}
	
	private void drawPolygonInfo(Polygon polygon, Canvas canvas) {
		try {
			for (MapPos p : polygon.getVertexList()) {
				MapPos tp = transformPoint(p);
				GeometryData data = (GeometryData) polygon.userData;
				float offset = getPosOffset(data.style);
				drawPosInfo(tp, pointToText(projectPoint(p)), offset, -offset, canvas);
			}
		} catch (Exception e) {
			FLog.e("error drawing polygon info", e);
		}
	}
	
	private void drawPosInfo(MapPos p, String text, float offx, float offy, Canvas canvas) {
		canvas.drawText(text, offx + ((float) p.x), offy + ((float) p.y), textPaint);
	}
	
	private String pointToText(MapPos p) {
		if (showDecimal) {
			return "(" + MeasurementUtil.displayAsCoord(p.x) + ", " + MeasurementUtil.displayAsCoord(p.y) + ")";
		} else {
			return "(" + MeasurementUtil.convertToDegrees(p.x) + ", " + MeasurementUtil.convertToDegrees(p.y) + ")";
		}
	}
	
	private float getPosOffset(GeometryStyle style) {
		return ScaleUtil.getDip(this.getContext(), style.size == 0 ? DEFAULT_OFFSET : style.size * SCALE_FACTOR * 2);
	}
	
	protected MapPos transformPoint(MapPos p) {
		return GeometryUtil.transformVertex(p, mapView, true);
	}
	
	protected MapPos projectPoint(MapPos p) {
		return GeometryUtil.convertFromProjToProj(GeometryUtil.EPSG3857, mapView.getProjectSrid(), p);
	}
	
	public void setDrawList(List<Geometry> geometryList) {
		this.geometryList = geometryList;
		this.invalidate();
	}
	
	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
		updatePaint();
	}
	
	public float getStrokeSize() {
		return strokeSize;
	}
	
	public void setStrokeSize(float strokeSize) {
		this.strokeSize = strokeSize;
		updatePaint();
	}

	private void updatePaint() {
		paint.setColor(color);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(ScaleUtil.getDip(this.getContext(), strokeSize * STROKE_SCALE));
		paint.setAntiAlias(true);
		
		textPaint.setColor(color);
		textPaint.setTextSize(ScaleUtil.getSp(this.getContext(), textSize * TEXT_SCALE));
		textPaint.setAntiAlias(true);
		invalidate();
	}

	public void setMapView(CustomMapView mapView) {
		this.mapView = mapView;
	}

	public float getTextSize() {
		return textSize;
	}

	public void setTextSize(float value) {
		textSize = value;
		updatePaint();
	}

	public void setShowDetail(boolean value) {
		showDetail = value;
	}

	public void showDecimal(boolean value) {
		showDecimal = value;
	}
	
}
