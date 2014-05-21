package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.view.View;
import android.view.View.OnClickListener;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.button.SettingsButton;
import au.org.intersect.faims.android.util.GeometryUtil;

import com.google.inject.Inject;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.geometry.VectorElement;

public class PathFollowerTool extends HighlightTool {
	
	@Inject
	DatabaseManager databaseManager;
	
	private class PathFollowerToolCanvas extends ToolCanvas {
		
		private MapPos tp;
		private Geometry geom;

		public PathFollowerToolCanvas(Context context) {
			super(context);
		}

		@Override
		public void onDraw(Canvas canvas) {
			if (isDirty) {
				
				canvas.drawCircle((float) tp.x, (float) tp.y, 10, paint);
				
				if (geom instanceof Polygon) {
					drawPolygonOverlay((Polygon) geom, canvas);
				}
			}
		}

		public void drawPointOnPath(MapPos p) {
			
			this.tp = GeometryUtil.transformVertex(GeometryUtil.convertFromWgs84(p), PathFollowerTool.this.mapView, true);
			
			this.isDirty = true;
			invalidate();
		}

		public void drawBuffer(Geometry bufferGeom) {
			this.isDirty = true;
			
			this.geom = bufferGeom;
			
			invalidate();
		}
		
		private void drawPolygonOverlay(Polygon polygon, Canvas canvas) {
			MapPos lp = null;
			for (MapPos p : polygon.getVertexList()) {
				p = transformPoint(p);
				if (lp != null) {
					canvas.drawLine((float) lp.x, (float) lp.y, (float) p.x, (float) p.y, paint);
				}
				lp = p;
			}
			MapPos p = polygon.getVertexList().get(0);
			p = transformPoint(p);
			canvas.drawLine((float) lp.x, (float) lp.y, (float) p.x, (float) p.y, paint);
		}
		
		protected MapPos transformPoint(MapPos p) {
			return GeometryUtil.transformVertex(p, mapView, true);
		}
		
	}
	
	public static final String NAME = "Path Follower Tool - Debug";
	
	private PathFollowerToolCanvas canvas;

	protected SettingsDialog settingsDialog;

	protected float speed = 0.5f;

	private Thread pathFollowerThread;

	private boolean killFollowerThread;

	private MapPos currentPoint;

	protected float buffer = 0.5f;

	private Geometry bufferGeom;

	public PathFollowerTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		canvas = new PathFollowerToolCanvas(context);
		container.addView(canvas);
	}

	@Override 
	public void activate() {
		super.activate();
		canvas.clear();
	}
	
	@Override
	public void deactivate() {
		super.deactivate();
		canvas.clear();
	}
	
	@Override
	public void onLayersChanged() {
		super.onLayersChanged();
		canvas.clear();
	}
	
	@Override
	public void onMapChanged() {
		super.onMapChanged();
		updatePath();
	}
	
	@Override
	protected void updateLayout() {
		super.updateLayout();
		if (canvas != null) layout.addView(canvas);
	}
	
	@Override
	protected void clearSelection() {
		super.clearSelection();
		canvas.clear();
		stopPathFollower();
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (element instanceof Geometry) {
			try {
				if ((element instanceof Line) && (mapView.getHighlights().size() == 0)) {
					Line line = (Line) element;
					
					if (mapView.hasHighlight(line)) {
						mapView.removeHighlight(line);
					} else {
						mapView.addHighlight(line);
					}
					
					mapView.setGeomToFollow((Line) GeometryUtil.convertGeometryToWgs84(line));
				} else if ((element instanceof Point) && (mapView.getHighlights().size() == 1)) {
					Point p = (Point) element;
					
					if (mapView.hasHighlight(p)) {
						mapView.removeHighlight(p);
					} else {
						mapView.addHighlight(p);
					}
					
					startPathFollower((Point) GeometryUtil.convertGeometryToWgs84(p));
				}
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}
	
	@Override
	public void onMapUpdate() {

		PathFollowerTool.this.updatePath();
	
	}
	
	private void startPathFollower(Point p) {
		killFollowerThread = false;
		currentPoint = p.getMapPos();
		try {
			bufferGeom = databaseManager.spatialRecord().geometryBuffer(mapView.getGeomToFollow(), buffer * 100, mapView.getModuleSrid());
		} catch (Exception e) {
			FLog.e("error gettting geometry buffer", e);
		}
		pathFollowerThread = new Thread(new Runnable() {
			

			@Override
			public void run() {
				while(!killFollowerThread) {
					try {
						MapPos point = PathFollowerTool.this.mapView.nextPointToFollow(currentPoint, buffer * 100);
						FLog.d("Next Point: " + point);
						if (point != null) {
						
							double dx = point.x - currentPoint.x;
							double dy = point.y - currentPoint.y;
							double d = databaseManager.spatialRecord().distanceBetween(point, currentPoint, PathFollowerTool.this.mapView.getModuleSrid());
							
							double nextStep;
							if (d == 0) {
								d = 1;
								nextStep = 1;
							} else {
								nextStep = speed * 100;
								if (nextStep > d) {
									nextStep = d;
								}
							}
							
							currentPoint = new MapPos(currentPoint.x + nextStep * dx / d, currentPoint.y + nextStep * dy / d);
						}
						Thread.sleep(1000);
					} catch (Exception e) {
						FLog.e("error in path follower thread", e);
					}
				}
			}
			
		});
		pathFollowerThread.start();
	}
	
	private void stopPathFollower() {
		killFollowerThread = true;
	}
	
	private void updatePath() {
		if (mapView.getHighlights().size() < 2) return;
		
		canvas.drawPointOnPath(currentPoint);
		canvas.drawBuffer(bufferGeom);
		canvas.setColor(mapView.getDrawViewColor());
		canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
		canvas.setTextSize(mapView.getDrawViewTextSize());
	}
	
	@Override
	protected SettingsButton createSettingsButton(final Context context) {
		SettingsButton button = new SettingsButton(context);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				SettingsDialog.Builder builder = new SettingsDialog.Builder(context);
				builder.setTitle("Style Settings");
				
				builder.addColorField("color", "Select Color:", Integer.toHexString(mapView.getDrawViewColor()));
				builder.addSlider("strokeSize", "Stroke Size:", mapView.getDrawViewStrokeStyle());
				builder.addSlider("textSize", "Text Size:", mapView.getDrawViewTextSize());
				final boolean isEPSG4326 = GeometryUtil.EPSG4326.equals(mapView.getModuleSrid());
				if (isEPSG4326)
					builder.addCheckBox("showDegrees", "Show Degrees:", !mapView.showDecimal());
				builder.addSlider("buffer", "Path Buffer", buffer);
				builder.addSlider("speed", "Path Speed:", speed);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = settingsDialog.parseColor("color");
							float strokeSize = settingsDialog.parseSlider("strokeSize");
							float textSize = settingsDialog.parseSlider("textSize");
							boolean showDecimal;
							if (isEPSG4326)
								showDecimal = !settingsDialog.parseCheckBox("showDegrees");
							else
								showDecimal = false;
							float buffer = settingsDialog.parseSlider("buffer");
							float speed = settingsDialog.parseSlider("speed");
							
							mapView.setDrawViewColor(color);
							mapView.setDrawViewStrokeStyle(strokeSize);
							mapView.setDrawViewTextSize(textSize);
							mapView.setEditViewTextSize(textSize);
							mapView.setShowDecimal(showDecimal);
							PathFollowerTool.this.buffer = buffer;
							PathFollowerTool.this.speed = speed;
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
				
				settingsDialog = builder.create();
				settingsDialog.show();
			}
				
		});
		return button;
	}

}
