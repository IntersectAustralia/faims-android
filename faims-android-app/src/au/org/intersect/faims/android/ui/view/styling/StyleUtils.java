package au.org.intersect.faims.android.ui.view.styling;

import android.widget.LinearLayout.LayoutParams;

public final class StyleUtils {

	public static int getLayoutParamsValue(String value) {
		if ("wrap_content".equals(value)) {
			return LayoutParams.WRAP_CONTENT;
		} else if ("match_parent".equals(value)) {
			return LayoutParams.MATCH_PARENT;
		} else {
			return Integer.parseInt(value);
		}
	}

}
