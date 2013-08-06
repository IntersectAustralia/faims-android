package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.button.ClearButton;
import au.org.intersect.faims.android.ui.map.button.PropertiesButton;
import au.org.intersect.faims.android.ui.map.button.ToolBarButton;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

public class GeometriesIntersectSelectionTool extends HighlightSelectionTool {

	public static final String NAME = "Geometries Intersect Selection";
	private PropertiesButton settingsButton;
	protected SettingsDialog settingsDialog;
	private ClearButton clearButton;

	public GeometriesIntersectSelectionTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		settingsButton = createSettingsButton(context);
		clearButton = createClearButton(context);
		RelativeLayout.LayoutParams settingsParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		settingsParams.alignWithParent = true;
		settingsParams.addRule(RelativeLayout.ALIGN_LEFT);
		settingsParams.topMargin = (int) ScaleUtil.getDip(context, buttons.size() * HEIGHT + TOP_MARGIN);
		settingsButton.setLayoutParams(settingsParams);
		buttons.add(settingsButton);
		RelativeLayout.LayoutParams clearParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		clearParams.alignWithParent = true;
		clearParams.addRule(RelativeLayout.ALIGN_RIGHT);
		clearParams.addRule(RelativeLayout.ALIGN_BOTTOM);
		clearParams.bottomMargin = (int) ScaleUtil.getDip(context, BOTTOM_MARGIN);
		clearButton.setLayoutParams(clearParams);
		
		updateLayout();
	}
	
	@Override
	protected void updateLayout() {
		if (settingsButton != null) {
			layout.removeAllViews();
			layout.addView(selectionManagerButton);
			layout.addView(settingsButton);
			layout.addView(clearButton);
			layout.addView(infoLayout);
		}
	}
	
	private ClearButton createClearButton(final Context context) {
		ClearButton button = new ClearButton(context);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clearSelection();
			}
			
		});
		return button;
	}
	
	protected PropertiesButton createSettingsButton(final Context context) {
		PropertiesButton button = new PropertiesButton(context);
		button.setLabel("Query");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				SettingsDialog.Builder builder = new SettingsDialog.Builder(context);
				builder.setTitle("Intersection Tool");
				builder.addCheckBox("remove", "Remove from selection?", false);
				
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
	
	public ToolBarButton getButton(Context context) {
		ToolBarButton button = new ToolBarButton(context);
		button.setLabel("Intersect");
		button.setSelectedState(R.drawable.tools_select_intersect_s);
		button.setNormalState(R.drawable.tools_select_intersect);
		return button;
	}

}
