package au.org.intersect.faims.android.managers;

import java.io.File;
import java.util.ArrayList;

import android.graphics.Bitmap;
import au.org.intersect.faims.android.util.BitmapUtil;

import com.google.inject.Singleton;

@Singleton
public class BitmapManager {

	private ArrayList<Bitmap> bitmaps;

	public synchronized Bitmap createBitmap(String path, int width, int height) {
		Bitmap bitmap = BitmapUtil.decodeFile(new File(path), width, height);
		addBitmap(bitmap);
		return bitmap;
	}
	
	public synchronized void addBitmap(Bitmap bitmap) {
		bitmaps.add(bitmap);
	}
	
	public synchronized void init() {
		this.bitmaps = new ArrayList<Bitmap>();
	}
	
	public synchronized void destroy() {
		if (bitmaps != null) {
			for (Bitmap bitmap : bitmaps) {
				bitmap.recycle();
			}
			this.bitmaps = null;
		}
	}
	
}
