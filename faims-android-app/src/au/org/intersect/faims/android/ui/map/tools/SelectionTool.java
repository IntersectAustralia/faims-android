package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.form.MapText;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.GeometrySelection;

public class SelectionTool extends MapTool {

	protected LinearLayout layout;
	protected MapButton restrictSelection;
	protected MapButton selectSelection;
	protected MapText restrictedSelection;
	protected MapText selectedSelection;
	protected MapText selectionCount;

	public SelectionTool(Context context, CustomMapView mapView, String name) {
		super(context, mapView, name);
		
		layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		container.addView(layout);
		
		selectedSelection = new MapText(context);
		selectedSelection.setBackgroundColor(Color.WHITE);
		
		restrictedSelection = new MapText(context);
		restrictedSelection.setBackgroundColor(Color.WHITE);
		
		selectionCount = new MapText(context);
		selectionCount.setBackgroundColor(Color.WHITE);
		
		selectSelection = createSelectButton(context);
		restrictSelection = createRestrictSelectButton(context);
		
		updateLayout();
	}
	
	protected void updateLayout() {
		if (layout != null) {
			layout.removeAllViews();
			layout.addView(selectSelection);
			layout.addView(restrictSelection);
			layout.addView(selectedSelection);
			layout.addView(restrictedSelection);
			layout.addView(selectionCount);
		}
	}
	
	@Override
	public void activate() {
		setSelectedSelection(mapView.getSelectedSelection());
		setRestrictedSelection(mapView.getRestrictedSelection());
	}
	
	@Override
	public void deactivate() {
		
	}
	
	@Override
	public void onSelectionChanged() {
		setSelectedSelection(mapView.getSelectedSelection());
	}
	
	private MapButton createSelectButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Select Selection");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final List<GeometrySelection> selections = SelectionTool.this.mapView.getSelections();
				
				if (selections.isEmpty()) {
					showError("No selections found");
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setTitle("Select Selection");
					
					ArrayList<String> selectionNames = new ArrayList<String>();
					for (GeometrySelection set : selections) {
						selectionNames.add(set.getName());
					}
					
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(SelectionTool.this.context, android.R.layout.simple_list_item_1, selectionNames);
					
					ListView listView = new ListView(context);
					listView.setAdapter(adapter);
					
					builder.setView(listView);
					
					final Dialog d = builder.create();
					
					listView.setOnItemClickListener(new OnItemClickListener() {
	
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1,
								int position, long arg3) {
							d.dismiss();
							setSelectedSelection(selections.get(position));
						}
	
					});
					
					d.show();
				}
			}
			
		});
		return button;
	}
	
	private MapButton createRestrictSelectButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Select Restricted Selection");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final List<GeometrySelection> selections = SelectionTool.this.mapView.getSelections();
				
				if (selections.isEmpty()) {
					showError("No restricted selections found");
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setTitle("Select Restricted Selection");
					
					ArrayList<String> selectionNames = new ArrayList<String>();
					for (GeometrySelection set : selections) {
						selectionNames.add(set.getName());
					}
					
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(SelectionTool.this.context, android.R.layout.simple_list_item_1, selectionNames);
					
					ListView listView = new ListView(context);
					listView.setAdapter(adapter);
					
					builder.setView(listView);
					
					final Dialog d = builder.create();
					
					listView.setOnItemClickListener(new OnItemClickListener() {
	
						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1,
								int position, long arg3) {
							d.dismiss();
							setRestrictedSelection(selections.get(position));
						}

					});
					
					d.show();
				}
			}
			
		});
		return button;
	}

	private void setRestrictedSelection(GeometrySelection set) {
		if(set == null){
			restrictedSelection.setText("No restricted selection selected");
		}else{
			if(mapView.getSelectedSelection() == null){
				showError("No selections found, please select a selection first");
			}else{
				if(mapView.getSelectedSelection().equals(set)){
					showError("Restricted selection can not be the same as the selected selection");
				}else{
					mapView.setRestrictedSelection(set);
					restrictedSelection.setText("Current Restriction: " + set.getName());
				}
			}
		}
		
	}
	protected void setSelectedSelection(GeometrySelection set) {
		if (set == null) {
			selectedSelection.setText("No selection selected");
			selectionCount.setVisibility(View.GONE);
		} else {
			if(mapView.getRestrictedSelection() != null){
				if(mapView.getRestrictedSelection().equals(set)){
					showError("Selected selection can not be the same as the restricted selection");
				}else{
					mapView.setSelectedSelection(set);
					selectedSelection.setText("Current Selection: " + set.getName());
					selectionCount.setVisibility(View.VISIBLE);
					selectionCount.setText("Selection Count: " + set.getDataList().size());
				}
			}else{
				mapView.setSelectedSelection(set);
				selectedSelection.setText("Current Selection: " + set.getName());
				selectionCount.setVisibility(View.VISIBLE);
				selectionCount.setText("Selection Count: " + set.getDataList().size());
			}
		}
	}

}
