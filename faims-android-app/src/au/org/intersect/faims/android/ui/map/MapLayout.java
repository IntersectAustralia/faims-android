package au.org.intersect.faims.android.ui.map;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import au.org.intersect.faims.android.ui.activity.ShowProjectActivity;
import au.org.intersect.faims.android.ui.map.tools.MapTool;
import au.org.intersect.faims.android.util.ScaleUtil;

public class MapLayout extends LinearLayout {

	private DrawView drawView;
	private MapNorthView northView;
	private ScaleBarView scaleView;
	private CustomMapView mapView;
	private RelativeLayout toolsView;
	private EditView editView;
	private RelativeLayout container;
	private Button layerButton;
	private Spinner toolsDropDown;
	private Button setButton;

	public MapLayout(Context context) {
		super(context);
		
		ShowProjectActivity activity = (ShowProjectActivity) context;
		
		this.setOrientation(LinearLayout.VERTICAL);
		this.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));
		
		container = new RelativeLayout(activity);
		
		drawView = new DrawView(activity);
		
		editView = new EditView(activity);
		
		northView = new MapNorthView(activity);
		RelativeLayout.LayoutParams northLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		northLayout.alignWithParent = true;
		northLayout.addRule(RelativeLayout.ALIGN_RIGHT);
		northLayout.topMargin = (int) ScaleUtil.getDip(activity, 10);
		northLayout.rightMargin = (int) ScaleUtil.getDip(activity, 10);
		northView.setLayoutParams(northLayout);
		
		scaleView = new ScaleBarView(activity);
		
		toolsView = new RelativeLayout(activity);
		
		mapView = new CustomMapView(activity, this);
		mapView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mapView.startMapping();
		
		indexViews();
		
		layerButton = createLayerButton();
		layerButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
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
		horizontalLayout.addView(layerButton);
		horizontalLayout.addView(setButton);
		horizontalLayout.addView(toolsDropDown);
		addView(horizontalLayout);
		addView(container);
	}
	
	public Button getLayerButton() {
		return layerButton;
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
	
	public EditView getEditView() {
		return editView;
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
	
	public RelativeLayout getToolsView() {
		return toolsView;
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
		if (northView != null) container.addView(northView);
		if (scaleView != null) container.addView(scaleView);
		if (toolsView != null) container.addView(toolsView);
	}
	
	private Button createLayerButton() {
		Button button = new Button(this.getContext());
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
        button.setText("Layer Manager Dialog");
        return button;
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

}
