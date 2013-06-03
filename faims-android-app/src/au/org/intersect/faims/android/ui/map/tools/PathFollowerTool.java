package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.view.View;
import android.view.View.OnClickListener;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;

public class PathFollowerTool extends HighlightTool {
	
	private class PathFollowerToolCanvas extends ToolCanvas {
		
		private MapPos tp;

		public PathFollowerToolCanvas(Context context) {
			super(context);
		}

		@Override
		public void onDraw(Canvas canvas) {
			if (isDirty) {
				
				canvas.drawCircle((float) tp.x, (float) tp.y, 10, paint);
				
			}
		}

		public void drawPointOnPath(MapPos p) {
			this.isDirty = true;
			
			this.tp = GeometryUtil.transformVertex(GeometryUtil.convertFromWgs84(p), PathFollowerTool.this.mapView, true);
			
			invalidate();
		}
		
	}
	
	public static final String NAME = "Follow Path - Debug";
	
	private PathFollowerToolCanvas canvas;

	protected SettingsDialog settingsDialog;

	protected float speed = 0.5f;

	private Thread pathFollowerThread;

	private boolean killFollowerThread;

	private MapPos currentPoint;

	protected float buffer = 0.5f;

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
					
					mapView.setPathToFollow((Line) GeometryUtil.convertGeometryToWgs84(line));
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
	
	private void startPathFollower(Point p) {
		killFollowerThread = false;
		currentPoint = p.getMapPos();
		pathFollowerThread = new Thread(new Runnable() {
			

			@Override
			public void run() {
				while(!killFollowerThread) {
					try {
						MapPos point = PathFollowerTool.this.mapView.nextPointOnPath(currentPoint, buffer * 10);
						FLog.d("Next Point: " + point);
						if (point != null) {
						
							double dx = point.x - currentPoint.x;
							double dy = point.y - currentPoint.y;
							double d = Math.sqrt(dx * dx + dy * dy);
							
							double nextStep = speed * 10;
							currentPoint = new MapPos(currentPoint.x + nextStep * dx / d, currentPoint.y + nextStep * dy / d);
							
							PathFollowerTool.this.updatePath();
						
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
		canvas.setColor(mapView.getDrawViewColor());
		canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
		canvas.setTextSize(mapView.getDrawViewTextSize());
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
				builder.addSlider("buffer", "Path Buffer", buffer);
				builder.addSlider("speed", "Path Speed:", speed);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = settingsDialog.parseColor("color");
							float strokeSize = settingsDialog.parseSlider("strokeSize");
							float textSize = settingsDialog.parseSlider("textSize");
							boolean showDecimal = !settingsDialog.parseCheckBox("showDegrees");
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
