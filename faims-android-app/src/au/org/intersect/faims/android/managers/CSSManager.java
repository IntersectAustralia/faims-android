package au.org.intersect.faims.android.managers;

import java.util.HashMap;

import android.view.View;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.util.FileUtil;

import com.google.inject.Singleton;
import com.nativecss.NativeCSS;

@Singleton
public class CSSManager {

	private HashMap<View, String> viewCSS;
	
	public void init(String css, ShowModuleActivity activity) {
		this.viewCSS = new HashMap<View, String>();
		if (!css.isEmpty()) {
			try {
				NativeCSS.styleWithCSS(css);
			} catch (Exception e) {
				FLog.e("Couldn't style module with module CSS file", e);
				NativeCSS.styleWithCSS("");
			}
		} else {
			try {
				NativeCSS.styleWithCSS(FileUtil.convertStreamToString(activity.getAssets().open("default.css")));
			} catch (Exception e) {
				FLog.e("Couldn't style module with default styling", e);
				NativeCSS.styleWithCSS("");
			}
		}
	}
	
	public void destroy() {
		if (viewCSS != null) {
			for (View view : viewCSS.keySet()) {
				NativeCSS.removeCSSClass(view, viewCSS.get(view));
			}
			this.viewCSS = null;
		}
	}
	
	public void addCSS(View view, String cssClass) {
		removeCSS(view);
		viewCSS.put(view, cssClass);
		NativeCSS.addCSSClass(view, cssClass);
	}
	
	public void removeCSS(View view) {
		if (viewCSS.containsKey(view)) {
			NativeCSS.removeCSSClass(view, viewCSS.get(view));
			viewCSS.remove(view);
		}
	}

	public void refreshCSS(View view) {
		NativeCSS.refreshCSSStyling(view);
	}

	public void setCSSID(View view, String id) {
		NativeCSS.setCSSId(view, id);
	}
	
}
