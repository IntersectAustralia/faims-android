package au.org.intersect.faims.android.ui.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.map.button.ToolBarButton;
import au.org.intersect.faims.android.ui.map.button.ToolGroupButton;
import au.org.intersect.faims.android.ui.map.tools.AreaTool;
import au.org.intersect.faims.android.ui.map.tools.AzimuthTool;
import au.org.intersect.faims.android.ui.map.tools.CreateLineTool;
import au.org.intersect.faims.android.ui.map.tools.CreatePointTool;
import au.org.intersect.faims.android.ui.map.tools.CreatePolygonTool;
import au.org.intersect.faims.android.ui.map.tools.DatabaseSelectionTool;
import au.org.intersect.faims.android.ui.map.tools.EditTool;
import au.org.intersect.faims.android.ui.map.tools.FollowTool;
import au.org.intersect.faims.android.ui.map.tools.GeometriesIntersectSelectionTool;
import au.org.intersect.faims.android.ui.map.tools.HighlightTool;
import au.org.intersect.faims.android.ui.map.tools.LegacySelectionTool;
import au.org.intersect.faims.android.ui.map.tools.LineDistanceTool;
import au.org.intersect.faims.android.ui.map.tools.LoadTool;
import au.org.intersect.faims.android.ui.map.tools.MapTool;
import au.org.intersect.faims.android.ui.map.tools.PointDistanceTool;
import au.org.intersect.faims.android.ui.map.tools.PointSelectionTool;
import au.org.intersect.faims.android.ui.map.tools.PolygonSelectionTool;
import au.org.intersect.faims.android.ui.map.tools.TouchSelectionTool;
import au.org.intersect.faims.android.util.ScaleUtil;

public class ToolsBarView extends RelativeLayout {
	
	public static final float BAR_HEIGHT = 65.0f;
	private static final int BAR_COLOR = 0x88000000;

	private CustomMapView mapView;
	private HashMap<String, MapTool> toolMap;
	private LinearLayout buttonsLayout;
	private ToolBarButton configButton;
	private ArrayList<ToolBarButton> toolButtons;
	private ArrayList<View> toolGroups;
	private HashMap<ToolBarButton, String> toolButtonMap;
	
	public ToolsBarView(Context context) {
		super(context);
		RelativeLayout.LayoutParams layerBarLayout = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, (int) ScaleUtil.getDip(getContext(), BAR_HEIGHT));
		layerBarLayout.alignWithParent = true;
		layerBarLayout.addRule(RelativeLayout.ALIGN_TOP);
		setLayoutParams(layerBarLayout);
		setBackgroundColor(BAR_COLOR);
	}

	public void setMapView(CustomMapView mapView) {
		this.mapView = mapView;
		createToolLayout();
	}
	
	private void createToolLayout() {
		
		buttonsLayout = new LinearLayout(getContext());
		buttonsLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
		buttonsLayout.setGravity(Gravity.CENTER_VERTICAL);
		addView(buttonsLayout);
		
		List<MapTool> tools = mapView.getTools();
		
		toolMap = new HashMap<String, MapTool>();
		toolButtonMap = new HashMap<ToolBarButton, String>();
		
		for (MapTool tool : tools) {
			toolMap.put(tool.toString(), tool);
		}
		
		toolButtons = new ArrayList<ToolBarButton>();
		toolGroups = new ArrayList<View>();
		
		// create tool groups
		
		createSelectGroup();
		createCreateGroup();
		createMeasureGroup();
		createSelectionGroup();
		
		// adding tool buttons as single group buttons
		
		ToolBarButton followButton = toolMap.get(FollowTool.NAME).getButton(getContext());
		toolButtons.add(followButton);
		toolButtonMap.put(followButton, FollowTool.NAME);
		toolGroups.add(followButton);
		
		ToolBarButton loadButton = toolMap.get(LoadTool.NAME).getButton(getContext());
		toolButtons.add(loadButton);
		toolButtonMap.put(loadButton, LoadTool.NAME);
		toolGroups.add(loadButton);
		
		// wrap groups as radio buttons
		for (ToolBarButton button : toolButtons) {
			wrapAsRadioButton(button);
		}
		
		// set anchor for groups
		for (View view : toolGroups) {
			if (view instanceof ToolGroupButton) {
				((ToolGroupButton) view).setAnchorView(buttonsLayout);
			}
		}
		
		configButton = new ToolBarButton(getContext());
		configButton.setSelectedState(R.drawable.tools_select_s);
		configButton.setNormalState(R.drawable.tools_select);
		RelativeLayout.LayoutParams configLayout = new RelativeLayout.LayoutParams(
				new LayoutParams((int) ScaleUtil.getDip(getContext(), ToolsBarView.BAR_HEIGHT), (int) ScaleUtil.getDip(getContext(), ToolsBarView.BAR_HEIGHT)));
		configLayout.alignWithParent = true;
		configLayout.addRule(RelativeLayout.ALIGN_RIGHT);
		configLayout.addRule(RelativeLayout.CENTER_VERTICAL);
		configButton.setLayoutParams(configLayout);
		configButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showConfigDialog();
			}
		});
		
		refreshLayout();
		
		// select first button
		setSelectedButton(toolButtons.get(0));
	}
	
	private void showConfigDialog(){
		ConfigDialog configDialog = new ConfigDialog(getContext());
		configDialog.init(mapView);
		configDialog.show();
	}

	public void createSelectGroup() {
		ToolGroupButton group = new ToolGroupButton(getContext());
		group.setLabel("Geometry");
		ToolBarButton highlightButton = toolMap.get(HighlightTool.NAME).getButton(getContext());
		toolButtons.add(highlightButton);
		toolButtonMap.put(highlightButton, HighlightTool.NAME);
		ToolBarButton editButton = toolMap.get(EditTool.NAME).getButton(getContext());
		toolButtons.add(editButton);
		toolButtonMap.put(editButton, EditTool.NAME);
		group.addButton(highlightButton);
		group.addButton(editButton);
		toolGroups.add(group);
	}
	
	public void createCreateGroup() {
		ToolGroupButton group = new ToolGroupButton(getContext());
		group.setLabel("Create");
		ToolBarButton pointButton = toolMap.get(CreatePointTool.NAME).getButton(getContext());
		toolButtons.add(pointButton);
		toolButtonMap.put(pointButton, CreatePointTool.NAME);
		ToolBarButton lineButton = toolMap.get(CreateLineTool.NAME).getButton(getContext());
		toolButtons.add(lineButton);
		toolButtonMap.put(lineButton, CreateLineTool.NAME);
		ToolBarButton polygonButton = toolMap.get(CreatePolygonTool.NAME).getButton(getContext());
		toolButtons.add(polygonButton);
		toolButtonMap.put(polygonButton, CreatePolygonTool.NAME);
		group.addButton(pointButton);
		group.addButton(lineButton);
		group.addButton(polygonButton);
		toolGroups.add(group);
	}
	
	public void createMeasureGroup() {
		ToolGroupButton group = new ToolGroupButton(getContext());
		group.setLabel("Measure");
		ToolBarButton areaButton = toolMap.get(AreaTool.NAME).getButton(getContext());
		toolButtons.add(areaButton);
		toolButtonMap.put(areaButton, AreaTool.NAME);
		ToolBarButton azimuthButton = toolMap.get(AzimuthTool.NAME).getButton(getContext());
		toolButtons.add(azimuthButton);
		toolButtonMap.put(azimuthButton, AzimuthTool.NAME);
		ToolBarButton pointDistanceButton = toolMap.get(PointDistanceTool.NAME).getButton(getContext());
		toolButtons.add(pointDistanceButton);
		toolButtonMap.put(pointDistanceButton, PointDistanceTool.NAME);
		ToolBarButton lineDistanceButton = toolMap.get(LineDistanceTool.NAME).getButton(getContext());
		toolButtons.add(lineDistanceButton);
		toolButtonMap.put(lineDistanceButton, LineDistanceTool.NAME);
		group.addButton(areaButton);
		group.addButton(azimuthButton);
		group.addButton(pointDistanceButton);
		group.addButton(lineDistanceButton);
		toolGroups.add(group);
	}
	
	public void createSelectionGroup() {
		ToolGroupButton group = new ToolGroupButton(getContext());
		group.setLabel("Selection");
		ToolBarButton touchButton = toolMap.get(TouchSelectionTool.NAME).getButton(getContext());
		toolButtons.add(touchButton);
		toolButtonMap.put(touchButton, TouchSelectionTool.NAME);
		ToolBarButton pointSelectionButton = toolMap.get(PointSelectionTool.NAME).getButton(getContext());
		toolButtons.add(pointSelectionButton);
		toolButtonMap.put(pointSelectionButton, PointSelectionTool.NAME);
		ToolBarButton polygonSelectionButton = toolMap.get(PolygonSelectionTool.NAME).getButton(getContext());
		toolButtons.add(polygonSelectionButton);
		toolButtonMap.put(polygonSelectionButton, PolygonSelectionTool.NAME);
		ToolBarButton geometrySelectionButton = toolMap.get(GeometriesIntersectSelectionTool.NAME).getButton(getContext());
		toolButtons.add(geometrySelectionButton);
		toolButtonMap.put(geometrySelectionButton, GeometriesIntersectSelectionTool.NAME);
		ToolBarButton databaseSelectionButton = toolMap.get(DatabaseSelectionTool.NAME).getButton(getContext());
		toolButtons.add(databaseSelectionButton);
		toolButtonMap.put(databaseSelectionButton, DatabaseSelectionTool.NAME);
		ToolBarButton legacySelectionButton = toolMap.get(LegacySelectionTool.NAME).getButton(getContext());
		toolButtons.add(legacySelectionButton);
		toolButtonMap.put(legacySelectionButton, LegacySelectionTool.NAME);
		group.addButton(touchButton);
		group.addButton(pointSelectionButton);
		group.addButton(polygonSelectionButton);
		group.addButton(geometrySelectionButton);
		group.addButton(databaseSelectionButton);
		group.addButton(legacySelectionButton);
		toolGroups.add(group);
	}
	
	public View wrapAsRadioButton(final ToolBarButton button) {
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setSelectedButton(button);
			}
			
		});
		return button;
	}
	
	public void setSelectedButton(ToolBarButton button) {
		for (ToolBarButton b : toolButtons) {
			for (View view : toolGroups) {
				if (view instanceof ToolGroupButton) {
					ToolGroupButton group = (ToolGroupButton) view;
					if (group.getButtons().contains(button)) {
						group.setSelectedButton(button);
					}
				}
			}
			
			b.setChecked(button == b);
			
			if (button == b) {
				mapView.selectTool(toolButtonMap.get(b));
			}
		}
	}
	
	public void refreshLayout() {
		this.removeAllViews();
		this.addView(buttonsLayout);
		
		buttonsLayout.removeAllViews();
		
		for (View button : toolGroups) {
			buttonsLayout.addView(button);
		}
		
		this.addView(configButton);
	}
	
}
