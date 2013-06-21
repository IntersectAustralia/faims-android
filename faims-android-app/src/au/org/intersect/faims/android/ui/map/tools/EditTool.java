package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryData;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.dialog.LineStyleDialog;
import au.org.intersect.faims.android.ui.dialog.PointStyleDialog;
import au.org.intersect.faims.android.ui.dialog.PolygonStyleDialog;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.form.MapToggleButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.components.MapPos;
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

	private MapToggleButton vertexButton;

	protected float vertexSize = 0.2f;

	protected List<Geometry> vertexGeometry;

	protected HashMap<Geometry, ArrayList<Point>> vertexGeometryToPointsMap;
	
	public EditTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		lockButton = createLockButton(context);
		propertiesButton = createPropertiesButton(context);
		deleteButton = createDeleteButton(context);
		vertexButton = createVertexButton(context);
		
		updateLayout();
	}
	
	@Override
	public void activate() {
		clearLock();
		resetVertexGeometry();
		super.activate();
	}
	
	@Override
	public void deactivate() {
		clearLock();
		resetVertexGeometry();
		super.activate();
	}
	
	@Override
	public void onLayersChanged() {
		clearLock();
		resetVertexGeometry();
		super.onLayersChanged();
	}
	
	@Override
	protected void updateLayout() {
		super.updateLayout();
		if (lockButton != null) layout.addView(lockButton);
		if (propertiesButton != null) layout.addView(propertiesButton);
		if (deleteButton != null) layout.addView(deleteButton);
		if (vertexButton != null) layout.addView(vertexButton);
	}
	
	public void resetVertexGeometry() {
		vertexButton.setChecked(false);
		try {
			if (EditTool.this.vertexGeometry != null) {
				for (Geometry geom : EditTool.this.vertexGeometry) {
					GeometryData data = (GeometryData) geom.userData;
					if (geom instanceof Line) {
						EditTool.this.mapView.drawLine(data.layerId, GeometryUtil.convertToWgs84(((Line) geom).getVertexList()), data.style);
					} else if (geom instanceof Polygon) {
						EditTool.this.mapView.drawPolygon(data.layerId, GeometryUtil.convertToWgs84(((Polygon) geom).getVertexList()), data.style);
					}
					
					ArrayList<Point> geometryPoints = EditTool.this.vertexGeometryToPointsMap.get(geom);
					for (Point point : geometryPoints) {
						// check if point exists
						GeometryData pointData = (GeometryData) point.userData;
						Point realPoint = (Point) EditTool.this.mapView.getGeometry(pointData.geomId);
						if (realPoint != null) {
							EditTool.this.mapView.clearGeometry(realPoint);
						}
					}
				}
				
			}
		} catch (Exception e) {
			FLog.e("error resetting vertex geometry", e);
			showError("error resetting vertex geometry");
		}
		EditTool.this.vertexGeometry = null;
		EditTool.this.vertexGeometryToPointsMap = null;
	}
	
	private MapToggleButton createVertexButton(final Context context) {
		final MapToggleButton button = new MapToggleButton(context);
		button.setTextOn("Join");
		button.setTextOff("Break");
		button.setChecked(false);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					if (!button.isChecked()) {
						
						if (EditTool.this.mapView.hasTransformGeometry()) {
							showError("Please clear locked geometry before joining");
							button.setChecked(true);
							return;
						}
						
						for (Geometry geom : EditTool.this.vertexGeometry) {
							ArrayList<Point> geometryPoints = EditTool.this.vertexGeometryToPointsMap.get(geom);
							ArrayList<MapPos> pts = new ArrayList<MapPos>();
							for (Point point : geometryPoints) {
								// check if point exists
								GeometryData pointData = (GeometryData) point.userData;
								Point realPoint = (Point) EditTool.this.mapView.getGeometry(pointData.geomId);
								if (realPoint != null) {
									pts.add(GeometryUtil.convertToWgs84(realPoint.getMapPos()));
									EditTool.this.mapView.clearGeometry(realPoint);
								}
							}

							GeometryData data = (GeometryData) geom.userData;
							if (geom instanceof Line) {
								EditTool.this.mapView.drawLine(data.layerId, pts, data.style);
							} else if (geom instanceof Polygon) {
								EditTool.this.mapView.drawPolygon(data.layerId, pts, data.style);
							}
						}
						
						EditTool.this.vertexGeometry = null;
						EditTool.this.vertexGeometryToPointsMap = null;
						
					} else {
						List<Geometry> list = EditTool.this.mapView.getHighlights();
						List<Geometry> vertexGeometry = new ArrayList<Geometry>();
						HashMap<Geometry, ArrayList<Point>> vertexGeometryToPointsMap = new HashMap<Geometry, ArrayList<Point>>();
						for (Geometry geom : list) {
							if (geom instanceof Line) {
								vertexGeometry.add(geom);
								EditTool.this.mapView.clearGeometry(geom);
								
								Line line = (Line) geom;
								GeometryData data = (GeometryData) geom.userData;
								GeometryStyle vertexStyle = GeometryStyle.defaultPointStyle();
								vertexStyle.pointColor = data.style.pointColor > 0 ? data.style.pointColor : data.style.lineColor;
								vertexStyle.size = vertexSize;
								ArrayList<Point> geometryPoints = new ArrayList<Point>();
								for (MapPos p : line.getVertexList()) {
									geometryPoints.add(EditTool.this.mapView.drawPoint(data.layerId, GeometryUtil.convertToWgs84(p), vertexStyle));
								}
								vertexGeometryToPointsMap.put(geom, geometryPoints);
							} else if (geom instanceof Polygon) {
								vertexGeometry.add(geom);
								EditTool.this.mapView.clearGeometry(geom);
								
								Polygon polygon = (Polygon) geom;
								GeometryData data = (GeometryData) geom.userData;
								GeometryStyle vertexStyle = GeometryStyle.defaultPointStyle();
								vertexStyle.pointColor = data.style.pointColor > 0 ? data.style.pointColor : data.style.lineColor;
								vertexStyle.size = vertexSize;
								ArrayList<Point> geometryPoints = new ArrayList<Point>();
								for (MapPos p : polygon.getVertexList()) {
									geometryPoints.add(EditTool.this.mapView.drawPoint(data.layerId, GeometryUtil.convertToWgs84(p), vertexStyle));
								}
								vertexGeometryToPointsMap.put(geom, geometryPoints);
							}
							
							EditTool.this.vertexGeometry = vertexGeometry;
							EditTool.this.vertexGeometryToPointsMap = vertexGeometryToPointsMap;
						}
					}
				} catch (Exception e) {
					FLog.e("error generating geometry vertices", e);
					showError("Error generating geometry vertices");
				}
				
			}
			
		});
		return button;
	}
	
	private MapToggleButton createLockButton(final Context context) {
		MapToggleButton button = new MapToggleButton(context);
		button.setTextOn("UnLock");
		button.setTextOff("Lock");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateLock();
			}
			
		});
		return button;
	}
	
	private void updateLock() {
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
				final boolean isEPSG4326 = GeometryUtil.EPSG4326.equals(mapView.getActivity().getProject().getSrid());
				if (isEPSG4326)
					builder.addCheckBox("showDegrees", "Show Degrees:", !mapView.showDecimal());
				builder.addSlider("vertexSize", "Vertex Size:", vertexSize);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = settingsDialog.parseColor("color");
							int editColor = settingsDialog.parseColor("editColor");
							float strokeSize = settingsDialog.parseSlider("strokeSize");
							float textSize = settingsDialog.parseSlider("textSize");
							boolean showDecimal;
							if (isEPSG4326)
								showDecimal = !settingsDialog.parseCheckBox("showDegrees");
							else
								showDecimal = false;
							float vertexSize = settingsDialog.parseSlider("vertexSize");
							
							mapView.setDrawViewColor(color);
							mapView.setEditViewColor(editColor);
							mapView.setDrawViewStrokeStyle(strokeSize);
							mapView.setDrawViewTextSize(textSize);
							mapView.setEditViewTextSize(textSize);
							mapView.setShowDecimal(showDecimal);
							
							EditTool.this.vertexSize = vertexSize;
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
