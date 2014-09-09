package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.tasks.CancelableTask;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.google.inject.Inject;
import com.nativecss.NativeCSS;

public class CustomListView extends ListView implements IView {
	
	@Inject
	DatabaseManager databaseManager;
	
	@Inject
	BeanShellLinker linker;
	
	private String ref;
	private boolean dynamic;

	private String clickCallback;
	private String focusCallback;
	private String blurCallback;
	
	private String query;
	private ArrayList<NameValuePair> items;
	private ArrayAdapter<NameValuePair> arrayAdapter;
	private boolean disableLoad;
	
	private LinearLayout loadingView;
	private Animation rotation;

	public CustomListView(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public CustomListView(Context context, String ref, boolean dynamic) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		this.ref = ref;
		this.dynamic = dynamic;
		setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		NativeCSS.addCSSClass(this, "list");
	}
	
	private void setupLoadingSpinner() {
		loadingView = new LinearLayout(getContext());
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, (int) ScaleUtil.getDip(getContext(), 50));
		loadingView.setLayoutParams(params);
		loadingView.setGravity(Gravity.CENTER_HORIZONTAL);
		
		ImageView loading = new ImageView(getContext());
		loading.setBackgroundResource(R.drawable.loading_wheel);
		
		rotation = AnimationUtils.loadAnimation(linker.getActivity(), R.anim.clockwise);
		rotation.setRepeatCount(Animation.INFINITE);
		
		loadingView.addView(loading);
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
		setOnScrollListener(null);
		arrayAdapter = new ArrayAdapter<NameValuePair>(this.getContext(),
				android.R.layout.simple_list_item_1, pairs);
		setAdapter(arrayAdapter);
	}
	
	public void populateWithCursor(String query, final int limit) throws Exception {
		this.query = query;
		setupLoadingSpinner();
		
		List<List<String>> values = databaseManager.fetchRecord().fetchAll(query);
		int end = limit >= values.size() ? values.size() : limit;
		items = linker.convertToNameValuePairs(values.subList(0, end));
		arrayAdapter = new ArrayAdapter<NameValuePair>(this.getContext(),
				android.R.layout.simple_list_item_1, items);
		setAdapter(arrayAdapter);
		
		setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				disableLoad = false;
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if (firstVisibleItem + visibleItemCount >= totalItemCount) {
					try {
						if (!disableLoad) {
							loadMoreItems(limit, totalItemCount);
						}
					} catch (Exception e) {
						FLog.e("error updating cursor list " + ref, e);
						linker.showWarning("Logic Error", "Error updating cursor list " + ref);
					}
				}
			}
		});
	}
 
	private void loadMoreItems(final int extra, final int shownItems) throws Exception {
		if (getFooterViewsCount() > 0) {
			return;
		}
		addFooterView(loadingView);
		loadingView.getChildAt(0).startAnimation(rotation);
		CancelableTask task = new CancelableTask() {
			
			private List<List<String>> values;
			
			@Override
			protected Void doInBackground(Void... params) {
				try {
					values = databaseManager.fetchRecord().fetchAll(query);
					if (values.size() == shownItems) {
						disableLoad = true;
					}
				} catch (Exception e) {
					FLog.e("error updating cursor list " + ref, e);
					linker.showWarning("Logic Error", "Error updating cursor list " + ref);
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				if (items.size() == values.size()) {
					removeFooterView(loadingView);
					loadingView.getChildAt(0).clearAnimation();
					return;
				}
				int end = items.size() + extra >= values.size() ? values.size() : items.size() + extra;
				try {
					items.addAll(linker.convertToNameValuePairs(values.subList(items.size(), end)));
				} catch (Exception e) {
					FLog.e("error updating cursor list " + ref, e);
					linker.showWarning("Logic Error", "Error updating cursor list " + ref);
				}
				arrayAdapter.notifyDataSetChanged();
				removeFooterView(loadingView);
				loadingView.getChildAt(0).clearAnimation();
				super.onPostExecute(result);
			}
		};
		task.execute();
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
