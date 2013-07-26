package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.util.ScaleUtil;
import au.org.intersect.faims.android.util.SpatialiteUtil;

public class LayerBarView extends RelativeLayout {

	public static final float BAR_HEIGHT = 65.0f;
	private static final int BAR_COLOR = 0x88000000;
	
	private MapNorthView northView;
	private ScaleBarView scaleView;
	private LayerManagerButton layerManagerButton;
	private LinearLayout layerInformationView;
	private Button layerInformationButton;
	private CustomMapView mapView;

	public LayerBarView(Context context) {
		super(context);
		RelativeLayout.LayoutParams layerBarLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, (int) ScaleUtil.getDip(getContext(), BAR_HEIGHT));
		layerBarLayout.alignWithParent = true;
		layerBarLayout.addRule(RelativeLayout.ALIGN_BOTTOM);
		setLayoutParams(layerBarLayout);
		setBackgroundColor(BAR_COLOR);
		
		createNorthView(getContext());
		createScaleView(getContext());
		createLayerManagerButton(getContext());
		createLayerInformationView(getContext());
		
		addView(scaleView);
		addView(northView);
		
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setGravity(Gravity.CENTER_VERTICAL);
		layout.addView(layerManagerButton);
		layout.addView(layerInformationView);
		
		addView(layout);
	}

	protected void createLayerInformationView(Context context) {
		layerInformationView = new LinearLayout(context);
		layerInformationView.setOrientation(LinearLayout.VERTICAL);
		
		TextView text = new TextView(context);
		text.setText("Current Layer Information:");
		text.setTextSize(12);
		text.setTextColor(Color.WHITE);

		layerInformationButton = new Button(context);
		layerInformationButton.setBackgroundResource(R.drawable.custom_button);
		layerInformationButton.setText("No layer selected");
		layerInformationButton.setTextColor(Color.WHITE);
		layerInformationButton.setGravity(Gravity.LEFT);
		
		layerInformationView.addView(text);
		layerInformationView.addView(layerInformationButton);
	}

	protected void createLayerManagerButton(Context context) {
		layerManagerButton = new LayerManagerButton(context);
	}

	protected void createScaleView(Context context) {
		scaleView = new ScaleBarView(context);
		RelativeLayout.LayoutParams scaleLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		scaleLayout.alignWithParent = true;
		scaleLayout.addRule(RelativeLayout.ALIGN_RIGHT);
		scaleLayout.addRule(RelativeLayout.CENTER_VERTICAL);
		scaleView.setLayoutParams(scaleLayout);
		
		// TODO make this configurable
		scaleView.setBarWidthRange((int) ScaleUtil.getDip(getContext(), 60), (int) ScaleUtil.getDip(getContext(), 120));
	}

	protected void createNorthView(Context context) {
		northView = new MapNorthView(context);
		RelativeLayout.LayoutParams northLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		northLayout.alignWithParent = true;
		northLayout.addRule(RelativeLayout.ALIGN_RIGHT);
		northLayout.addRule(RelativeLayout.CENTER_VERTICAL);
		northLayout.rightMargin = (int) ScaleUtil.getDip(context, 15);
		northView.setLayoutParams(northLayout);
	}
	
	public void update() {
		northView.setMapRotation(mapView.getRotation());
		
		int width = mapView.getWidth();
		int height = mapView.getHeight();
		
		try {
			scaleView.setMapBoundary(mapView.getZoom(), width, height, SpatialiteUtil
					.distanceBetween(
							GeometryUtil.convertToWgs84(mapView.screenToWorld(0, height, 0)), 
							GeometryUtil.convertToWgs84(mapView.screenToWorld(width, height, 0)), 
							GeometryUtil.EPSG4326) / 1000.0);
		} catch (Exception e) {
			FLog.e("error updating scalebar", e);
		}
		
		int sw = (int) ScaleUtil.getDip(getContext(), 140);
		scaleView.setOffset(getWidth() - northView.getWidth() - sw, getHeight() / 2);
		
		layerInformationButton.setText(mapView.getSelectedLayer() != null? mapView.getLayerName(mapView.getSelectedLayer()) : "No Layer Selected");
	}

	public void setMapView(CustomMapView mapView) {
		this.mapView = mapView;
		
		layerManagerButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				LayerBarView.this.mapView.showLayerManagerDialog();
			}
			
		});
	}
}
