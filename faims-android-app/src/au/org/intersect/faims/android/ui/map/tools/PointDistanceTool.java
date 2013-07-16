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
import au.org.intersect.faims.android.util.MeasurementUtil;
import au.org.intersect.faims.android.util.ScaleUtil;
import au.org.intersect.faims.android.util.SpatialiteUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.VectorElement;

public class PointDistanceTool extends HighlightTool {
	
	private class PointDistanceToolCanvas extends ToolCanvas {
		
		private float textX;
		private float textY;
		private MapPos tp1;
		private MapPos tp2;
		private boolean showKm;
		
		public PointDistanceToolCanvas(Context context) {
			super(context);
		}

		@Override
		public void onDraw(Canvas canvas) {
			if (isDirty) {
				canvas.drawLine((float) tp1.x, (float) tp1.y, (float) tp2.x, (float) tp2.y, paint);
				
				if (showKm) {
					canvas.drawText(MeasurementUtil.displayAsKiloMeters(PointDistanceTool.this.distance/1000), textX, textY, textPaint);
				} else {
					canvas.drawText(MeasurementUtil.displayAsMeters(PointDistanceTool.this.distance), textX, textY, textPaint);
				}
				
			}
		}

		public void drawDistanceBetween(MapPos p1, MapPos p2) {
			this.tp1 = GeometryUtil.transformVertex(p1, PointDistanceTool.this.mapView, true);
			this.tp2 = GeometryUtil.transformVertex(p2, PointDistanceTool.this.mapView, true);
			
			float midX = (float) (tp1.x + tp2.x) / 2;
			float midY = (float) (tp1.y + tp2.y) / 2;
			
			float offset = ScaleUtil.getDip(this.getContext(), DEFAULT_OFFSET);
			
			textX = midX + offset;
			
			if (((tp1.x < tp2.x) && (tp1.y > tp2.y)) || ((tp1.x > tp2.x) && (tp1.y < tp2.y))){
				textY = midY + offset;
			} else {
				textY = midY - offset;
			}
			
			this.isDirty = true;
			invalidate();
		}
		
		public void setShowKm(boolean value) {
			showKm = value;
		}
		
	}
	
	public static final String NAME = "Point Distance";
	
	private PointDistanceToolCanvas canvas;

	protected SettingsDialog settingsDialog;
	
	private float distance;

	public PointDistanceTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		canvas = new PointDistanceToolCanvas(context);
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
				if ((element instanceof Point) && (mapView.getHighlights().size() < 2)) {
					Point p = (Point) element;
					
					if (mapView.hasHighlight(p)) {
						mapView.removeHighlight(p);
					} else {
						mapView.addHighlight(p);
					}
					
					calculateDistance();
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
	
	private void calculateDistance() {
		try {
			if (mapView.getHighlights().size() < 2) return;
			
			MapPos p1 = ((Point) mapView.getHighlights().get(0)).getMapPos();
			MapPos p2 = ((Point) mapView.getHighlights().get(1)).getMapPos();
			
			this.distance = (float) SpatialiteUtil.computePointDistance(GeometryUtil.convertToWgs84(p1), GeometryUtil.convertToWgs84(p2), mapView.getActivity().getProject().getSrid());
			
		} catch (Exception e) {
			FLog.e("error calculating point distance", e);
			showError("Error calculating point distance");
		}
	}
	
	private void drawDistance() {
		try {
			if (mapView.getHighlights().size() < 2) return;
			
			MapPos p1 = ((Point) mapView.getHighlights().get(0)).getMapPos();
			MapPos p2 = ((Point) mapView.getHighlights().get(1)).getMapPos();
			
			if (p1 == null || p2 == null) return;
			
			canvas.setColor(mapView.getDrawViewColor());
			canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
			canvas.setTextSize(mapView.getDrawViewTextSize());
			canvas.setShowKm(mapView.showKm());
			canvas.drawDistanceBetween(p1, p2);
		} catch (Exception e) {
			FLog.e("error drawing line distance", e);
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
							
							mapView.setDrawViewColor(color);
							mapView.setDrawViewStrokeStyle(strokeSize);
							mapView.setDrawViewTextSize(textSize);
							mapView.setEditViewTextSize(textSize);
							mapView.setShowDecimal(showDecimal);
							mapView.setShowKm(showKm);
							
							PointDistanceTool.this.drawDistance();
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
