package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.form.MapToggleButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

public class EditTool extends SelectTool {
	
	public static final String NAME = "Edit";
	
	private MapToggleButton lockButton;
	
	public EditTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		lockButton = createLockButton(context);
		
		updateLayout();
	}
	
	@Override
	protected void updateLayout() {
		super.updateLayout();
		if (lockButton != null) layout.addView(lockButton);
	}
	
	private MapToggleButton createLockButton(final Context context) {
		MapToggleButton button = new MapToggleButton(context);
		lockButton.setChecked(mapView.isDrawViewLocked());
		updateLockButton();
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateLock();
			}
			
		});
		return button;
	}
	
	private void updateLockButton() {
		lockButton.setText(lockButton.isChecked() ? "UnLock" : "Lock");
	}
	
	private void updateLock() {
		updateLockButton();
		mapView.setDrawViewLock(lockButton.isChecked());
		
		try {
			if (!lockButton.isChecked()) {
				
			}
		} catch (Exception e) {
			FLog.e("error replacing geometry overlay", e);
			showError(e.getMessage());
		}
	}
	
	public void onMapClicked(double arg0, double arg1, boolean arg2) {
		mapView.clearSelection();
	}

	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (element instanceof Geometry) {
			Geometry geom = (Geometry) element;
			
			try {
				
			} catch (Exception e) {
				FLog.e("error drawing geometry overlay", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}
	
}
