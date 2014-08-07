package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.log.FLog;

import com.google.inject.Inject;
import com.nativecss.NativeCSS;

public class CustomListView extends ListView implements IView {
	
	@Inject
	BeanShellLinker linker;
	
	private String ref;
	private boolean dynamic;

	private String clickCallback;
	private String focusCallback;
	private String blurCallback;

	public CustomListView(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public CustomListView(Context context, String ref, boolean dynamic) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		this.ref = ref;
		this.dynamic = dynamic;
		NativeCSS.addCSSClass(this, "list");
	}
	
	@Override
	public String getRef() {
		return ref;
	}
	
	@Override
	public boolean isDynamic() {
		return dynamic;
	}
	
	public List<NameValuePair> getPairs() {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		for (int i = 0; i < getAdapter().getCount(); ++i) {
			NameValuePair pair = (NameValuePair) getItemAtPosition(i);
			pairs.add(pair);
		}
		return pairs;
	}
	
	public void setPairs(List<NameValuePair> pairs) {
		populate(pairs);
	}
	
	public void populate(List<NameValuePair> pairs) {
		ArrayAdapter<NameValuePair> arrayAdapter = new ArrayAdapter<NameValuePair>(this.getContext(),
				android.R.layout.simple_list_item_1, pairs);
		setAdapter(arrayAdapter);
	}
 
	@Override
	public String getClickCallback() {
		return clickCallback;
	}

	@Override
	public void setClickCallback(String code) {
		if (code == null) return;
		clickCallback = code;
		setOnItemClickListener(new ListView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0,
					View arg1, int index, long arg3) {
				try {
					NameValuePair pair = (NameValuePair) getItemAtPosition(index);
					linker.getInterpreter().set("_list_item_value", pair.getValue());
					linker.execute(clickCallback);
				} catch (Exception e) {
					FLog.e("error setting list item value", e);
				}
			}

		});
	}
	
	@Override
	public String getSelectCallback() {
		return null;
	}

	@Override
	public void setSelectCallback(String code) {
	}

	@Override
	public String getFocusCallback() {
		return focusCallback;
	}
	
	@Override
	public String getBlurCallback() {
		return blurCallback;
	}
	
	@Override
	public void setFocusBlurCallbacks(String focusCode, String blurCode) {
		if (focusCode == null && blurCode == null) return;
		focusCallback = focusCode;
		blurCallback = blurCode;
		setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					linker.execute(focusCallback);
				} else {
					linker.execute(blurCallback);
				}
			}
		});
	}
}
