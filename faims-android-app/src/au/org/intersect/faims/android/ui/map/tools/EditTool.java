package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryData;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.ui.dialog.LineStyleDialog;
import au.org.intersect.faims.android.ui.dialog.PointStyleDialog;
import au.org.intersect.faims.android.ui.dialog.PolygonStyleDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.button.BreakButton;
import au.org.intersect.faims.android.ui.map.button.DeleteButton;
import au.org.intersect.faims.android.ui.map.button.LockButton;
import au.org.intersect.faims.android.ui.map.button.PropertiesButton;
import au.org.intersect.faims.android.ui.map.button.ToolBarButton;
import au.org.intersect.faims.android.util.GeometryUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.geometry.VectorElement;

public class EditTool extends HighlightTool {
	
	public static final String NAME = "Edit";
	
	private LockButton lockButton;

	private PropertiesButton propertiesButton;

	private DeleteButton deleteButton;

	private PointStyleDialog pointStyleDialog;

	private LineStyleDialog lineStyleDialog;

	private PolygonStyleDialog polygonStyleDialog;

	private BreakButton editVertexButton;

	protected List<Geometry> vertexGeometry;

	protected HashMap<Geometry, ArrayList<Point>> vertexGeometryToPointsMap;
	
	public EditTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		propertiesButton = createPropertiesButton(context);
		RelativeLayout.LayoutParams propertiesParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		propertiesParams.alignWithParent = true;
		propertiesParams.addRule(RelativeLayout.ALIGN_LEFT);
		propertiesParams.topMargin = (int) ScaleUtil.getDip(context, buttons.size() * HEIGHT + TOP_MARGIN);
		propertiesButton.setLayoutParams(propertiesParams);
		buttons.add(propertiesButton);

		lockButton = createLockButton(context);
		RelativeLayout.LayoutParams lockParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lockParams.alignWithParent = true;
		lockParams.addRule(RelativeLayout.ALIGN_LEFT);
		lockParams.topMargin = (int) ScaleUtil.getDip(context, buttons.size() * HEIGHT + TOP_MARGIN);
		lockButton.setLayoutParams(lockParams);
		buttons.add(lockButton);

		deleteButton = createDeleteButton(context);
		RelativeLayout.LayoutParams deleteParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		deleteParams.alignWithParent = true;
		deleteParams.addRule(RelativeLayout.ALIGN_LEFT);
		deleteParams.addRule(RelativeLayout.ALIGN_BOTTOM);
		deleteParams.bottomMargin = (int) ScaleUtil.getDip(context, BOTTOM_MARGIN);
		deleteButton.setLayoutParams(deleteParams);

		editVertexButton = createEditVertexButton(context);
		RelativeLayout.LayoutParams editVertexParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		editVertexParams.alignWithParent = true;
		editVertexParams.addRule(RelativeLayout.ALIGN_LEFT);
		editVertexParams.topMargin = (int) ScaleUtil.getDip(context, buttons.size() * HEIGHT + TOP_MARGIN);
		editVertexButton.setLayoutParams(editVertexParams);
		buttons.add(editVertexButton);
		
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
		if (propertiesButton != null) layout.addView(propertiesButton);
		if (lockButton != null) layout.addView(lockButton);
		if (deleteButton != null) layout.addView(deleteButton);
		if (editVertexButton != null) layout.addView(editVertexButton);
	}
	
	public void resetVertexGeometry() {
		editVertexButton.setChecked(false);
		try {
			if (EditTool.this.vertexGeometry != null) {
				for (Geometry geom : EditTool.this.vertexGeometry) {
					GeometryData data = (GeometryData) geom.userData;
					
					if (data.id == null) {
						if (geom instanceof Line) {
							EditTool.this.mapView.drawLine(data.layerId, GeometryUtil.convertToWgs84(((Line) geom).getVertexList()), data.style);
						} else if (geom instanceof Polygon) {
							EditTool.this.mapView.drawPolygon(data.layerId, GeometryUtil.convertToWgs84(((Polygon) geom).getVertexList()), data.style);
						}
					} else {
						mapView.clearHiddenGeometry(geom);
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
			showError("Error resetting vertex geometry");
		}
		EditTool.this.vertexGeometry = null;
		EditTool.this.vertexGeometryToPointsMap = null;
	}
	
	private BreakButton createEditVertexButton(final Context context) {
		final BreakButton button = new BreakButton(context);
		button.setChecked(false);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					if (EditTool.this.mapView.hasTransformGeometry()) {
						showError("Please clear locked geometry before joining");
						button.setChecked(!button.isChecked());
						return;
					}
					if (hasLegacyGeometry()) {
						showError("Cannot edit legacy geometry");
						button.setChecked(!button.isChecked());
						return;
					}
					
					if (!button.isChecked()) {
						
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
							
							if (data.id == null) {
								if (geom instanceof Line) {
									EditTool.this.mapView.drawLine(data.layerId, pts, data.style);
								} else if (geom instanceof Polygon) {
									EditTool.this.mapView.drawPolygon(data.layerId, pts, data.style);
								}
							} else {
								
								mapView.clearHiddenGeometry(geom);
								
								if (geom instanceof Line) {
									mapView.saveGeometry(new Line(pts, null, data.style.toLineStyleSet(), data));
								} else if (geom instanceof Polygon) {
									mapView.saveGeometry(new Polygon(pts, null, null, data.style.toPolygonStyleSet(), data));
								}
							}
								
						}
						
						EditTool.this.vertexGeometry = null;
						EditTool.this.vertexGeometryToPointsMap = null;
						
					} else {
						if (hasPointGeometry()) {
							showError("Cannot break point geometry");
							button.setChecked(!button.isChecked());
							return;
						}
						if (mapView.getHighlights().size() == 0) {
							showError("Pleas select geometry");
							button.setChecked(!button.isChecked());
							return;
						}
						
						List<Geometry> list = EditTool.this.mapView.getHighlights();
						List<Geometry> vertexGeometry = new ArrayList<Geometry>();
						HashMap<Geometry, ArrayList<Point>> vertexGeometryToPointsMap = new HashMap<Geometry, ArrayList<Point>>();
						for (Geometry geom : list) {
							vertexGeometry.add(geom);
							GeometryStyle vertexStyle = GeometryStyle.defaultPointStyle();
							vertexStyle.size = mapView.getVertexSize();
							GeometryData data = (GeometryData) geom.userData;
							if (data.id == null) {
								EditTool.this.mapView.clearGeometry(geom);
							} else {
								EditTool.this.mapView.hideGeometry(geom);
							}
							
							if (geom instanceof Line) {
								Line line = (Line) geom;
								
								vertexStyle.pointColor = data.style.pointColor > 0 ? data.style.pointColor : data.style.lineColor;
								vertexStyle.pointColor = vertexStyle.pointColor | 0xFF000000;
								
								ArrayList<Point> geometryPoints = new ArrayList<Point>();
								for (MapPos p : line.getVertexList()) {
									geometryPoints.add(EditTool.this.mapView.drawPoint(mapView.getVertexLayerId(), GeometryUtil.convertToWgs84(p), vertexStyle));
								}
								vertexGeometryToPointsMap.put(geom, geometryPoints);
							} else if (geom instanceof Polygon) {
								Polygon polygon = (Polygon) geom;
								
								vertexStyle.pointColor = data.style.pointColor > 0 ? data.style.pointColor : data.style.polygonColor;
								vertexStyle.pointColor = vertexStyle.pointColor | 0xFF000000;
								
								ArrayList<Point> geometryPoints = new ArrayList<Point>();
								for (MapPos p : polygon.getVertexList()) {
									geometryPoints.add(EditTool.this.mapView.drawPoint(mapView.getVertexLayerId(), GeometryUtil.convertToWgs84(p), vertexStyle));
								}
								vertexGeometryToPointsMap.put(geom, geometryPoints);
							}
							mapView.clearHighlights();
							
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
	
	private boolean hasLegacyGeometry() {
		List<Geometry> list = EditTool.this.mapView.getHighlights();
		for (Geometry geom : list) {
			GeometryData data = (GeometryData) geom.userData;
			if (data.type == GeometryData.Type.LEGACY) return true;
		}
		return false;
	}
	
	private boolean hasDatabaseGeometry() {
		List<Geometry> list = EditTool.this.mapView.getHighlights();
		for (Geometry geom : list) {
			GeometryData data = (GeometryData) geom.userData;
			if (data.type == GeometryData.Type.ENTITY || data.type == GeometryData.Type.RELATIONSHIP) return true;
		}
		return false;
	}
	
	private boolean hasPointGeometry() {
		List<Geometry> list = EditTool.this.mapView.getHighlights();
		for (Geometry geom : list) {
			if (geom instanceof Point) return true;
		}
		return false;
	}
	
	private LockButton createLockButton(final Context context) {
		final LockButton button = new LockButton(context);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (hasLegacyGeometry()) {
					button.setChecked(!button.isChecked());
					return;
				}
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
	
	private DeleteButton createDeleteButton(final Context context) {
		DeleteButton button = new DeleteButton(context);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					if (hasLegacyGeometry()) {
						showError("Cannot edit legacy geometry");
						return;
					}
					
					List<Geometry> selection = EditTool.this.mapView.getHighlights();
					
					for (Geometry geom : selection) {
						if(geom.userData != null){
							GeometryData data = (GeometryData) geom.userData;
							if(GeometryData.Type.ENTITY.equals(data.type)){
								EditTool.this.mapView.deleteGeometry(geom);
							}else if(GeometryData.Type.RELATIONSHIP.equals(data.type)){
								EditTool.this.mapView.deleteGeometry(geom);
							}else if (GeometryData.Type.LEGACY.equals(data.type)) {
								// ignore
							} else {
								EditTool.this.mapView.clearGeometry(geom);
							}
						}
					}
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
					showError(e.getMessage());
				}
			}
			
		});
		return button;
	}
	
	protected PropertiesButton createPropertiesButton(final Context context) {
		PropertiesButton button = new PropertiesButton(context);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// get selected geometry
				List<Geometry> selection = mapView.getHighlights();
				if (selection.size() != 1) {
					showError("Please select one geometry to edit");
					return;
				}
				
				if (hasLegacyGeometry()) {
					showError("Cannot edit legacy geometry");
					return;
				}
				if (hasDatabaseGeometry()) {
					showError("Cannot edit database geometry");
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
			super.onVectorElementClicked(element, arg1, arg2, arg3);
		}
	}
	
	public ToolBarButton getButton(Context context) {
		ToolBarButton button = new ToolBarButton(context);
		button.setLabel("Edit");
		button.setMutatedSelectedState(R.drawable.ic_menu_edit);
		button.setNormalState(R.drawable.ic_menu_edit);
		return button;
	}
}
