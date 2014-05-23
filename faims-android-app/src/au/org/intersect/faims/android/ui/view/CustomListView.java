package au.org.intersect.faims.android.ui.view;

import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class CustomListView extends ListView {
	
	public CustomListView(Context context) {
		super(context);
	}

	public void populate(List<NameValuePair> pairs) {
		ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(this.getContext(),
				android.R.layout.simple_list_item_1, pairs);
		setAdapter(arrayAdapter);
	}
}
