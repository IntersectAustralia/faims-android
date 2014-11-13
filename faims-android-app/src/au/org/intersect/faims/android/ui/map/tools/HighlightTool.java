package au.org.intersect.faims.android.ui.map.tools;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import au.org.intersect.faims.android.two.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.dialog.SettingsDialog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.button.ClearButton;
import au.org.intersect.faims.android.ui.map.button.SettingsButton;
import au.org.intersect.faims.android.ui.map.button.ShowDetailsToggleButton;
import au.org.intersect.faims.android.ui.map.button.ToolBarButton;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;

public class HighlightTool extends SettingsTool {
	
	public static final String NAME = "Highlight";
	
	protected ClearButton clearButton;

	private ShowDetailsToggleButton detailButton;

	protected SettingsDialog settingsDialog;
	
	public HighlightTool(Context context, CustomMapView mapView) {
		this(context, mapView, NAME);
	}
	
	public HighlightTool(Context context, CustomMapView mapView, String name) {
		super(context, mapView, name);
		
		detailButton = createDetailButton(context);
		RelativeLayout.LayoutParams detailParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		detailParams.alignWithParent = true;
		detailParams.addRule(RelativeLayout.ALIGN_RIGHT);
		detailParams.topMargin = (int) ScaleUtil.getDip(context, TOP_MARGIN); 
		detailButton.setLayoutParams(detailParams);
		clearButton = createClearButton(context);
		RelativeLayout.LayoutParams clearParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		clearParams.alignWithParent = true;
		clearParams.addRule(RelativeLayout.ALIGN_RIGHT);
		clearParams.addRule(RelativeLayout.ALIGN_BOTTOM);
		clearParams.bottomMargin = (int) ScaleUtil.getDip(context, BOTTOM_MARGIN); 
		clearButton.setLayoutParams(clearParams);
		
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
		mapView.setDrawViewDetail(false);
		mapView.setEditViewDetail(false);
		clearSelection();
	}
	
	@Override
	public void deactivate() {
		detailButton.setChecked(false);
		mapView.setDrawViewDetail(false);
		mapView.setEditViewDetail(false);
		clearSelection();
	}
	
	@Override
	public void onLayersChanged() {
		try {
			mapView.updateHighlights();
		} catch (Exception e) {
			FLog.e("error updating selection", e);
			showError(e.getMessage());
		}
	}
	
	private ShowDetailsToggleButton createDetailButton(final Context context) {
		final ShowDetailsToggleButton button = new ShowDetailsToggleButton(context);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mapView.setDrawViewDetail(button.isChecked());
				mapView.setEditViewDetail(button.isChecked());
			}
			
		});
		return button;
	}
	
	private ClearButton createClearButton(final Context context) {
		ClearButton button = new ClearButton(context);
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
			mapView.clearHighlights();
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
				
				if (mapView.hasHighlight(geom)) {
					mapView.removeHighlight(geom);
				} else {
					mapView.addHighlight(geom);
				}
			} catch (Exception e) {
				FLog.e("error selecting element", e);
				showError(e.getMessage());
			}
		} else {
			// ignore
		}
	}
	
	@Override
	protected SettingsButton createSettingsButton(Context context) {
		return null;
	}
	
	public ToolBarButton getButton(Context context) {
		ToolBarButton button = new ToolBarButton(context);
		button.setLabel("Select");
		button.setSelectedState(R.drawable.tools_select_s);
		button.setNormalState(R.drawable.tools_select);
		return button;
	}
	
}
