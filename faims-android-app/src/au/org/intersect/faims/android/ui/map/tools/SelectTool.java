package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

public class SelectTool extends MapTool {
	
	public static final String NAME = "Select";
	private LinearLayout layout;
	private MapButton clearButton;
	private MapButton undoButton;
	
	public SelectTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		clearButton = createClearButton(context);
		
		undoButton = createUndoButton(context);
		
		layout.addView(clearButton);
		layout.addView(undoButton);
	}

	@Override
	public View getUI() {
		return layout;
	}
	
	@Override
	public void activate() {
		mapView.clearSelection();
	}
	
	@Override
	public void deactivate() {
		mapView.clearSelection();
	}
	
	@Override
	public void update() {
		mapView.updateSelection();
	}
	
	private MapButton createClearButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Clear");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clearSelection();
			}
			
		});
		return button;
	}
	
	private MapButton createUndoButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Undo");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				undoLastSelection();
			}
			
		});
		return button;
	}
	
	private void undoLastSelection() {
		mapView.popSelection();
	}
	
	private void clearSelection() {
		mapView.clearSelection();
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (element instanceof Geometry) {
			mapView.pushSelection((Geometry) element);
		} else {
			// ignore
		}
	}
}
