package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.dialog.ErrorDialog;
import au.org.intersect.faims.android.ui.map.button.RestrictedButton;
import au.org.intersect.faims.android.util.ScaleUtil;

public class SelectionListItem extends LinearLayout {

	private TextView text;
	private GeometrySelection selection;
	private RestrictedButton restrictedButton;
	private boolean selected;

	public SelectionListItem(Context context) {
		super(context);
		setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
		setOrientation(LinearLayout.HORIZONTAL);
		int size = (int) ScaleUtil.getDip(context, 10);
		setPadding(size, size, size, size);
	}
	
	public void init(final ListView listView, final GeometrySelection set, final CustomMapView mapView) {
		selection = set;
		text = new TextView(this.getContext());
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		text.setLayoutParams(params);
		int size = (int) ScaleUtil.getDip(this.getContext(), 5);
		text.setPadding(size, size, size, size);
		text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
		text.setSingleLine(false);
		text.setText(set.getName());
		
		restrictedButton = new RestrictedButton(this.getContext());
		LinearLayout.LayoutParams restrictedParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		restrictedButton.setLayoutParams(restrictedParams);
		restrictedButton.setChecked(mapView.getRestrictedSelections() != null && mapView.getRestrictedSelections().contains(set));
		restrictedButton.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent action) {
				if(action.getAction() == MotionEvent.ACTION_UP){
					try {
						if(!selected){
							mapView.addRestrictedSelection(set, !restrictedButton.isChecked());
							restrictedButton.setChecked(!restrictedButton.isChecked());
							mapView.updateSelections();
						}
						mapView.updateLayers();
					} catch (Exception e) {
						FLog.e("error setting layer visibility", e);
						showErrorDialog("Error setting layer visibility");
					}
					((BaseAdapter)listView.getAdapter()).notifyDataSetChanged();
					v.setFocusable(false);
				}
				return true;
			}
		});
		
		addView(text);
		addView(restrictedButton);
	}
	
	public GeometrySelection getSelection() {
		return selection;
	}
	
	public boolean isRestricted(){
		return restrictedButton.isChecked();
	}
	
	private void showErrorDialog(String message) {
		new ErrorDialog(this.getContext(), "Layer Manager Error", message).show();
	}
	
	public void setItemSelected(boolean selected){
		this.selected = selected;
		setBackgroundResource(selected? android.R.color.holo_blue_dark : 0);
	}
}
