package au.org.intersect.faims.android.ui.map.tools;

import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.location.Location;
import android.view.View;
import android.view.View.OnClickListener;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.util.MeasurementUtil;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.VectorElement;

public class LineDistanceTool extends HighlightTool {
	
	private class LineDistanceToolCanvas extends ToolCanvas {
		
		private float textX;
		private float textY;
		
		private boolean showKm;
		
		public LineDistanceToolCanvas(Context context) {
			super(context);
		}

		@Override
		public void onDraw(Canvas canvas) {
			if (isDirty) {
				
				if (showKm) {
					canvas.drawText(MeasurementUtil.displayAsKiloMeters(LineDistanceTool.this.distance/1000), textX, textY, textPaint);
				} else {
					canvas.drawText(MeasurementUtil.displayAsMeters(LineDistanceTool.this.distance), textX, textY, textPaint);
				}
				
			}
		}

		public void drawDistance(Line line) {
			this.isDirty = true;
			
			float offset = ScaleUtil.getDip(this.getContext(), DEFAULT_OFFSET);
			
			MapPos p = GeometryUtil.transformVertex(line.getVertexList().get(line.getVertexList().size()-1), LineDistanceTool.this.mapView, true);
			
			textX = (float) p.x + offset;
			textY = (float) p.y + offset;
			
			invalidate();
		}
		
		public void setShowKm(boolean value) {
			showKm = value;
			invalidate();
		}
		
	}
	
	public static final String NAME = "Line Distance";
	
	private LineDistanceToolCanvas canvas;

	private float distance;

	protected SettingsDialog settingsDialog;

	public LineDistanceTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		canvas = new LineDistanceToolCanvas(context);
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
		drawDistance();
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
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (element instanceof Geometry) {
			try {
				if ((element instanceof Line) && (mapView.getHighlights().size() < 1)) {
					Line p = (Line) element;
					
					if (mapView.hasHighlight(p)) {
						mapView.removeHighlight(p);
					} else {
						mapView.addHighlight(p);
					}
					
					computeDistance();
					drawDistance();
				}
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}
	
	private void computeDistance() {
		if (mapView.getHighlights().size() < 1) return;
		
		Line line = (Line) mapView.getHighlights().get(0);
		
		this.distance = computeLineDistance(line.getVertexList());
	}
	
	private void drawDistance() {
		if (mapView.getHighlights().size() < 1) return;
		
		Line line = (Line) mapView.getHighlights().get(0);
		
		canvas.drawDistance(line);
		canvas.setColor(mapView.getDrawViewColor());
		canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
		canvas.setTextSize(mapView.getDrawViewTextSize());
		canvas.setShowKm(mapView.showKm());
	}
	
	public float computeLineDistance(List<MapPos> points) {
		float totalDistance = 0;
		MapPos lp = null;
		for (MapPos p : points) {
			p = GeometryUtil.convertToWgs84(p);
			if (lp != null) {
				float[] results = new float[3];
				Location.distanceBetween(lp.y, lp.x, p.y, p.x, results);
				totalDistance += results[0];
			}
			lp = p;
		}
		return totalDistance;
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
				builder.addCheckBox("showKm", "Show Km:", mapView.showKm());
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = settingsDialog.parseColor("color");
							float strokeSize = settingsDialog.parseSlider("strokeSize");
							float textSize = settingsDialog.parseSlider("textSize");
							boolean showDecimal = !settingsDialog.parseCheckBox("showDegrees");
							boolean showKm = settingsDialog.parseCheckBox("showKm");
							
							mapView.setDrawViewColor(color);
							mapView.setDrawViewStrokeStyle(strokeSize);
							mapView.setDrawViewTextSize(textSize);
							mapView.setEditViewTextSize(textSize);
							mapView.setShowDecimal(showDecimal);
							mapView.setShowKm(showKm);
							
							LineDistanceTool.this.drawDistance();
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
