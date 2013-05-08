package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.CustomGdalMapLayer;
import au.org.intersect.faims.android.nutiteq.CustomOgrLayer;
import au.org.intersect.faims.android.nutiteq.CustomSpatialiteLayer;
import au.org.intersect.faims.android.ui.form.CustomToggleButton;
import au.org.intersect.faims.android.util.Dip;

import com.nutiteq.layers.Layer;

public class LayerListItem extends LinearLayout {

	private TextView text;
	private Button infoButton;
	private CustomToggleButton showButton;
	private boolean selected;

	public LayerListItem(Context context) {
		super(context);
		setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
		setOrientation(LinearLayout.HORIZONTAL);
		int size = Dip.getDip(context, 10);
		setPadding(size, size, size, size);
	}
	
	public void init(final Layer layer) {
		text = new TextView(this.getContext());
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		params.leftMargin = Dip.getDip(this.getContext(), 5);
		text.setLayoutParams(params);
		text.setTextSize(18f);
		String layerName = "N/A";
		if (layer instanceof CustomGdalMapLayer) {
			layerName = ((CustomGdalMapLayer) layer).getName();
		} else if (layer instanceof CustomOgrLayer) {
			layerName = ((CustomOgrLayer) layer).getName();
		} else if (layer instanceof CustomSpatialiteLayer) {
			layerName = ((CustomSpatialiteLayer) layer).getName();
		} else if (layer instanceof CanvasLayer) {
			layerName = ((CanvasLayer) layer).getName();
		}
		
		text.setText(layerName);
		
		infoButton = new Button(this.getContext());
		infoButton.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		infoButton.setText("I");
		
		showButton = new CustomToggleButton(this.getContext());
		showButton.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		showButton.setToggle(layer.isVisible());
		showButton.setText("S");
		showButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				showButton.toggle();
				layer.setVisible(showButton.isToggle());
			}
			
		});
		
		addView(text);
		addView(infoButton);
		addView(showButton);
	}
	
	public void toggle() {
		selected = !selected;
		updateItem();
	}
	
	public void setSelected(boolean value) {
		selected = value;
		updateItem();
	}
	
	private void updateItem() {
		if (selected) {
			this.setBackgroundColor(Color.BLUE);
		} else {
			this.setBackgroundColor(Color.WHITE);
		}
	}
}
