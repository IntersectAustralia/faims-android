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

import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

public class SelectTool extends SettingsTool {
	
	public static final String NAME = "Select";
	
	protected MapButton clearButton;

	private MapToggleButton detailButton;
	
	public SelectTool(Context context, CustomMapView mapView) {
		this(context, mapView, NAME);
	}
	
	public SelectTool(Context context, CustomMapView mapView, String name) {
		super(context, mapView, name);
		
		detailButton = createDetailButton(context);
		clearButton = createClearButton(context);
		
		updateLayout();
	}
	
	@Override
	protected void updateLayout() {
		super.updateLayout();
		if (detailButton != null) layout.addView(detailButton);
		if (clearButton != null) layout.addView(clearButton);
	}
	
	@Override
	public void activate() {
		detailButton.setChecked(false);
		updateDetailButton();
		mapView.setDrawViewDetail(false);
		mapView.setEditViewDetail(false);
		clearSelection();
	}
	
	@Override
	public void deactivate() {
		detailButton.setChecked(false);
		updateDetailButton();
		mapView.setDrawViewDetail(false);
		mapView.setEditViewDetail(false);
		clearSelection();
	}
	
	@Override
	public void update() {
		try {
			mapView.updateSelection();
		} catch (Exception e) {
			FLog.e("error updating selection", e);
			showError(e.getMessage());
		}
	}
	
	private MapToggleButton createDetailButton(final Context context) {
		MapToggleButton button = new MapToggleButton(context);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateDetailButton();
				mapView.setDrawViewDetail(detailButton.isChecked());
				mapView.setEditViewDetail(detailButton.isChecked());
			}
			
		});
		return button;
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
	
	protected void clearSelection() {
		try {
			mapView.clearSelection();
		} catch (Exception e) {
			FLog.e("error clearing selection", e);
			showError(e.getMessage());
		}
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		if (element instanceof Geometry) {
			try {
				Geometry geom = (Geometry) element;
				
				if (mapView.hasSelection(geom)) {
					mapView.removeSelection(geom);
				} else {
					mapView.addSelection(geom);
				}
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}
	
	private void updateDetailButton() {
		detailButton.setText(detailButton.isChecked() ? "Hide Details" : "Show Details");
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
				final SeekBar strokeSizeBar = addSlider(context, layout, "Stroke Size:", mapView.getDrawViewStrokeStyle());
				final SeekBar textSizeBar = addSlider(context, layout, "Text Size:", mapView.getDrawViewTextSize());
				
				builder.setView(layout);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = parseColor(colorSetter.getText().toString());
							float strokeSize = parseSize(strokeSizeBar.getProgress());
							float textSize = parseSize(textSizeBar.getProgress());
							
							mapView.setDrawViewColor(color);
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
