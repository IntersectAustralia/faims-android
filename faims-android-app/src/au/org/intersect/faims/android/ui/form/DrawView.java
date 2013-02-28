package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;

public class DrawView extends View {

	private Paint paint = new Paint();
	private Geometry geometry;
	
	public DrawView(Context context) {
		super(context);
		
		paint.setColor(Color.CYAN);
		paint.setStrokeWidth(5.0f);
		paint.setAntiAlias(true);
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		if (geometry != null) {
			if (geometry instanceof Point) {
				Point p = (Point) geometry;
				canvas.drawCircle((float) p.getMapPos().x, (float) p.getMapPos().y, 10.0f, paint);
			} else if (geometry instanceof Line) {
				Line l = (Line) geometry;
				MapPos lp = null;
				for (MapPos p : l.getVertexList()) {
					if (lp != null) {
						canvas.drawLine((float) lp.x, (float) lp.y, (float) p.x, (float) p.y, paint);
					}
					lp = p;
				}
			} else if (geometry instanceof Polygon) {
				Polygon poly = (Polygon) geometry;
				MapPos lp = null;
				for (MapPos p : poly.getVertexList()) {
					if (lp != null) {
						canvas.drawLine((float) lp.x, (float) lp.y, (float) p.x, (float) p.y, paint);
					}
					lp = p;
				}
				MapPos p = poly.getVertexList().get(0);
				canvas.drawLine((float) lp.x, (float) lp.y, (float) p.x, (float) p.y, paint);
			}
		}
	}
	
	public void drawGeometry(Geometry geometry) {
		this.geometry = geometry;
		this.invalidate();
	}

}
