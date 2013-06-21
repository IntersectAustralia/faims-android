package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

public class GeometriesIntersectSelectionTool extends HighlightSelectionTool {

	private static final String NAME = "Geometries Intersect Selection";
	private MapButton settingsButton;
	protected SettingsDialog settingsDialog;
	private MapButton clearButton;

	public GeometriesIntersectSelectionTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		settingsButton = createSettingsButton(context);
		clearButton = createClearButton(context);
		
		updateLayout();
	}
	
	@Override
	protected void updateLayout() {
		if (settingsButton != null) {
			layout.removeAllViews();
			layout.addView(settingsButton);
			layout.addView(selectSelection);
			layout.addView(clearButton);
			layout.addView(selectedSelection);
			layout.addView(selectionCount);
		}
	}
	
	private MapButton createClearButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Clear");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clearSelection();
			}
			
		});
		return button;
	}
	
	protected MapButton createSettingsButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Run selection");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				SettingsDialog.Builder builder = new SettingsDialog.Builder(context);
				builder.addCheckBox("remove", "Remove Selection:", false);
				
				builder.setPositiveButton("Run Query", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							boolean remove = settingsDialog.parseCheckBox("remove");
							
							if (mapView.getHighlights().size() == 0) {
								showError("Please select a geometry");
								return;
							}
							
							List<Geometry> transformedGeometries = new ArrayList<Geometry>();
							for(Geometry geometry : mapView.getHighlights()){
								transformedGeometries.add(GeometryUtil.convertGeometryToWgs84(geometry));
							}
							
							mapView.runIntersectionSelection(transformedGeometries, remove);
						} catch (Exception e) {
							FLog.e("error running polygon selection query", e);
							showError(e.getMessage());
						}
					}
				});
				
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// ignore
					}
				});
				
				settingsDialog = builder.create();
				settingsDialog.show();
			}
				
		});
		return button;
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		super.onVectorElementClicked(element, arg1, arg2, arg3);
	}

}
