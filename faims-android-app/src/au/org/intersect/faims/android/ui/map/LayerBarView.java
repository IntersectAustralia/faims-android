package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.util.ScaleUtil;

public class LayerBarView extends RelativeLayout {

	private MapNorthView northView;
	private ScaleBarView scaleView;
	private LayerManagementView layerManagementView;
	private Button layerInformationView;

	public LayerBarView(Context context) {
		super(context);
		RelativeLayout.LayoutParams layerBarLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, (int) ScaleUtil.getDip(getContext(), 65.0f));
		layerBarLayout.alignWithParent = true;
		layerBarLayout.addRule(RelativeLayout.ALIGN_BOTTOM);
		setLayoutParams(layerBarLayout);
		setBackgroundColor(0x88000000);
		
		createNorthView(getContext());
		
		createScaleView(getContext());
		
		createLayerManagementView(getContext());
		
		addView(scaleView);
		addView(northView);
		addView(layerManagementView);
		addView(createLayerInformationView(getContext()));
	}

	protected LinearLayout createLayerInformationView(Context context) {
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		RelativeLayout.LayoutParams layerInformationLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layerInformationLayout.alignWithParent = true;
		layerInformationLayout.addRule(RelativeLayout.ALIGN_LEFT);
		layerInformationLayout.topMargin = (int) ScaleUtil.getDip(context, 5);
		layerInformationLayout.leftMargin = (int) ScaleUtil.getDip(context, 70);
		layout.setLayoutParams(layerInformationLayout);
		
		TextView text = new TextView(context);
		text.setText("Current Layer Information:");
		text.setTextSize(12);
		text.setTextColor(Color.WHITE);

		layerInformationView = new Button(context);
		layerInformationView.setBackgroundResource(R.drawable.custom_button);
		layerInformationView.setText("No layer selected");
		layerInformationView.setTextColor(Color.WHITE);
		layerInformationView.setGravity(Gravity.LEFT);
		
		layout.addView(text);
		layout.addView(layerInformationView);
		return layout;
	}

	protected void createLayerManagementView(Context context) {
		layerManagementView = new LayerManagementView(context);
		RelativeLayout.LayoutParams layerManagementLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layerManagementLayout.alignWithParent = true;
		layerManagementLayout.addRule(RelativeLayout.ALIGN_LEFT);
		layerManagementLayout.topMargin = (int) ScaleUtil.getDip(context, 10);
		layerManagementLayout.leftMargin = (int) ScaleUtil.getDip(context, 10);
		layerManagementView.setLayoutParams(layerManagementLayout);
	}

	protected void createScaleView(Context context) {
		scaleView = new ScaleBarView(context);
		RelativeLayout.LayoutParams scaleLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		scaleLayout.alignWithParent = true;
		scaleLayout.addRule(RelativeLayout.ALIGN_RIGHT);
		scaleView.setLayoutParams(scaleLayout);
	}

	protected void createNorthView(Context context) {
		northView = new MapNorthView(context);
		RelativeLayout.LayoutParams northLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		northLayout.alignWithParent = true;
		northLayout.addRule(RelativeLayout.ALIGN_RIGHT);
		northLayout.topMargin = (int) ScaleUtil.getDip(context, 15);
		northLayout.rightMargin = (int) ScaleUtil.getDip(context, 15);
		northView.setLayoutParams(northLayout);
	}

	public MapNorthView getNorthView() {
		return northView;
	}

	public ScaleBarView getScaleView() {
		return scaleView;
	}

	public LayerManagementView getLayerManagementView() {
		return layerManagementView;
	}

	public Button getLayerInformationView() {
		return layerInformationView;
	}
}
