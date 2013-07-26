package au.org.intersect.faims.android.ui.map.tools;

import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.ToolBarButton;
import au.org.intersect.faims.android.util.MeasurementUtil;
import au.org.intersect.faims.android.util.ScaleUtil;
import au.org.intersect.faims.android.util.SpatialiteUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.geometry.VectorElement;

public class FollowTool extends HighlightTool {
	
	private class FollowToolCanvas extends ToolCanvas {
		
		private float distanceTextX;
		private float distanceTextY;
		private float angleTextX;
		private float angleTextY;
		
		private boolean showKm;
		
		private MapPos tp1;
		private MapPos tp2;
		private RectF rectF;
		private Paint targetPaint;
		private MapPos tp3;
		private float radius;
		
		public FollowToolCanvas(Context context) {
			super(context);
			
			targetPaint = new Paint();
		}

		@Override
		public void onDraw(Canvas canvas) {
			if (isDirty) {
				
				canvas.drawLine((float) tp1.x, (float) tp1.y, (float) tp2.x, (float) tp2.y, paint);
				
				if (showKm) {
					canvas.drawText("Distance: " + MeasurementUtil.displayAsKiloMeters(FollowTool.this.distance/1000), distanceTextX, distanceTextY, textPaint);
				} else {
					canvas.drawText("Distance: " + MeasurementUtil.displayAsMeters(FollowTool.this.distance), distanceTextX, distanceTextY, textPaint);
				}
				
				canvas.drawArc(rectF, FollowTool.this.mapView.getRotation()-90, FollowTool.this.angle, true, paint);
				
				canvas.drawText("Bearing: " + MeasurementUtil.displayAsDegrees(FollowTool.this.angle), angleTextX, angleTextY, textPaint);
				
				canvas.drawCircle((float) tp3.x, (float) tp3.y, radius, targetPaint);
			
			}
		}

		public void drawDistanceAndBearing(MapPos currentPoint, MapPos targetPoint) {
			this.tp1 = GeometryUtil.transformVertex(GeometryUtil.convertFromWgs84(currentPoint), FollowTool.this.mapView, true);
			this.tp2 = GeometryUtil.transformVertex(GeometryUtil.convertFromWgs84(targetPoint), FollowTool.this.mapView, true);
			
			float dx = (float) (tp2.x - tp1.x);
			float dy = (float) (tp2.y - tp1.y);
			float d = (float) Math.sqrt(dx * dx + dy * dy) / 2;
			
			this.rectF = new RectF((float) tp1.x - d, (float) tp1.y - d, (float) tp1.x + d, (float) tp1.y + d);
			
			float offset = ScaleUtil.getDip(this.getContext(), DEFAULT_OFFSET);
			
			distanceTextX = (float) tp1.x + offset;
			distanceTextY = (float) tp1.y + offset;
			
			angleTextX = (float) tp1.x + offset;
			angleTextY = (float) tp1.y + 2 * offset;

			Geometry geomToFollow = FollowTool.this.mapView.getGeomToFollow();
			if (geomToFollow instanceof Point) {
				this.tp3 = tp2;
			} else {
				List<MapPos> list = ((Line) geomToFollow).getVertexList();
				this.tp3 = GeometryUtil.transformVertex(GeometryUtil.convertFromWgs84(list.get(list.size()-1)), mapView, true);
			}
			
			this.radius = ScaleUtil.getDip(FollowTool.this.mapView.getContext(), 10);
			
			this.isDirty = true;
			invalidate();
		}
		
		public void setShowKm(boolean value) {
			showKm = value;
		}

		public void setColors(int color, int targetColor) {
			setColor(color);
			
			targetPaint.setColor(targetColor);
			targetPaint.setStyle(Paint.Style.STROKE);
			targetPaint.setStrokeWidth(paint.getStrokeWidth());
			targetPaint.setAntiAlias(true);
			
		}
		
	}
	
	public static final String NAME = "Follow Path";
	
	private FollowToolCanvas canvas;

	protected SettingsDialog settingsDialog;

	protected GeometryStyle bufferStyle;

	protected int targetColor;

	private Polygon buffer;
	
	private float distance;
	
	private float angle;

	private MapPos currentPosition;

	private MapPos targetPoint;
	
	public FollowTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		canvas = new FollowToolCanvas(context);
		container.addView(canvas);
		
		bufferStyle = GeometryStyle.defaultPolygonStyle();
		bufferStyle.polygonColor = 0x00000000;
		bufferStyle.lineColor = Color.GREEN;
		
		targetColor = Color.RED;
	}

	@Override 
	public void activate() {
		super.activate();
		canvas.clear();
		if (!mapView.isProperProjection()) {
			showError("This tool will not function properly as projection is not a projected coordinate system.");
		}
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
		drawDistanceAndBearing();
	}
	
	@Override
	public void onMapUpdate() {
		super.onMapUpdate();
		calculateDistanceAndBearing();
		drawDistanceAndBearing();
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
		mapView.setGeomToFollow(null);
		drawBuffer();
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (element instanceof Geometry) {
			try {
				if (((element instanceof Line) || (element instanceof Point)) && (mapView.getHighlights().size() < 1)) {
					Geometry geom = (Geometry) element;
					
					if (mapView.hasHighlight(geom)) {
						mapView.removeHighlight(geom);
					} else {
						mapView.addHighlight(geom);
					}
					
					mapView.setGeomToFollow(GeometryUtil.convertGeometryToWgs84(geom));
					
					calculateDistanceAndBearing();
					drawDistanceAndBearing();
					drawBuffer();
				}
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}
	
	private void drawBuffer() {
		try {
			if (buffer != null) {
				List<Geometry> geomList = mapView.getHighlights();
				mapView.clearGeometry(buffer);
				mapView.setHighlights(geomList);
				buffer = null;
			}
			
			Geometry geomBuffer = mapView.getGeomToFollowBuffer();
			if (geomBuffer instanceof Polygon) {
				buffer = mapView.drawPolygon(mapView.getVertexLayerId(), ((Polygon) geomBuffer).getVertexList(), bufferStyle);
			}
		} catch (Exception e) {
			FLog.e("error updating buffer", e);
		}
	}
	
	private void calculateDistanceAndBearing() {
		try {
			if (mapView.getGeomToFollow() == null) return;
			
			currentPosition = mapView.getCurrentPosition();
			if (currentPosition == null) return;

			targetPoint = mapView.nextPointToFollow(currentPosition, mapView.getPathBuffer());
			
			distance = (float) SpatialiteUtil.distanceBetween(currentPosition, targetPoint, mapView.getActivity().getProject().getSrid());
			angle = SpatialiteUtil.computeAzimuth(currentPosition, targetPoint);
		} catch (Exception e) {
			FLog.e("error calculating distance and bearing", e);
			showError("Error calculating distance and bearing");
		}
	}
	
	private void drawDistanceAndBearing() {
		try {
			if (mapView.getGeomToFollow() == null) return;
			
			if (currentPosition == null || targetPoint == null) return;
			
			canvas.setColors(mapView.getDrawViewColor(), targetColor);
			canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
			canvas.setTextSize(mapView.getDrawViewTextSize());
			canvas.setShowKm(mapView.showKm());
			canvas.drawDistanceAndBearing(currentPosition, targetPoint);
		} catch (Exception e) {
			FLog.e("error drawing distance and bearing");
		}
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
				final boolean isEPSG4326 = GeometryUtil.EPSG4326.equals(mapView.getActivity().getProject().getSrid());
				if (isEPSG4326)
					builder.addCheckBox("showDegrees", "Show Degrees:", !mapView.showDecimal());
				builder.addCheckBox("showKm", "Show Km:", mapView.showKm());
				builder.addTextField("buffer", "Buffer Size (m):", Float.toString(mapView.getPathBuffer()));
				builder.addTextField("bufferColor", "Buffer Color:", Integer.toHexString(bufferStyle.lineColor));
				builder.addTextField("targetColor", "Target Color:", Integer.toHexString(targetColor));
				
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
							boolean showKm = settingsDialog.parseCheckBox("showKm");
							float buffer = Float.parseFloat(((EditText)settingsDialog.getField("buffer")).getText().toString());
							int bufferColor = settingsDialog.parseColor("bufferColor");
							int targetColor = settingsDialog.parseColor("targetColor");
							
							mapView.setDrawViewColor(color);
							mapView.setDrawViewStrokeStyle(strokeSize);
							mapView.setDrawViewTextSize(textSize);
							mapView.setEditViewTextSize(textSize);
							mapView.setShowDecimal(showDecimal);
							mapView.setShowKm(showKm);
							mapView.setPathBuffer(buffer);
							
							FollowTool.this.bufferStyle.lineColor = bufferColor;
							FollowTool.this.targetColor = targetColor;
							
							FollowTool.this.drawDistanceAndBearing();
							FollowTool.this.drawBuffer();
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
	
	public ToolBarButton getButton(Context context) {
		ToolBarButton button = new ToolBarButton(context);
		button.setLabel("Tracker");
		button.setSelectedState(R.drawable.tools_select_s);
		button.setNormalState(R.drawable.tools_select);
		return button;
	}

}
