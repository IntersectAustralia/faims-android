package au.org.intersect.faims.android.ui.map.tools;

import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryData;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.ui.dialog.LineStyleDialog;
import au.org.intersect.faims.android.ui.dialog.PointStyleDialog;
import au.org.intersect.faims.android.ui.dialog.PolygonStyleDialog;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.form.MapToggleButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.geometry.VectorElement;

public class EditTool extends HighlightTool {
	
	public static final String NAME = "Edit";
	
	private MapToggleButton lockButton;

	private MapButton propertiesButton;

	private MapButton deleteButton;

	private PointStyleDialog pointStyleDialog;

	private LineStyleDialog lineStyleDialog;

	private PolygonStyleDialog polygonStyleDialog;
	
	public EditTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		lockButton = createLockButton(context);
		propertiesButton = createPropertiesButton(context);
		deleteButton = createDeleteButton(context); 
				
		updateLockButton();
		
		updateLayout();
	}
	
	@Override
	public void activate() {
		clearLock();
		super.activate();
	}
	
	@Override
	public void deactivate() {
		clearLock();
		super.activate();
	}
	
	@Override
	public void onLayersChanged() {
		clearLock();
		super.onLayersChanged();
	}
	
	@Override
	protected void updateLayout() {
		super.updateLayout();
		if (lockButton != null) layout.addView(lockButton);
		if (propertiesButton != null) layout.addView(propertiesButton);
		if (deleteButton != null) layout.addView(deleteButton);
	}
	
	private MapToggleButton createLockButton(final Context context) {
		MapToggleButton button = new MapToggleButton(context);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateLock();
			}
			
		});
		return button;
	}
	
	private void updateLockButton() {
		lockButton.setText(lockButton.isChecked() ? "UnLock" : "Lock");
	}
	
	private void updateLock() {
		updateLockButton();
		try {
			if (lockButton.isChecked()) {
				mapView.prepareHighlightTransform();
			} else {
				mapView.doHighlightTransform();
			}
		} catch (Exception e) {
			FLog.e("error doing selection transform", e);
			showError(e.getMessage());
		}
	}
	
	private void clearLock() {
		lockButton.setChecked(false);
		updateLockButton();
		mapView.clearHighlightTransform();
	}
	
	protected void clearSelection() {
		clearLock();
		super.clearSelection();
	}
	
	private MapButton createDeleteButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Delete");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					List<Geometry> selection = EditTool.this.mapView.getHighlights();
					EditTool.this.mapView.clearGeometryList(selection);
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
					showError(e.getMessage());
				}
			}
			
		});
		return button;
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
				builder.addTextField("editColor", "Edit Color:", Integer.toHexString(mapView.getEditViewColor()));
				builder.addSlider("strokeSize", "Stroke Size:", mapView.getDrawViewStrokeStyle());
				builder.addSlider("textSize", "Text Size:", mapView.getDrawViewTextSize());
				builder.addCheckBox("showDegrees", "Show Degrees:", !mapView.showDecimal());
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = settingsDialog.parseColor("color");
							int editColor = settingsDialog.parseColor("editColor");
							float strokeSize = settingsDialog.parseSlider("strokeSize");
							float textSize = settingsDialog.parseSlider("textSize");
							boolean showDecimal = !settingsDialog.parseCheckBox("showDegrees");
							
							mapView.setDrawViewColor(color);
							mapView.setEditViewColor(editColor);
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
	
	protected MapButton createPropertiesButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Properties");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// get selected geometry
				List<Geometry> selection = mapView.getHighlights();
				if (selection.size() != 1) {
					showError("Please select only one geometry to edit");
					return;
				}
				
				Geometry geom = selection.get(0);
				
				if (geom instanceof Point) {
					showPointProperties((Point) geom);
				} else if (geom instanceof Line) {
					showLineProperties((Line) geom);
				} else if (geom instanceof Polygon) {
					showPolygonProperties((Polygon) geom);
				}
			}
				
		});
		return button;
	}
	
	private void showPointProperties(final Point point) {
		GeometryData data = (GeometryData) point.userData;
		final GeometryStyle style = data.style;
		PointStyleDialog.Builder builder = new PointStyleDialog.Builder(context, style);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					int minZoom = pointStyleDialog.parseRange("minZoom", 0, FaimsSettings.MAX_ZOOM);
					int color = pointStyleDialog.parseColor("color");
					float size = pointStyleDialog.parseSlider("size");
					float pickingSize = pointStyleDialog.parseSlider("pickingSize");
					
					style.minZoom = minZoom;
					style.pointColor = color;
					style.size = size;
					style.pickingSize = pickingSize;
					
					EditTool.this.mapView.restylePoint(point, style);
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
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
		
		pointStyleDialog = (PointStyleDialog) builder.create();
		pointStyleDialog.show();
	}
	
	private void showLineProperties(final Line line) {
		GeometryData data = (GeometryData) line.userData;
		final GeometryStyle style = data.style;
		LineStyleDialog.Builder builder = new LineStyleDialog.Builder(context, style);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					int minZoom = lineStyleDialog.parseRange("minZoom", 0, FaimsSettings.MAX_ZOOM);
					int color = lineStyleDialog.parseColor("color");
					float size = lineStyleDialog.parseSlider("size");
					float pickingSize = lineStyleDialog.parseSlider("pickingSize");
					float width = lineStyleDialog.parseSlider("width");
					float pickingWidth = lineStyleDialog.parseSlider("pickingWidth");
					boolean showPoints = lineStyleDialog.parseCheckBox("showPoints");
					
					style.minZoom = minZoom;
					style.pointColor = color;
					style.lineColor = color;
					style.size = size;
					style.pickingSize = pickingSize;
					style.width = width;
					style.pickingWidth = pickingWidth;
					style.showPoints = showPoints;
					
					EditTool.this.mapView.restyleLine(line, style);
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
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
		
		lineStyleDialog = (LineStyleDialog) builder.create();
		lineStyleDialog.show();
	}
	
	private void showPolygonProperties(final Polygon polygon) {
		GeometryData data = (GeometryData) polygon.userData;
		final GeometryStyle style = data.style;
		PolygonStyleDialog.Builder builder = new PolygonStyleDialog.Builder(context, style);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					int minZoom = polygonStyleDialog.parseRange("minZoom", 0, FaimsSettings.MAX_ZOOM);
					int color = polygonStyleDialog.parseColor("color");
					float size = polygonStyleDialog.parseSlider("size");
					float pickingSize = polygonStyleDialog.parseSlider("pickingSize");
					int lineColor = polygonStyleDialog.parseColor("strokeColor");
					float width = polygonStyleDialog.parseSlider("width");
					float pickingWidth = polygonStyleDialog.parseSlider("pickingWidth");
					boolean showStroke = polygonStyleDialog.parseCheckBox("showStroke");
					boolean showPoints = polygonStyleDialog.parseCheckBox("showPoints");
					
					style.minZoom = minZoom;
					style.pointColor = lineColor;
					style.lineColor = lineColor;
					style.polygonColor = color;
					style.size = size;
					style.pickingSize = pickingSize;
					style.width = width;
					style.pickingWidth = pickingWidth;
					style.showStroke = showStroke;
					style.showPoints = showPoints;
					
					EditTool.this.mapView.restylePolygon(polygon, style);
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
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
		
		polygonStyleDialog = (PolygonStyleDialog) builder.create();
		polygonStyleDialog.show();
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (!mapView.hasTransformGeometry()) {
			if (element instanceof Geometry) {
				Geometry geom = (Geometry) element;
				if (geom.userData instanceof GeometryData) {
					GeometryData geomData = (GeometryData) geom.userData;
					if (geomData.id == null) {
						super.onVectorElementClicked(element, arg1, arg2, arg3);
					}
				}
			} 
		}
	}
}
