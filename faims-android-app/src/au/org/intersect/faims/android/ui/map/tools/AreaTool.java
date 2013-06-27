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
import com.nutiteq.geometry.Polygon;
import com.nutiteq.geometry.VectorElement;

public class AreaTool extends HighlightTool {
	
	private class AreaToolCanvas extends ToolCanvas {
		
		private float textX;
		private float textY;
		private boolean showKm;
		
		public AreaToolCanvas(Context context) {
			super(context);
		}

		@Override
		public void onDraw(Canvas canvas) {
			if (isDirty) {
				
				if (showKm) {
					canvas.drawText(MeasurementUtil.displayAsKiloMeters(AreaTool.this.area/(1000*1000)) + "\u00B2", textX, textY, textPaint);
				} else {
					canvas.drawText(MeasurementUtil.displayAsMeters(AreaTool.this.area) + "\u00B2", textX, textY, textPaint);
				}
				
			}
		}
		
		public void drawArea(Polygon polygon) {
			float offset = ScaleUtil.getDip(this.getContext(), DEFAULT_OFFSET);
			
			MapPos p = GeometryUtil.transformVertex(polygon.getVertexList().get(polygon.getVertexList().size()-1), AreaTool.this.mapView, true);
			
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
	
	public static final String NAME = "Area";
	
	private AreaToolCanvas canvas;
	
	private SettingsDialog settingsDialog;
	
	private float area;

	public AreaTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		canvas = new AreaToolCanvas(context);
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
		drawArea();
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
				if ((element instanceof Polygon) && (mapView.getHighlights().size() < 1)) {
					Polygon p = (Polygon) element;
					
					if (mapView.hasHighlight(p)) {
						mapView.removeHighlight(p);
					} else {
						mapView.addHighlight(p);
					}
					
					calculateArea();
					drawArea();
				}
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}
	
	private void calculateArea() {
		try {
			if (mapView.getHighlights().size() < 1) return;
			Polygon polygon = (Polygon) mapView.getHighlights().get(0);
			area = (float) SpatialiteUtil.computePolygonArea((Polygon) GeometryUtil.convertGeometryToWgs84(polygon), mapView.getActivity().getProject().getSrid());
		} catch (Exception e) {
			FLog.e("error computing area of polygon", e);
			showError("Error computing area of polygon");
		}
	}
	
	private void drawArea() {
		try {
			if (mapView.getHighlights().size() < 1) return;
			
			Polygon polygon = (Polygon) mapView.getHighlights().get(0);
			if (polygon == null) return;
			
			canvas.setColor(mapView.getDrawViewColor());
			canvas.setStrokeSize(mapView.getDrawViewStrokeStyle());
			canvas.setTextSize(mapView.getDrawViewTextSize());
			canvas.setShowKm(mapView.showKm());
			canvas.drawArea(polygon);
		} catch (Exception e) {
			FLog.e("error drawing area", e);
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
							
							AreaTool.this.drawArea();
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
