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

import com.nutiteq.geometry.VectorElement;
import com.nutiteq.projections.EPSG3857;

public class CreatePointTool extends BaseGeometryTool {
	
	public static final String NAME = "Create Point";
	
	private PointStyleDialog styleDialog;
	
	public CreatePointTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
	}

	@Override
	protected MapButton createSettingsButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Style Tool");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				PointStyleDialog.Builder builder = new PointStyleDialog.Builder(context);
				styleDialog = (PointStyleDialog) builder.create();
				styleDialog.show();
			}
				
		});
		
		return button;
	}
	
	@Override
	public void onMapClicked(double x, double y, boolean z) {
		CanvasLayer layer = (CanvasLayer) mapView.getSelectedLayer();
		if (layer == null) {
			setSelectedLayer(null);
			showError("No layer selected");
			return;
		}
		
		try {
			mapView.drawPoint(layer, (new EPSG3857()).toWgs84(x, y), createPointStyle());
		} catch (Exception e) {
			FLog.e("error drawing point", e);
			showError(e.getMessage());
		}
	}

	private GeometryStyle createPointStyle() {
		return styleDialog.getStyle().cloneStyle();
	}

	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
	}
	
}
