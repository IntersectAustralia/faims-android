package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.dialog.ErrorDialog;
import au.org.intersect.faims.android.ui.map.button.EyeButton;
import au.org.intersect.faims.android.util.ScaleUtil;

public class SelectionListItem extends LinearLayout {

	private TextView text;
	private GeometrySelection selection;
	private ImageView restrictedView;
	private EyeButton visibleButton;
	private boolean selected;
	private boolean restricted;

	public SelectionListItem(Context context) {
		super(context);
		setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
		setOrientation(LinearLayout.HORIZONTAL);
		setGravity(Gravity.CENTER_VERTICAL);
		int size = (int) ScaleUtil.getDip(context, 10);
		setPadding(size, size, size, size);
	}
	
	public void init(final ListView listView, final GeometrySelection set, final CustomMapView mapView) {
		selection = set;
		restricted = mapView.getRestrictedSelections() != null && mapView.getRestrictedSelections().contains(set);

		text = new TextView(this.getContext());
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		text.setLayoutParams(params);
		int size = (int) ScaleUtil.getDip(this.getContext(), 5);
		text.setPadding(size, size, size, size);
		text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
		text.setSingleLine(false);
		text.setText(set.getName());
		
		restrictedView = new ImageView(this.getContext());
		LinearLayout.LayoutParams restrictedParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		restrictedView.setLayoutParams(restrictedParams);
		restrictedView.setBackgroundResource(restricted ? R.drawable.restricted_button : 0);

		visibleButton = new EyeButton(this.getContext());
		visibleButton.setChecked(set.isActive());
		visibleButton.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent action) {
				if(action.getAction() == MotionEvent.ACTION_UP){
					try {
						mapView.setSelectionActive(set, !visibleButton.isChecked());
						visibleButton.setChecked(!visibleButton.isChecked());
					} catch (Exception e) {
						FLog.e("error setting selection visibility", e);
						showErrorDialog("Error setting selection visibility");
					}
					((BaseAdapter)listView.getAdapter()).notifyDataSetChanged();
					v.setFocusable(false);
				}
				return true;
			}
		});

		addView(text);
		addView(restrictedView);
		addView(visibleButton);
	}
	
	public GeometrySelection getSelection() {
		return selection;
	}
	
	public boolean isRestricted(){
		return restricted;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setRestricted(boolean restricted) {
		this.restricted = restricted;
		restrictedView.setBackgroundResource(restricted ? R.drawable.restricted_button : 0);
	}
	
	public void setItemSelected(boolean selected){
		this.selected = selected;
		setBackgroundResource(selected? android.R.color.holo_blue_dark : 0);
	}
	
	private void showErrorDialog(String message) {
		new ErrorDialog(this.getContext(), "Layer Manager Error", message).show();
	}
}
