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
			float offset = ScaleUtil.getDip(this.getContext(), DEFAULT_OFFSET);
			
			MapPos p = GeometryUtil.transformVertex(line.getVertexList().get(line.getVertexList().size()-1), LineDistanceTool.this.mapView, true);
			
			textX = (float) p.x + offset;
			textY = (float) p.y + offset;
			
			this.isDirty = true;
			invalidate();
		}
		
		public void setShowKm(boolean value) {
			showKm = value;
			invalidate();
		}
		
	}
	
	public static final String NAME = "Line Distance";
	
	private LineDistanceToolCanvas canvas;

	protected SettingsDialog settingsDialog;
	
	private float distance;

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
			if (mapView.getHighlights().size() < 1) return;
			
			Line line = (Line) mapView.getHighlights().get(0);
			this.distance = SpatialiteUtil.computeLineDistance(GeometryUtil.convertToWgs84(line.getVertexList()), mapView.getActivity().getProject().getSrid());
			
		} catch (Exception e) {
			FLog.e("error calculating line distance", e);
			showError("Error calculating line distance");
		}
	}
	
	private void drawDistance() {
		try {
			if (mapView.getHighlights().size() < 1) return;
			
			Line line = (Line) mapView.getHighlights().get(0);
			
			canvas.setColor(mapView.getDrawViewColor());
			canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
			canvas.setTextSize(mapView.getDrawViewTextSize());
			canvas.setShowKm(mapView.showKm());
			canvas.drawDistance(line);
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
							
							LineDistanceTool.this.drawDistance();
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
