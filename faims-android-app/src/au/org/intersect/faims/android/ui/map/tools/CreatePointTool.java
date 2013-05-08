package au.org.intersect.faims.android.ui.map.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import au.org.intersect.faims.android.exceptions.MapException;
import au.org.intersect.faims.android.ui.dialog.ErrorDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.geometry.VectorElement;

public class CreatePointTool extends MapTool {
	
	public static final String NAME = "Create Point";
	
	private int color;
	private float size;
	private float pickingSize;

	public CreatePointTool(CustomMapView mapView) {
		super(mapView, NAME);
	}
	
	@Override
	public View getUI(final Context context) {
		LinearLayout layout = new LinearLayout(context);
		
		Button settingsButton = new Button(context);
		settingsButton.setText("Settings");
		settingsButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Tool Settings");
				
				LinearLayout layout = new LinearLayout(context);
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				
				final EditText colorSetter = addSetter(context, layout, "Color:");
				final SeekBar sizeBar = addSlider(context, layout, "Size:");
				final SeekBar pickingSizeBar = addSlider(context, layout, "Picking Size:");
				
				builder.setView(layout);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = parseColor(colorSetter.getText().toString());
							float size = parseSize(sizeBar.getProgress());
							float pickingSize = parseSize(pickingSizeBar.getProgress());
							
							CreatePointTool.this.color = color;
							CreatePointTool.this.size = size;
							CreatePointTool.this.pickingSize = pickingSize;
						} catch (Exception e) {
							new ErrorDialog(context, "Tool Error", e.getMessage()).show();
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
		
		layout.addView(settingsButton);
		
		return layout;
	}
	
	private int parseColor(String value) throws Exception {
		Integer color = Integer.parseInt(value, 16);
		if (color == null) {
			throw new MapException("Invalid color specified");
		}
		return color;
	}
	
	private float parseSize(int value) throws Exception {
		if (value < 0 || value > 100) {
			throw new MapException("Invalid size");
		}
		
		return value / 100;
	}
	
	private EditText addSetter(Context context, LinearLayout layout, String labelText) {
		return addSetter(context, layout, labelText, -1);
	}
	
	private EditText addSetter(Context context, LinearLayout layout, String labelText, int type) {
		TextView label = new TextView(context);
		label.setText(labelText);
		
		EditText text = new EditText(context);
		if (type >= 0) text.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		
		layout.addView(label);
		layout.addView(text);
		
		return text;
	}
	
	private SeekBar addSlider(Context context, LinearLayout layout, String labelText) {
		TextView label = new TextView(context);
		label.setText(labelText);
		
		SeekBar seekBar = new SeekBar(context);
		seekBar.setMax(100);
		
		layout.addView(label);
		layout.addView(seekBar);
		
		return seekBar;
	}
	
	@Override
	public void onMapClicked(double x, double y, boolean z) {
		
	}

	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public float getSize() {
		return size;
	}

	public void setSize(float size) {
		this.size = size;
	}

	public float getPickingSize() {
		return pickingSize;
	}

	public void setPickingSize(float pickingSize) {
		this.pickingSize = pickingSize;
	}
	
}
