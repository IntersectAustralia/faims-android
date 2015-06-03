package au.org.intersect.faims.android.managers;

import java.io.File;
import java.util.HashMap;

import android.graphics.Bitmap;
import au.org.intersect.faims.android.util.BitmapUtil;

import com.google.inject.Singleton;

@Singleton
public class BitmapManager {

	private HashMap<String, Bitmap> bitmaps;

	public BitmapManager() {
	}
	
	public synchronized Bitmap createBitmap(String path, int width, int height) {
		Bitmap bitmap = BitmapUtil.decodeFile(new File(path), width, height);
		addBitmap(path, bitmap);
		return bitmap;
	}
	
	public synchronized void addBitmap(String name, Bitmap bitmap) {
		recycleBitmap(name);
		bitmaps.put(name,  bitmap);
	}
	
	public synchronized void removeBitmap(String name) {
		recycleBitmap(name);
		bitmaps.remove(name);
	}
	
	public synchronized void init() {
		this.bitmaps = new HashMap<String, Bitmap>();
	}
	
	public synchronized void destroy() {
		if (bitmaps != null) {
			for (Bitmap bitmap : bitmaps.values()) {
				bitmap.recycle();
			}
			this.bitmaps = null;
		}
	}
	
	private void recycleBitmap(String name) {
		if (bitmaps.containsKey(name)) {
			bitmaps.get(name).recycle();
		}
	}
	
}
