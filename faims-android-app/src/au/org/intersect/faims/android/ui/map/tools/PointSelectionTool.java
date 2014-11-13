package au.org.intersect.faims.android.ui.map.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import au.org.intersect.faims.android.two.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.button.ClearButton;
import au.org.intersect.faims.android.ui.map.button.PropertiesButton;
import au.org.intersect.faims.android.ui.map.button.ToolBarButton;
import au.org.intersect.faims.android.ui.view.MapText;
import au.org.intersect.faims.android.util.GeometryUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;

public class PointSelectionTool extends HighlightSelectionTool {

	public static final String NAME = "Point Selection";
	private PropertiesButton settingsButton;
	protected float distance = 0;
	protected SettingsDialog settingsDialog;
	private ClearButton clearButton;
	private MapText selectedDistance;

	public PointSelectionTool(Context context, CustomMapView mapView) {
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
		
		int padding = (int) ScaleUtil.getDip(context, PADDING);
		
		selectedDistance = new MapText(context);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		selectedDistance.setTextColor(Color.WHITE);
		selectedDistance.setBackgroundColor(Color.parseColor("#88000000"));
		selectedDistance.setLayoutParams(layoutParams);
		selectedDistance.setText("Current Distance: " + distance + " m");
		selectedDistance.setPadding(padding, 0, padding, padding);
		infoLayout.addView(selectedDistance);
		updateLayout();
	}
	
	@Override
	public void activate() {
		super.activate();
		if (!mapView.isProperProjection()) {
			showError("This tool will not function properly as projection is not a projected coordinate system.");
		}
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
		button.setLabel("Distance");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				SettingsDialog.Builder builder = new SettingsDialog.Builder(context);
				builder.setTitle("Point Selection Distance");
				
				builder.addTextField("distance", "Distance (m):", Float.toString(distance));
				builder.addCheckBox("remove", "Remove from selection?", false);
				
				builder.setPositiveButton("Run Query", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// ignore
					}
				});
				
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// ignore
					}
				});
				
				settingsDialog = builder.create();
				settingsDialog.setCanceledOnTouchOutside(true);
				settingsDialog.show();
				
				settingsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View arg0) {
						try {
							float distance = Float.parseFloat(((EditText) settingsDialog.getField("distance")).getText().toString());
							boolean remove = settingsDialog.parseCheckBox("remove");
							
							PointSelectionTool.this.distance = distance;
							
							if (mapView.getHighlights().size() == 0) {
								showError("Please select a point");
								return;
							}
							
							Point point = (Point) mapView.getHighlights().get(0);
							
							mapView.runPointSelection((Point) GeometryUtil.convertGeometryToWgs84(point), distance, remove);
							selectedDistance.setText("Current Distance: " + distance + " m");
							
							settingsDialog.dismiss();
						} catch (NumberFormatException e) {
							FLog.e("error setting config", e);
							showError("Please enter a float value");
						} catch (Exception e) {
							FLog.e("error running point selection query", e);
							showError(e.getMessage());
						}
					}
					
				});
			}
				
		});
		return button;
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if ((element instanceof Point) && (mapView.getHighlights().size() < 1)) {
			super.onVectorElementClicked(element, arg1, arg2, arg3);
		}
	}
	
	public ToolBarButton getButton(Context context) {
		ToolBarButton button = new ToolBarButton(context);
		button.setLabel("Point");
		button.setSelectedState(R.drawable.tools_select_point_s);
		button.setNormalState(R.drawable.tools_select_point);
		return button;
	}

}
