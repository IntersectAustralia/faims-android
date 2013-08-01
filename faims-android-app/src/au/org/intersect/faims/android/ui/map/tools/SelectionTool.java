package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import au.org.intersect.faims.android.ui.form.MapText;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.GeometrySelection;
import au.org.intersect.faims.android.ui.map.button.SelectionManagerButton;
import au.org.intersect.faims.android.util.ScaleUtil;

public class SelectionTool extends MapTool {

	protected RelativeLayout layout;
	protected LinearLayout infoLayout;
	protected SelectionManagerButton selectionManagerButton;
	protected MapText restrictedSelection;
	protected MapText selectedSelection;
	protected MapText selectionCount;
	protected List<View> buttons;
	protected static final int TOP_MARGIN = 85;
	protected static final int BOTTOM_MARGIN = 85;
	protected static final int HEIGHT = 65;
	protected static final int INFO_WIDTH = 225;
	protected static final int PADDING = 5;

	public SelectionTool(Context context, final CustomMapView mapView, String name) {
		super(context, mapView, name);
		
		layout = new RelativeLayout(context);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.leftMargin = (int) ScaleUtil.getDip(context, 10);
		params.rightMargin = (int) ScaleUtil.getDip(context, 10);
		layout.setLayoutParams(params);
		container.addView(layout);
		
		infoLayout = new LinearLayout(context);
		infoLayout.setOrientation(LinearLayout.VERTICAL);
		RelativeLayout.LayoutParams infoParams = new RelativeLayout.LayoutParams((int) ScaleUtil.getDip(context, INFO_WIDTH), LayoutParams.WRAP_CONTENT);
		infoParams.alignWithParent = true;
		infoParams.addRule(RelativeLayout.ALIGN_RIGHT);
		infoParams.addRule(RelativeLayout.ALIGN_TOP);
		infoParams.topMargin = (int) ScaleUtil.getDip(context, TOP_MARGIN);
		infoLayout.setLayoutParams(infoParams);
		
		int padding = (int) ScaleUtil.getDip(context, PADDING);

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		selectedSelection = new MapText(context);
		selectedSelection.setTextColor(Color.WHITE);
		selectedSelection.setBackgroundColor(Color.parseColor("#88000000"));	
		selectedSelection.setLayoutParams(layoutParams);
		selectedSelection.setPadding(padding, padding, padding, padding);
		infoLayout.addView(selectedSelection);
		
		restrictedSelection = new MapText(context);
		restrictedSelection.setTextColor(Color.WHITE);
		restrictedSelection.setBackgroundColor(Color.parseColor("#88000000"));
		restrictedSelection.setLayoutParams(layoutParams);
		restrictedSelection.setPadding(padding, 0, padding, padding);
		infoLayout.addView(restrictedSelection);
		
		selectionCount = new MapText(context);
		selectionCount.setTextColor(Color.WHITE);
		selectionCount.setBackgroundColor(Color.parseColor("#88000000"));
		selectionCount.setLayoutParams(layoutParams);
		selectionCount.setPadding(padding, 0, padding, padding);
		infoLayout.addView(selectionCount);
		
		buttons = new ArrayList<View>();
		selectionManagerButton = new SelectionManagerButton(context);
		RelativeLayout.LayoutParams selectionParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		selectionParams.alignWithParent = true;
		selectionParams.addRule(RelativeLayout.ALIGN_LEFT);
		selectionParams.topMargin = (int) ScaleUtil.getDip(context, buttons.size() * HEIGHT + TOP_MARGIN);
		selectionManagerButton.setLayoutParams(selectionParams);
		selectionManagerButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mapView.showSelectionDialog();
			}
		});
		buttons.add(selectionManagerButton);
		updateLayout();
	}
	
	protected void updateLayout() {
		if (layout != null) {
			layout.removeAllViews();
			layout.addView(selectionManagerButton);
			layout.addView(infoLayout);
		}
	}
	
	@Override
	public void activate() {
		setSelectedSelection(mapView.getSelectedSelection());
		setRestrictedSelection(mapView.getRestrictedSelections());
	}
	
	@Override
	public void deactivate() {
		
	}
	
	@Override
	public void onSelectionChanged() {
		setSelectedSelection(mapView.getSelectedSelection());
		setRestrictedSelection(mapView.getRestrictedSelections());
	}
	
	private void setRestrictedSelection(ArrayList<GeometrySelection> restrictedSelections) {
		if(restrictedSelections == null || restrictedSelections.isEmpty()){
			restrictedSelection.setVisibility(View.GONE);
		}else{
			StringBuilder builder = new StringBuilder();
			builder.append("Current Restrictions: ");
			for (GeometrySelection geometrySelection : restrictedSelections) {
				builder.append(geometrySelection.getName());
				if(restrictedSelections.indexOf(geometrySelection) < restrictedSelections.size() - 1){
					builder.append(", ");
				}
			}
			restrictedSelection.setVisibility(View.VISIBLE);
			restrictedSelection.setText(builder.toString());
		}
		
	}
	protected void setSelectedSelection(GeometrySelection set) {
		if (set == null) {
			selectedSelection.setText("No selection selected");
			selectionCount.setVisibility(View.GONE);
		} else {
			selectedSelection.setText("Current Selection: " + set.getName());
			selectionCount.setVisibility(View.VISIBLE);
			selectionCount.setText("Selection Count: " + set.getDataList().size());
		}
	}

}
