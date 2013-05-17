package au.org.intersect.faims.android.ui.map.tools;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import au.org.intersect.faims.android.data.GeometryStyle;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CustomLine;
import au.org.intersect.faims.android.nutiteq.CustomPoint;
import au.org.intersect.faims.android.nutiteq.CustomPolygon;
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.form.MapToggleButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

public class EditTool extends SelectTool {
	
	public static final String NAME = "Edit";
	
	private MapToggleButton lockButton;

	private MapButton propertiesButton;

	private MapButton deleteButton;
	
	public EditTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		lockButton = createLockButton(context);
		propertiesButton = createPropertiesButton(context);
		deleteButton = createDeleteButton(context); 
				
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
	public void onLayersChanged() {
		clearLock();
		super.onLayersChanged();
	}
	
	@Override
	protected void updateLayout() {
		super.updateLayout();
		if (lockButton != null) layout.addView(lockButton);
		if (propertiesButton != null) layout.addView(propertiesButton);
		if (deleteButton != null) layout.addView(deleteButton);
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
	
	private MapButton createDeleteButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Delete");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					List<Geometry> selection = EditTool.this.mapView.getSelection();
					EditTool.this.mapView.clearGeometryList(selection);
				} catch (Exception e) {
					showError(e.getMessage());
				}
			}
			
		});
		return button;
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
				
				ScrollView scrollView = new ScrollView(context);
				LinearLayout layout = new LinearLayout(context);
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				scrollView.addView(layout);
				
				final EditText colorSetter = addSetter(context, layout, "Select Color:", Integer.toHexString(mapView.getDrawViewColor()));
				final EditText editColorSetter = addSetter(context, layout, "Edit Color:", Integer.toHexString(mapView.getEditViewColor()));
				final SeekBar strokeSizeBar = addSlider(context, layout, "Stroke Size:", mapView.getDrawViewStrokeStyle());
				final SeekBar textSizeBar = addSlider(context, layout, "Text Size:", mapView.getDrawViewTextSize());
				final CheckBox decimalBox = addCheckBox(context, layout, "Show Degrees:", !mapView.showDecimal());
				
				builder.setView(scrollView);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = parseColor(colorSetter.getText().toString());
							int editColor = parseColor(editColorSetter.getText().toString());
							float strokeSize = parseSize(strokeSizeBar.getProgress());
							float textSize = parseSize(textSizeBar.getProgress());
							boolean showDecimal = !decimalBox.isChecked();
							
							mapView.setDrawViewColor(color);
							mapView.setEditViewColor(editColor);
							mapView.setDrawViewStrokeStyle(strokeSize);
							mapView.setDrawViewTextSize(textSize);
							mapView.setEditViewTextSize(textSize);
							mapView.setShowDecimal(showDecimal);
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
	
	protected MapButton createPropertiesButton(final Context context) {
		MapButton button = new MapButton(context);
		button.setText("Properties");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// get selected geometry
				List<Geometry> selection = mapView.getSelection();
				if (selection.size() != 1) {
					showError("Please select only one geometry to edit");
					return;
				}
				
				Geometry geom = selection.get(0);
				
				if (geom instanceof CustomPoint) {
					showPointProperties((CustomPoint) geom);
				} else if (geom instanceof CustomLine) {
					showLineProperties((CustomLine) geom);
				} else if (geom instanceof CustomPolygon) {
					showPolygonProperties((CustomPolygon) geom);
				}
			}
				
		});
		return button;
	}
	
	private void showPointProperties(final CustomPoint point) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Point Properties");
		
		ScrollView scrollView = new ScrollView(context);
		LinearLayout layout = new LinearLayout(context);
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		final GeometryStyle style = point.getStyle();
		
		final EditText colorSetter = addSetter(context, layout, "Point Color:", Integer.toHexString(style.pointColor));
		final SeekBar sizeBar = addSlider(context, layout, "Point Size:", style.size);
		final SeekBar pickingSizeBar = addSlider(context, layout, "Point Picking Size:", style.pickingSize);
		
		builder.setView(scrollView);
		
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					int color = parseColor(colorSetter.getText().toString());
					float size = parseSize(sizeBar.getProgress());
					float pickingSize = parseSize(pickingSizeBar.getProgress());
					
					style.pointColor = color;
					style.size = size;
					style.pickingSize = pickingSize;
					
					EditTool.this.mapView.restylePoint(point, style);
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
	
	private void showLineProperties(final CustomLine line) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Line Properties");
		
		ScrollView scrollView = new ScrollView(context);
		LinearLayout layout = new LinearLayout(context);
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		final GeometryStyle style = line.getStyle();
		
		final EditText colorSetter = addSetter(context, layout, "Line Color:", Integer.toHexString(style.pointColor));
		final SeekBar sizeBar = addSlider(context, layout, "Point Size:", style.size);
		final SeekBar pickingSizeBar = addSlider(context, layout, "Point Picking Size:", style.pickingSize);
		final SeekBar widthBar = addSlider(context, layout, "Line Width:", style.width);
		final SeekBar pickingWidthBar = addSlider(context, layout, "Line Picking Width:", style.pickingWidth);
		final CheckBox showPointsBox = addCheckBox(context, layout, "Show Points on Line:", style.showPoints);
		
		builder.setView(scrollView);
		
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					int color = parseColor(colorSetter.getText().toString());
					float size = parseSize(sizeBar.getProgress());
					float pickingSize = parseSize(pickingSizeBar.getProgress());
					float width = parseSize(widthBar.getProgress());
					float pickingWidth = parseSize(pickingWidthBar.getProgress());
					boolean showPoints = showPointsBox.isChecked();
					
					style.pointColor = color;
					style.size = size;
					style.pickingSize = pickingSize;
					style.lineColor = color;
					style.width = width;
					style.pickingWidth = pickingWidth;
					style.showPoints = showPoints;
					
					EditTool.this.mapView.restyleLine(line, style);
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
	
	private void showPolygonProperties(final CustomPolygon polygon) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Line Properties");
		
		ScrollView scrollView = new ScrollView(context);
		LinearLayout layout = new LinearLayout(context);
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		final GeometryStyle style = polygon.getStyle();
		
		final EditText colorSetter = addSetter(context, layout, "Polygon Color:", Integer.toHexString(style.polygonColor));
		final SeekBar sizeBar = addSlider(context, layout, "Point Size:", style.size);
		final SeekBar pickingSizeBar = addSlider(context, layout, "Point Picking Size:", style.pickingSize);
		final EditText strokeColorSetter = addSetter(context, layout, "Stroke Color:", Integer.toHexString(style.lineColor));
		final SeekBar widthBar = addSlider(context, layout, "Stroke Width:", style.width);
		final SeekBar pickingWidthBar = addSlider(context, layout, "Stroke Picking Width:", style.pickingWidth);
		final CheckBox showStrokeBox = addCheckBox(context, layout, "Show Stroke on Polygon:", style.showStroke);
		
		builder.setView(scrollView);
		
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				try {
					int color = parseColor(colorSetter.getText().toString());
					float size = parseSize(sizeBar.getProgress());
					float pickingSize = parseSize(pickingSizeBar.getProgress());
					int lineColor = parseColor(strokeColorSetter.getText().toString());
					float width = parseSize(widthBar.getProgress());
					float pickingWidth = parseSize(pickingWidthBar.getProgress());
					boolean showStroke = showStrokeBox.isChecked();
					
					style.polygonColor = color;
					style.size = size;
					style.pickingSize = pickingSize;
					style.lineColor = lineColor;
					style.width = width;
					style.pickingWidth = pickingWidth;
					style.showStroke = showStroke;
					
					EditTool.this.mapView.restylePolygon(polygon, style);
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
}
