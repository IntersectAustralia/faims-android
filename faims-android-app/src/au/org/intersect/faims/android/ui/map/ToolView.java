package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.widget.RelativeLayout;
import au.org.intersect.faims.android.util.ScaleUtil;

public class ToolView extends RelativeLayout {

	public ToolView(Context context) {
		super(context);
		RelativeLayout.LayoutParams layerBarLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		layerBarLayout.topMargin = (int) ScaleUtil.getDip(context, ToolsBarView.BAR_HEIGHT + 20);
		layerBarLayout.bottomMargin = (int) ScaleUtil.getDip(context, ToolsBarView.BAR_HEIGHT + 20);
		setLayoutParams(layerBarLayout);
	}

}
