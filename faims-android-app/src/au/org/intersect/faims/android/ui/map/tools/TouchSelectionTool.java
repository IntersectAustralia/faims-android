package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import au.org.intersect.faims.android.nutiteq.CustomLine;
import au.org.intersect.faims.android.nutiteq.CustomPoint;
import au.org.intersect.faims.android.nutiteq.CustomPolygon;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.GeometrySelection;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

public class TouchSelectionTool extends SelectionTool {

	private static final String NAME = "Touch Selection";

	public TouchSelectionTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (!(element instanceof Geometry) ||
				(element instanceof CustomPoint) || 
				(element instanceof CustomLine) ||
				(element instanceof CustomPolygon)) {
			return;
		}
		
		GeometrySelection selection = mapView.getSelectedSelection();
		if (selection == null) {
			showError("No selection selected");
			return;
		}
		String data = ((String[]) element.userData)[0];
		if (selection.hasData(data)) {
			selection.removeData(data);
		} else {
			selection.addData(data);
		}
		
		mapView.updateSelections();
	}

}
