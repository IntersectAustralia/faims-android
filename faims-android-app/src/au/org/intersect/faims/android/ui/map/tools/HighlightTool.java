package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.nutiteq.WKTUtil;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.form.MapToggleButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

public class HighlightTool extends SettingsTool {
	
	public static final String NAME = "Highlight";
	
	protected MapButton clearButton;

	private MapToggleButton detailButton;

	protected SettingsDialog settingsDialog;
	
	public HighlightTool(Context context, CustomMapView mapView) {
		this(context, mapView, NAME);
	}
	
	public HighlightTool(Context context, CustomMapView mapView, String name) {
		super(context, mapView, name);
		
		detailButton = createDetailButton(context);
		clearButton = createClearButton(context);
		
		updateLayout();
	}
	
	@Override
	protected void updateLayout() {
		super.updateLayout();
		if (detailButton != null) layout.addView(detailButton);
		if (clearButton != null) layout.addView(clearButton);
	}
	
	@Override
	public void activate() {
		detailButton.setChecked(false);
		updateDetailButton();
		mapView.setDrawViewDetail(false);
		mapView.setEditViewDetail(false);
		clearSelection();
	}
	
	@Override
	public void deactivate() {
		detailButton.setChecked(false);
		updateDetailButton();
		mapView.setDrawViewDetail(false);
		mapView.setEditViewDetail(false);
		clearSelection();
	}
	
	@Override
	public void onLayersChanged() {
		try {
			mapView.updateHighlights();
		} catch (Exception e) {
			FLog.e("error updating selection", e);
			showError(e.getMessage());
		}
	}
	
	private MapToggleButton createDetailButton(final Context context) {
		MapToggleButton button = new MapToggleButton(context);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateDetailButton();
				mapView.setDrawViewDetail(detailButton.isChecked());
				mapView.setEditViewDetail(detailButton.isChecked());
			}
			
		});
		return button;
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
	
	protected void clearSelection() {
		try {
			mapView.clearHighlights();
		} catch (Exception e) {
			FLog.e("error clearing selection", e);
			showError(e.getMessage());
		}
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (element instanceof Geometry) {
			try {
				Geometry geom = (Geometry) element;
				
				if (mapView.hasHighlight(geom)) {
					mapView.removeHighlight(geom);
				} else {
					mapView.addHighlight(geom);
				}
				
				FLog.d(WKTUtil.geometryToWKT(GeometryUtil.convertGeometryToWgs84(geom)));
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}
	
	private void updateDetailButton() {
		detailButton.setText(detailButton.isChecked() ? "Hide Details" : "Show Details");
	}

	@Override
	protected MapButton createSettingsButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Style Tool");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				SettingsDialog.Builder builder = new SettingsDialog.Builder(context);
				builder.setTitle("Style Settings");
				
				builder.addTextField("color", "Select Color:", Integer.toHexString(mapView.getDrawViewColor()));
				builder.addSlider("strokeSize", "Stroke Size:", mapView.getDrawViewStrokeStyle());
				builder.addSlider("textSize", "Text Size:", mapView.getDrawViewTextSize());
				builder.addCheckBox("showDegrees", "Show Degrees:", !mapView.showDecimal());
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = settingsDialog.parseColor("color");
							float strokeSize = settingsDialog.parseSlider("strokeSize");
							float textSize = settingsDialog.parseSlider("textSize");
							boolean showDecimal = !settingsDialog.parseCheckBox("showDegrees");
							
							mapView.setDrawViewColor(color);
							mapView.setDrawViewStrokeStyle(strokeSize);
							mapView.setDrawViewTextSize(textSize);
							mapView.setEditViewTextSize(textSize);
							mapView.setShowDecimal(showDecimal);
						} catch (Exception e) {
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
}
