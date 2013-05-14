package au.org.intersect.faims.android.util;

import android.content.Context;
import android.util.TypedValue;

public class ScaleUtil {

	public static float getDip(Context context, float size) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, context.getResources().getDisplayMetrics());
	}
	
	public static float getSp(Context context, float size) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, context.getResources().getDisplayMetrics());
	}
	
}
