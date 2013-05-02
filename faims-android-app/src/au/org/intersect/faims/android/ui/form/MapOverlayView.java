package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.widget.ImageView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.nutiteq.SimpleScaleBar;

import com.nutiteq.components.MapPos;

public class MapOverlayView extends ImageView {
	
	private float lastRotation = -1;
	
	private SimpleScaleBar scaleBar;

	private Paint paint;

	public MapOverlayView(Context context) {
		super(context);
		setMapRotation(0);
		paint = new Paint();
	}
	
	public Bitmap loadBitmap() {
		return BitmapFactory.decodeResource(this.getContext().getResources(),
                R.drawable.small_north);
	}
	
	public Bitmap rotate(Bitmap src, float degree) {
	    Matrix matrix = new Matrix();
	    matrix.postRotate(degree);
	    return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
	}
	
	public void setMapRotation(float value) {
		if (lastRotation == value) return;
		lastRotation = value;
		//setImageBitmap(rotate(loadBitmap(), -value));
		invalidate();
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawBitmap(loadBitmap(), getImageMatrix(), paint);
		if (scaleBar != null) {
			scaleBar.paint(canvas);
		}
	}
	
	public void setMapBoundary(int mapWidth, int mapHeight, MapPos min, MapPos max) {
		if (scaleBar == null) {
			scaleBar = new SimpleScaleBar();
		}
		scaleBar.reSize(mapWidth, mapHeight, min, max);
		invalidate();
	}

}
