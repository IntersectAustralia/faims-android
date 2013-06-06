package au.org.intersect.faims.android.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;

public class BitmapUtil {
	
	public static Bitmap rotateBitmap(Bitmap bitmap, float angle) {
		Canvas canvas = new Canvas();
		Matrix matrix = new Matrix();
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		int d = (int) Math.sqrt(w * w + h * h);
		matrix.postRotate(angle, w / 2, h / 2);
		matrix.postTranslate((d-w)/2, (d-h)/2);
		Bitmap rotateBitmap = Bitmap.createBitmap(d, d, Config.ARGB_8888);
		canvas.setBitmap(rotateBitmap);
		canvas.drawBitmap(bitmap, matrix, null);
		return rotateBitmap;
	}

}
