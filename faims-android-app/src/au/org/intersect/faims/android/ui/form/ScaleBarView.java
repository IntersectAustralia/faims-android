package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import au.org.intersect.faims.android.nutiteq.SimpleScaleBar;

import com.nutiteq.components.MapPos;

public class ScaleBarView extends View {

	private SimpleScaleBar bar;
	private float lastZoom = -1;

	public ScaleBarView(Context context) {
		super(context);
		bar = new SimpleScaleBar();
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		bar.paint(canvas);
	}

	public void setMapBoundary(float zoom, int width, int height,
			MapPos min, MapPos max) {
		if (lastZoom == zoom) return;
		lastZoom = zoom;
		
		bar.reSize(width, height, min, max);
		
		invalidate();
	}
	
}
