package au.org.intersect.faims.android.ui.view;

import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import au.org.intersect.faims.android.data.NameValuePair;

public class CustomListView extends ListView implements IView {
	
	private String ref;
	private boolean dynamic;

	public CustomListView(Context context) {
		super(context);
	}
	
	public CustomListView(Context context, String ref, boolean dynamic) {
		super(context);
		this.ref = ref;
		this.dynamic = dynamic;
	}
	
	@Override
	public String getRef() {
		return ref;
	}
	
	@Override
	public boolean isDynamic() {
		return dynamic;
	}

	public void populate(List<NameValuePair> pairs) {
		ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(this.getContext(),
				android.R.layout.simple_list_item_1, pairs);
		setAdapter(arrayAdapter);
	}
}
