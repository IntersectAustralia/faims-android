package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import au.org.intersect.faims.android.util.ScaleUtil;

public class MapLayout extends RelativeLayout {

	private DrawView drawView;
	private MapNorthView northView;
	private ScaleBarView scaleView;
	private CustomMapView mapView;
	private RelativeLayout toolsView;
	private EditView editView;

	public MapLayout(Context context) {
		super(context);
		
		setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));
		
		drawView = new DrawView(context);
		drawView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		editView = new EditView(context);
		editView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		northView = new MapNorthView(context);
		RelativeLayout.LayoutParams northLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		northLayout.alignWithParent = true;
		northLayout.addRule(RelativeLayout.ALIGN_RIGHT);
		northLayout.topMargin = (int) ScaleUtil.getDip(context, 10);
		northLayout.rightMargin = (int) ScaleUtil.getDip(context, 10);
		northView.setLayoutParams(northLayout);
		
		scaleView = new ScaleBarView(context);
		scaleView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		toolsView = new RelativeLayout(context);
		
		mapView = new CustomMapView(context, drawView, editView, northView, scaleView, toolsView);

		mapView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mapView.startMapping();
		
		indexViews();
	}

	public DrawView getDrawView() {
		return drawView;
	}

	public MapNorthView getNorthView() {
		return northView;
	}

	public ScaleBarView getScaleView() {
		return scaleView;
	}

	public CustomMapView getMapView() {
		return mapView;
	}
	
	public void setMapView(CustomMapView value) {
		mapView = value;
		indexViews();
	}
	
	private void indexViews() {
		while(this.getChildCount() > 0) {
			this.removeViewAt(0);
		}
		if (mapView != null) addView(mapView);
		if (drawView != null) addView(drawView);
		if (editView != null) addView(editView);
		if (northView != null) addView(northView);
		if (scaleView != null) addView(scaleView);
		if (toolsView != null) addView(toolsView);
	}

}
