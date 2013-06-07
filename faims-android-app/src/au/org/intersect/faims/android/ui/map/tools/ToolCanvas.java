package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.graphics.Paint;
import android.view.View;
import au.org.intersect.faims.android.util.ScaleUtil;

public abstract class ToolCanvas extends View {

	protected static final float STROKE_SCALE = 10.0f;
	
	protected static final float TEXT_SCALE = 24.0f;
	
	protected static final float DEFAULT_OFFSET = 20.0f;
	
	protected int color;
	protected float strokeSize;
	protected float textSize;
	protected Paint paint;
	protected Paint textPaint;
	protected boolean isDirty;

	public ToolCanvas(Context context) {
		super(context);
		paint = new Paint();
		textPaint = new Paint();
	}
	
	
	public void clear() {
		isDirty = false;
		invalidate();
	}

	public void setColor(int color) {
		this.color = color;
		updatePaint();
	}

	public void setStrokeSize(float strokeSize) {
		this.strokeSize = strokeSize;
		updatePaint();
	}
	
	public void setTextSize(float value) {
		textSize = value;
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
	}
	
}
