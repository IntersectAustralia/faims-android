package au.org.intersect.faims.android.ui.map;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import au.org.intersect.faims.android.nutiteq.CustomLine;
import au.org.intersect.faims.android.nutiteq.CustomPoint;
import au.org.intersect.faims.android.nutiteq.CustomPolygon;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;

public class DrawView extends View {
	
	private static final float SCALE_FACTOR = 50.0f;

	private Paint paint = new Paint();
	private List<Geometry> geometryList;
	
	public DrawView(Context context) {
		super(context);
		
		paint.setColor(Color.CYAN);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(5.0f);
		paint.setAntiAlias(true);
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		if (geometryList == null) return;
		
		for (Geometry geom : geometryList) {
			drawGeometryOverlay(geom, canvas);
		}
	}
	
	private void drawGeometryOverlay(Geometry geom, Canvas canvas) {
		if (geom instanceof CustomPoint) {
			drawPointOverlay((CustomPoint) geom, canvas);
		} else if (geom instanceof CustomLine) {
			drawLineOverlay((CustomLine) geom, canvas);
		} else if (geom instanceof CustomPolygon) {
			drawPolygonOverlay((CustomPolygon) geom, canvas);
		}
	}
	
	private void drawPointOverlay(CustomPoint point, Canvas canvas) {
		float size = point.getStyleSet().getFirstZoomStyle().pickingSize * SCALE_FACTOR;
		canvas.drawCircle((float) point.getMapPos().x, (float) point.getMapPos().y, size, paint);
	}
	
	private void drawLineOverlay(CustomLine line, Canvas canvas) {
		MapPos lp = null;
		for (MapPos p : line.getVertexList()) {
			if (lp != null) {
				canvas.drawLine((float) lp.x, (float) lp.y, (float) p.x, (float) p.y, paint);
			}
			lp = p;
		}
	}
	
	private void drawPolygonOverlay(CustomPolygon polygon, Canvas canvas) {
		MapPos lp = null;
		for (MapPos p : polygon.getVertexList()) {
			if (lp != null) {
				canvas.drawLine((float) lp.x, (float) lp.y, (float) p.x, (float) p.y, paint);
			}
			lp = p;
		}
		MapPos p = polygon.getVertexList().get(0);
		canvas.drawLine((float) lp.x, (float) lp.y, (float) p.x, (float) p.y, paint);
	}
	
	public void setDrawList(List<Geometry> geometryList) {
		this.geometryList = geometryList;
		this.invalidate();
	}

}
