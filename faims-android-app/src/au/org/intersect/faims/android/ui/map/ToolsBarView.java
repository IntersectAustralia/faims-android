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
	
	private static final float BAR_HEIGHT = 65.0f;
	private static final int BAR_COLOR = 0x88000000;

	private CustomMapView mapView;
	private HashMap<String, MapTool> toolMap;
	private LinearLayout buttonsLayout;
	private ToolBarButton configButton;
	private ArrayList<ToolBarButton> toolButtons;
	private ArrayList<View> toolGroups;
	
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
		toolGroups.add(followButton);
		
		ToolBarButton loadButton = toolMap.get(LoadTool.NAME).getButton(getContext());
		toolButtons.add(loadButton);
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
		RelativeLayout.LayoutParams configLayout = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		configLayout.alignWithParent = true;
		configLayout.addRule(RelativeLayout.ALIGN_RIGHT);
		configLayout.addRule(RelativeLayout.CENTER_VERTICAL);
		configButton.setLayoutParams(configLayout);
		
		refreshLayout();
	}
	
	public void createSelectGroup() {
		ToolGroupButton group = new ToolGroupButton(getContext());
		ToolBarButton highlightButton = toolMap.get(HighlightTool.NAME).getButton(getContext());
		toolButtons.add(highlightButton);
		ToolBarButton editButton = toolMap.get(EditTool.NAME).getButton(getContext());
		toolButtons.add(highlightButton);
		group.addButton(highlightButton);
		group.addButton(editButton);
		toolGroups.add(group);
	}
	
	public void createCreateGroup() {
		ToolGroupButton group = new ToolGroupButton(getContext());
		ToolBarButton pointButton = toolMap.get(CreatePointTool.NAME).getButton(getContext());
		toolButtons.add(pointButton);
		ToolBarButton lineButton = toolMap.get(CreateLineTool.NAME).getButton(getContext());
		toolButtons.add(lineButton);
		ToolBarButton polygonButton = toolMap.get(CreatePolygonTool.NAME).getButton(getContext());
		toolButtons.add(polygonButton);
		group.addButton(pointButton);
		group.addButton(lineButton);
		group.addButton(polygonButton);
		toolGroups.add(group);
	}
	
	public void createMeasureGroup() {
		ToolGroupButton group = new ToolGroupButton(getContext());
		ToolBarButton areaButton = toolMap.get(AreaTool.NAME).getButton(getContext());
		toolButtons.add(areaButton);
		ToolBarButton azimuthButton = toolMap.get(AzimuthTool.NAME).getButton(getContext());
		toolButtons.add(azimuthButton);
		ToolBarButton pointDistanceButton = toolMap.get(PointDistanceTool.NAME).getButton(getContext());
		toolButtons.add(pointDistanceButton);
		ToolBarButton lineDistanceButton = toolMap.get(LineDistanceTool.NAME).getButton(getContext());
		toolButtons.add(lineDistanceButton);
		group.addButton(areaButton);
		group.addButton(azimuthButton);
		group.addButton(pointDistanceButton);
		group.addButton(lineDistanceButton);
		toolGroups.add(group);
	}
	
	public void createSelectionGroup() {
		ToolGroupButton group = new ToolGroupButton(getContext());
		ToolBarButton touchButton = toolMap.get(TouchSelectionTool.NAME).getButton(getContext());
		toolButtons.add(touchButton);
		ToolBarButton pointSelectionButton = toolMap.get(PointSelectionTool.NAME).getButton(getContext());
		toolButtons.add(pointSelectionButton);
		ToolBarButton polygonSelectionButton = toolMap.get(PolygonSelectionTool.NAME).getButton(getContext());
		toolButtons.add(polygonSelectionButton);
		ToolBarButton geometrySelectionButton = toolMap.get(GeometriesIntersectSelectionTool.NAME).getButton(getContext());
		toolButtons.add(geometrySelectionButton);
		ToolBarButton databaseSelectionButton = toolMap.get(DatabaseSelectionTool.NAME).getButton(getContext());
		toolButtons.add(databaseSelectionButton);
		ToolBarButton legacySelectionButton = toolMap.get(LegacySelectionTool.NAME).getButton(getContext());
		toolButtons.add(legacySelectionButton);
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
		}
	}
	
	public void refreshLayout() {
		buttonsLayout.removeAllViews();
		
		for (View button : toolGroups) {
			buttonsLayout.addView(button);
		}
		
		addView(configButton);
	}
	
}
