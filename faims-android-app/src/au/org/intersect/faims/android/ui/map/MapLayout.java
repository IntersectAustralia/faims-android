package au.org.intersect.faims.android.ui.map;

import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.activity.ShowProjectActivity;
import au.org.intersect.faims.android.ui.map.tools.MapTool;

public class MapLayout extends LinearLayout {

	private DrawView drawView;
	private CustomMapView mapView;
	private RelativeLayout toolsView;
	private RelativeLayout layersView;
	private EditView editView;
	private RelativeLayout container;
	private Spinner toolsDropDown;
	private Button setButton;
	private LayerBarView layerBarView;

	public MapLayout(Context context) {
		super(context);
		
		ShowProjectActivity activity = (ShowProjectActivity) context;
		
		this.setOrientation(LinearLayout.VERTICAL);
		this.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));
		
		container = new RelativeLayout(activity);
		
		drawView = new DrawView(activity);
		
		editView = new EditView(activity);
		
		toolsView = new RelativeLayout(activity);

		layersView = new RelativeLayout(activity);
		
		layerBarView = new LayerBarView(activity);
		
		mapView = new CustomMapView(activity, this);
		mapView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mapView.startMapping();
		
		indexViews();
		
		layerBarView.getLayerManagementView().setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mapView.showLayerManagerDialog();
			}
		});
		
		setButton = createSetButton();
		setButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				mapView.showSelectionDialog();
			}
			
		});
		
		toolsDropDown = createToolsDropDown(mapView);
		toolsDropDown.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0,
					View arg1, int arg2, long arg3) {
				mapView.selectToolIndex(arg2);
			}

			@Override
			public void onNothingSelected(
					AdapterView<?> arg0) {
				// ignore
			}
			
		});
		
		LinearLayout horizontalLayout = new LinearLayout(getContext());
		horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
		horizontalLayout.addView(setButton);
		horizontalLayout.addView(toolsDropDown);
		addView(horizontalLayout);
		addView(container);
	}
	
	public Button getSetButton() {
		return setButton;
	}
	
	public Spinner getToolsDropDown() {
		return toolsDropDown;
	}

	public DrawView getDrawView() {
		return drawView;
	}
	
	public LayerBarView getLayerBarView() {
		return layerBarView;
	}

	public EditView getEditView() {
		return editView;
	}

	public MapNorthView getNorthView() {
		return layerBarView.getNorthView();
	}
	
	public Button getLayerInformationView() {
		return layerBarView.getLayerInformationView();
	}

	public ScaleBarView getScaleView() {
		return layerBarView.getScaleView();
	}

	public CustomMapView getMapView() {
		return mapView;
	}
	
	public RelativeLayout getToolsView() {
		return toolsView;
	}
	
	public RelativeLayout getLayersView() {
		return layersView;
	}
	
	public void setMapView(CustomMapView value) {
		mapView = value;
		indexViews();
	}
	
	private void indexViews() {
		while(container.getChildCount() > 0) {
			container.removeViewAt(0);
		}
		if (mapView != null) container.addView(mapView);
		if (drawView != null) container.addView(drawView);
		if (editView != null) container.addView(editView);
		if (layerBarView != null) container.addView(layerBarView);
		if (toolsView != null) container.addView(toolsView);
		if (layersView != null) container.addView(layersView);
	}

	private Button createSetButton() {
		Button button = new Button(this.getContext());
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
        button.setText("Selection Manager Dialog");
        return button;
	}
	
	private Spinner createToolsDropDown(CustomMapView mapView) {
		Spinner spinner = new Spinner(this.getContext());
		List<MapTool> tools = mapView.getTools();
		ArrayAdapter<MapTool> arrayAdapter = new ArrayAdapter<MapTool>(
				this.getContext(),
				android.R.layout.simple_spinner_dropdown_item,
				tools);
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		spinner.setLayoutParams(layoutParams);
		spinner.setAdapter(arrayAdapter);
		spinner.setSelection(0);
		return spinner;
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		Bitmap logo = BitmapFactory.decodeResource(getContext().getResources(),
				R.drawable.ic_launcher);
		if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			CustomMapView.setWatermark(logo, -1.0f, -0.9f, 0.1f);
		}else{
			CustomMapView.setWatermark(logo, -1.0f, -0.7f, 0.1f);
		}
		super.onConfigurationChanged(newConfig);
	}
}
