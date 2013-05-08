package au.org.intersect.faims.android.util;

import android.content.Context;
import android.util.DisplayMetrics;

public class Dip {

	public static int getDip(Context context, int size) {
		return (size * context.getResources().getDisplayMetrics().densityDpi) / DisplayMetrics.DENSITY_DEFAULT;
	}
	
}
