package au.org.intersect.faims.android.ui.map;

import android.content.Context;
import android.util.TypedValue;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.org.intersect.faims.android.data.User;
import au.org.intersect.faims.android.util.ScaleUtil;

public class UserListItem extends LinearLayout{

	private TextView text;
	private CheckBox showBox;
	private User user;

	public UserListItem(Context context) {
		super(context);
		setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
		setOrientation(LinearLayout.HORIZONTAL);
		int size = (int) ScaleUtil.getDip(context, 10);
		setPadding(size, size, size, size);
	}
	
	public void init(User user, boolean isChecked) {
		text = new TextView(this.getContext());
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		text.setLayoutParams(params);
		int size = (int) ScaleUtil.getDip(this.getContext(), 5);
		text.setPadding(size, size, size, size);
		text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
		text.setSingleLine(false);
		text.setText(user.getFirstName() + " " + user.getLastName());
		this.user = user;
		showBox = new CheckBox(this.getContext());
		showBox.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		showBox.setChecked(isChecked);
		showBox.setFocusable(false);
		
		addView(text);
		addView(showBox);
	}

	public void toggle() {
		showBox.toggle();
	}

	public User getUser(){
		return user;
	}

	public boolean isChecked() {
		return showBox.isChecked();
	}

}
