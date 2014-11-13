package au.org.intersect.faims.android.ui.map.tools;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.ui.dialog.PointStyleDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.button.PlotGPSButton;
import au.org.intersect.faims.android.ui.map.button.SettingsButton;
import au.org.intersect.faims.android.ui.map.button.ToolBarButton;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.projections.EPSG3857;

public class CreatePointTool extends SettingsTool {
	
	public static final String NAME = "Create Point";
	
	private GeometryStyle style;
	
	private PointStyleDialog styleDialog;

	private PlotGPSButton plotButton;
	
	public CreatePointTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		style = GeometryStyle.defaultPointStyle();
		
		plotButton = createPlotButton(context);
		RelativeLayout.LayoutParams plotGPSParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		plotGPSParams.alignWithParent = true;
		plotGPSParams.addRule(RelativeLayout.ALIGN_LEFT);
		plotGPSParams.addRule(RelativeLayout.ALIGN_BOTTOM);
		plotGPSParams.bottomMargin = (int) ScaleUtil.getDip(context, BOTTOM_MARGIN);
		plotButton.setLayoutParams(plotGPSParams);
		
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
	protected SettingsButton createSettingsButton(final Context context) {
		SettingsButton button = new SettingsButton(context);
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
	
	private PlotGPSButton createPlotButton(final Context context) {
		PlotGPSButton button = new PlotGPSButton(context);
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
	
	@Override
	public ToolBarButton getButton(Context context) {
		ToolBarButton button = new ToolBarButton(context);
		button.setLabel("Point");
		button.setSelectedState(R.drawable.tools_point_s);
		button.setNormalState(R.drawable.tools_point);
		return button;
	}

	public void setStyle(GeometryStyle style) {
		this.style = style;
	}

	public void saveToJSON(JSONObject json) {
		try {
			json.put("name", name);
			JSONObject styleSettings = new JSONObject();
			style.saveToJSON(styleSettings);
			json.put("style", styleSettings);
		} catch (JSONException e) {
			FLog.e("Couldn't serialize CreatePointTool", e);
		}
	}
	
}
