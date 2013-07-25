package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.ui.dialog.PointStyleDialog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.projections.EPSG3857;

public class CreatePointTool extends SettingsTool {
	
	public static final String NAME = "Create Point";
	
	private GeometryStyle style;
	
	private PointStyleDialog styleDialog;

	private MapButton plotButton;
	
	public CreatePointTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		style = GeometryStyle.defaultPointStyle();
		
		plotButton = createPlotButton(context);
		
		updateLayout();
	}
	
	@Override
	protected void updateLayout() {
		if (layout != null) {
			layout.removeAllViews();
			layout.addView(settingsButton);
		}
		
		if (plotButton != null) layout.addView(plotButton);
	}
	
	private void showNotCanvasLayerError() {
		showError("The selected layer is not canvas layer");
	}

	@Override
	protected MapButton createSettingsButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Style Tool");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				PointStyleDialog.Builder builder = new PointStyleDialog.Builder(context, style);
				styleDialog = (PointStyleDialog) builder.create();
				styleDialog.show();
			}
				
		});
		
		return button;
	}
	
	private MapButton createPlotButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Plot GPS");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				MapPos gpsPoint = CreatePointTool.this.mapView.getCurrentPosition();
				if (gpsPoint != null) {
					if(!(mapView.getSelectedLayer() instanceof CanvasLayer)) {
						showNotCanvasLayerError();
						return;
					}
					
					try {
						mapView.notifyGeometryCreated(mapView.drawPoint(mapView.getSelectedLayer(), gpsPoint, createPointStyle()));
					} catch (Exception e) {
						FLog.e("error drawing point", e);
						showError(e.getMessage());
					}
				} else {
					showError("No GPS Signal");
				}
			}
			
		});
		return button;
	}
	
	@Override
	public void onMapClicked(double x, double y, boolean z) {
		if(!(mapView.getSelectedLayer() instanceof CanvasLayer)) {
			showNotCanvasLayerError();
			return;
		}
		
		try {
			mapView.notifyGeometryCreated(mapView.drawPoint(mapView.getSelectedLayer(), (new EPSG3857()).toWgs84(x, y), createPointStyle()));
		} catch (Exception e) {
			FLog.e("error drawing point", e);
			showError(e.getMessage());
		}
	}

	private GeometryStyle createPointStyle() {
		return style.cloneStyle();
	}

	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
	}
	
}
