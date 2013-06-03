package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import au.org.intersect.faims.android.nutiteq.GeometryData;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.GeometrySelection;

import com.nutiteq.geometry.VectorElement;

public class TouchSelectionTool extends SelectionTool {

	private static final String NAME = "Touch Selection";

	public TouchSelectionTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {		
		GeometrySelection selection = mapView.getSelectedSelection();
		if (selection == null) {
			showError("No selection selected");
			return;
		}
		GeometryData geomData = (GeometryData) element.userData;
		if (geomData == null) return;
		if (geomData.id == null) return;
		
		String data = geomData.id;
		if (selection.hasData(data)) {
			selection.removeData(data);
		} else {
			selection.addData(data);
		}
		
		mapView.updateSelections();
	}

}
