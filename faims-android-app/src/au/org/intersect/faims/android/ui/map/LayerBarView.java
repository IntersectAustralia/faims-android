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

	private static final float BAR_HEIGHT = 65.0f;
	private MapNorthView northView;
	private ScaleBarView scaleView;
	private LayerManagementView layerManagementView;
	private LinearLayout layerInformationView;
	private Button layerInformationButton;

	public LayerBarView(Context context) {
		super(context);
		RelativeLayout.LayoutParams layerBarLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, (int) ScaleUtil.getDip(getContext(), BAR_HEIGHT));
		layerBarLayout.alignWithParent = true;
		layerBarLayout.addRule(RelativeLayout.ALIGN_BOTTOM);
		setLayoutParams(layerBarLayout);
		setBackgroundColor(0x88000000);
		
		createNorthView(getContext());
		
		createScaleView(getContext());
		
		createLayerManagementView(getContext());
		
		createLayerInformationView(getContext());
		
		addView(scaleView);
		addView(northView);
		
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setGravity(Gravity.CENTER_VERTICAL);
		layout.addView(layerManagementView);
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

	protected void createLayerManagementView(Context context) {
		layerManagementView = new LayerManagementView(context);
	}

	protected void createScaleView(Context context) {
		scaleView = new ScaleBarView(context);
		RelativeLayout.LayoutParams scaleLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		scaleLayout.alignWithParent = true;
		scaleLayout.addRule(RelativeLayout.ALIGN_RIGHT);
		scaleLayout.addRule(RelativeLayout.CENTER_VERTICAL);
		scaleView.setLayoutParams(scaleLayout);
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

	public MapNorthView getNorthView() {
		return northView;
	}

	public ScaleBarView getScaleView() {
		return scaleView;
	}

	public LayerManagementView getLayerManagementView() {
		return layerManagementView;
	}

	public Button getLayerInformationButton() {
		return layerInformationButton;
	}
	
	public void refreshLayout() {
		int sw = (int) ScaleUtil.getDip(getContext(), 140);
		scaleView.setOffset(getWidth() - northView.getWidth() - sw, getHeight() / 2);
	}
}
