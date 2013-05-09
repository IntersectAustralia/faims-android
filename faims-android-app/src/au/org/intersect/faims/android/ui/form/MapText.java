package au.org.intersect.faims.android.ui.form;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.TextView;

public class MapText extends TextView {

	private Paint paint;
	private RectF bounds;

	public MapText(Context context) {
		super(context);
		
		paint = new Paint(Color.argb(0x11, 0xFF, 0xFF, 0xFF));
		bounds = new RectF(new Rect(0, 0, this.getWidth(), this.getHeight()));
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		canvas.drawRoundRect(bounds, 5, 5, paint);
	}

}
