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
import au.org.intersect.faims.android.ui.form.MapButton;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.geometry.VectorElement;

public class SelectTool extends SettingsTool {
	
	public static final String NAME = "Select";
	
	private int color = 0xFF00FFFF;
	private float size = 0.3f;
	private float pickingSize = 0.3f;
	private float width = 0.1f;
	private float pickingWidth = 0.1f;
	
	public SelectTool(Context context, CustomMapView mapView, String name) {
		super(context, mapView, name);
	}
	
	@Override
	public void onVectorElementClicked(VectorElement element, double arg1,
			double arg2, boolean arg3) {
		
		
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
				
				final EditText colorSetter = addSetter(context, layout, "Color:", Integer.toHexString(color));
				final SeekBar sizeBar = addSlider(context, layout, "Size:", getSize());
				final SeekBar pickingSizeBar = addSlider(context, layout, "Picking Size:", getPickingSize());
				final SeekBar widthBar = addSlider(context, layout, "Width:", width);
				final SeekBar pickingWidthBar = addSlider(context, layout, "Picking Width:", pickingWidth);
				
				builder.setView(layout);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = parseColor(colorSetter.getText().toString());
							float size = parseSize(sizeBar.getProgress());
							float pickingSize = parseSize(pickingSizeBar.getProgress());
							float width = parseSize(widthBar.getProgress());
							float pickingWidth = parseSize(pickingWidthBar.getProgress());
							
							SelectTool.this.color = color;
							SelectTool.this.setSize(size);
							SelectTool.this.setPickingSize(pickingSize);
							SelectTool.this.width = width;
							SelectTool.this.pickingWidth = pickingWidth;
						} catch (Exception e) {
							showError(context, e.getMessage());
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
	
	public float getWidth() {
		return width;
	}

	public void setWidth(float width) {
		this.width = width;
	}

	public float getPickingWidth() {
		return pickingWidth;
	}

	public void setPickingWidth(float pickingWidth) {
		this.pickingWidth = pickingWidth;
	}

}
