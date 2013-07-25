package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import au.org.intersect.faims.android.nutiteq.SimpleScaleBar;

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
		bar.paint(this.getContext(), canvas);
	}

	public void setMapBoundary(float zoom, int width, int height,
			double mapWidth) {
		if (lastZoom == zoom) return;
		lastZoom = zoom;
		
		bar.reSize(width, height, mapWidth);
		
		invalidate();
	}
	
	public void refreshMapBoundary(float zoom, int width, int height,
			double mapWidth) {
		lastZoom = zoom;
		
		bar.reSize(width, height, mapWidth);
		
		invalidate();
	}

	public void setBarWidthRange(int minWidth, int maxWidth) {
		bar.setBarWidthRange(minWidth, maxWidth);
	}
	
}
