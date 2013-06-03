package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;

public class PointSelectionTool extends HighlightSelectionTool {

	private static final String NAME = "Point Selection";
	private MapButton settingsButton;
	protected float distance;
	protected SettingsDialog settingsDialog;

	public PointSelectionTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		settingsButton = createSettingsButton(context);
		
		updateLayout();
	}
	
	@Override
	protected void updateLayout() {
		if (settingsButton != null) {
			layout.removeAllViews();
			layout.addView(settingsButton);
			layout.addView(selectSelection);
			layout.addView(selectedSelection);
			layout.addView(selectionCount);
		}
	}
	
	protected MapButton createSettingsButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Set Distance");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				SettingsDialog.Builder builder = new SettingsDialog.Builder(context);
				builder.setTitle("Set Distance");
				
				builder.addTextField("distance", "Distance (m):", Float.toString(distance));
				builder.addCheckBox("remove", "Remove Selection:", false);
				
				builder.setPositiveButton("Run Query", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
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
						} catch (Exception e) {
							FLog.e("error running point selection query", e);
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
		if ((element instanceof Point) && (mapView.getHighlights().size() < 2)) {
			super.onVectorElementClicked(element, arg1, arg2, arg3);
		}
	}

}
