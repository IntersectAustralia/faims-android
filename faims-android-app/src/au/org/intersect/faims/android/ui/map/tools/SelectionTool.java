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
	protected MapButton selectSelection;
	protected MapText selectedSelection;

	public SelectionTool(Context context, CustomMapView mapView, String name) {
		super(context, mapView, name);
		
		layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		container.addView(layout);
		
		selectedSelection = new MapText(context);
		selectedSelection.setBackgroundColor(Color.WHITE);
		
		selectSelection = createSelectButton(context);
		
		updateLayout();
	}
	
	protected void updateLayout() {
		if (layout != null) {
			layout.removeAllViews();
			layout.addView(selectSelection);
			layout.addView(selectedSelection);
		}
	}
	
	@Override
	public void activate() {
		setSelectedSelection(mapView.getSelectedSelection());
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
	
	protected void setSelectedSelection(GeometrySelection set) {
		if (set == null) {
			selectedSelection.setText("No selection selected");
		} else {
			mapView.setSelectedSelection(set);
			selectedSelection.setText("Current Selection: " + set.getName());
		}
	}

}
