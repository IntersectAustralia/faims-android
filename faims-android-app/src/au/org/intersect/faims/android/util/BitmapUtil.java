package au.org.intersect.faims.android.util;

import java.io.File;
import java.io.FileInputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import au.org.intersect.faims.android.log.FLog;

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
		canvas.drawBitmap(bitmap, matrix, new Paint(Paint.FILTER_BITMAP_FLAG));
		return rotateBitmap;
	}
	
	public static Bitmap decodeFile(File f, int width, int height) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			// The new size we want to scale to
			
			// Find the correct scale value. It should be the power of 2.
			int scale = 1;
			int pixels = width * height;
			while ((o.outWidth / scale) * (o.outHeight / scale) > pixels) {
				scale *= 2;
			}
			
			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (Exception e) {
			FLog.e("error when decoding the bitmap", e);
		}
		return null;
	}

}
