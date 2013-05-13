package au.org.intersect.faims.android.ui.map.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.form.MapToggleButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.geometry.VectorElement;

public class EditTool extends SelectTool {
	
	public static final String NAME = "Edit";
	
	private MapToggleButton lockButton;
	
	public EditTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		lockButton = createLockButton(context);
		updateLockButton();
		
		updateLayout();
	}
	
	@Override
	public void activate() {
		clearLock();
		super.activate();
	}
	
	@Override
	public void deactivate() {
		clearLock();
		super.activate();
	}
	
	@Override
	public void update() {
		clearLock();
		super.update();
	}
	
	@Override
	protected void updateLayout() {
		super.updateLayout();
		if (lockButton != null) layout.addView(lockButton);
	}
	
	private MapToggleButton createLockButton(final Context context) {
		MapToggleButton button = new MapToggleButton(context);
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
		try {
			if (lockButton.isChecked()) {
				mapView.prepareSelectionTransform();
			} else {
				mapView.doSelectionTransform();
			}
		} catch (Exception e) {
			FLog.e("error doing selection transform", e);
			showError(e.getMessage());
		}
	}
	
	private void clearLock() {
		lockButton.setChecked(false);
		updateLockButton();
		mapView.clearSelectionTransform();
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (!mapView.hasTransformGeometry()) {
			super.onVectorElementClicked(element, arg1, arg2, arg3);
		}
	}
	
	protected void clearSelection() {
		clearLock();
		super.clearSelection();
	}
	
	@Override
	protected MapButton createSettingsButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Style Tool");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Style Settings");
				
				LinearLayout layout = new LinearLayout(context);
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				
				final EditText colorSetter = addSetter(context, layout, "Select Color:", Integer.toHexString(mapView.getDrawViewColor()));
				final EditText editColorSetter = addSetter(context, layout, "Edit Color:", Integer.toHexString(mapView.getEditViewColor()));
				final SeekBar strokeSizeBar = addSlider(context, layout, "Stroke Size:", mapView.getDrawViewStrokeStyle());
				final SeekBar textSizeBar = addSlider(context, layout, "Text Size:", mapView.getDrawViewTextSize());
				
				builder.setView(layout);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = parseColor(colorSetter.getText().toString());
							int editColor = parseColor(editColorSetter.getText().toString());
							float strokeSize = parseSize(strokeSizeBar.getProgress());
							float textSize = parseSize(textSizeBar.getProgress());
							
							mapView.setDrawViewColor(color);
							mapView.setEditViewColor(editColor);
							mapView.setDrawViewStrokeStyle(strokeSize);
							mapView.setDrawViewTextSize(textSize);
							mapView.setEditViewTextSize(textSize);
						} catch (Exception e) {
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
				
				builder.create().show();
			}
				
		});
		return button;
	}
}
