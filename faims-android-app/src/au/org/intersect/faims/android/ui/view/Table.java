package au.org.intersect.faims.android.ui.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.beanshell.BeanShellLinker;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.CSSManager;
import au.org.intersect.faims.android.tasks.CancelableTask;
import au.org.intersect.faims.android.util.FileUtil;
import au.org.intersect.faims.android.util.StringUtil;

import com.google.inject.Inject;

@SuppressLint("SetJavaScriptEnabled")
public class Table extends WebView implements IView {
	
	class TableInterface {
		
		@JavascriptInterface
		public void onAction(int row) {
			beanShellLinker.set("_table_row", String.valueOf(row));
			beanShellLinker.set("_table_value", actionValues.get(row));
			beanShellLinker.execute(actionCallback);
		}

		@JavascriptInterface
		public void onLoad() {
			beanShellLinker.getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					loadUrl("javascript:restoreScrollPosition(" + scrollX + "," + scrollY + ")");
				}
				
			});
		}

		@JavascriptInterface
		public void setScrollPosition(int x, int y) {
			scrollX = x;
			scrollY = y;
		}

	}
	
	@Inject
	DatabaseManager databaseManager;
	
	@Inject
	BeanShellLinker beanShellLinker;
	
	@Inject
	CSSManager cssManager;
	
	private String query;
	private String actionName;
	private int actionIndex;
	private String actionCallback;
	private List<String> actionValues;
	private List<String> headers;
	private boolean pivot;
	private String cssFile;
		
	int scrollX;
	int scrollY;

	private String ref;
	private boolean dynamic;

	public Table(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
	}
	
	public Table(Context context, String ref, boolean dynamic) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		
		this.ref = ref;
		this.dynamic = dynamic;
		
		setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1));
		
		// settings
		WebSettings settings = getSettings();
		settings.setBuiltInZoomControls(true);
		settings.setSupportZoom(true);
		settings.setJavaScriptEnabled(true);
		setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		setScrollbarFadingEnabled(false);
		
		// add java interface
		addJavascriptInterface(new TableInterface(), "Android");
		cssManager.addCSS(this, "table-view");
	}
	
	@Override
	public String getRef() {
		return ref;
	}
	
	@Override
	public boolean isDynamic() {
		return dynamic;
	}

	public void populate(String query, List<String> headers, String actionName, int actionIndex, String actionCallback, boolean pivot) throws Exception {
		this.query = query;
		this.headers = headers;
		this.actionName = actionName;
		this.actionIndex = actionIndex;
		this.actionCallback = actionCallback;
		this.pivot = pivot;
		
		refresh();
	}

	public void refresh() throws Exception {
		if (this.query == null) return; // nothing to refresh
		this.actionValues = new ArrayList<String>();
		
		CancelableTask task = new CancelableTask() {

			private String table;

			@Override
			protected Void doInBackground(Void... params) {
				try {
					table = generateTable();
					return null;
				} catch (Exception e) {
					FLog.e("Error trying to load table", e);
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				if (table == null) {
					Table.this.loadDataWithBaseURL("file:///android_asset/", "<h1>Error trying to load table</h1>", "text/html", "utf-8", null);
				} else {
					loadUrl("javascript:saveScrollPosition()");
					Table.this.loadDataWithBaseURL("file:///android_asset/", table, "text/html", "utf-8", null);
				}
			}
			
		};
		task.execute();
	}

	public void style(String cssFile) throws Exception {
		this.cssFile = cssFile;
		refresh();
	}
	
	public void scrollToTop() {
		loadUrl("javascript:scrollToElement('page-top')");
	}
	
	public void scrollToBottom() {
		loadUrl("javascript:scrollToElement('page-bottom')");
	}
	
	public void scrollToRow(int num) {
		loadUrl("javascript:scrollToElement('row-" + num + "')");
	}
	
	private String generateTable() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append(readFileFromAssets("table.header.html"));
		if (cssFile != null) {
			sb.append("<style>" + FileUtil.readFileIntoString(cssFile) + "</style>");
		} else {
			sb.append("<link href=\"table.css\" type=\"text/css\" rel=\"stylesheet\"/>");
		}
		sb.append("<table id=\"table\" class=\"table\">");
		
		Collection<List<String>> results = databaseManager.fetchRecord().fetchAll(query);
		if (results != null) {
			sb.append(generateTableRow(headers, -1, true));	
			
			if (pivot) {
				results = pivotResults(results);
			}
			
			int count = 0;
			for (List<String> row : results) {
				sb.append(generateTableRow(row, count, false));
				count++;
			}
		}
		
		sb.append("</table>");
		sb.append(readFileFromAssets("table.footer.html"));
		return sb.toString();
	}
	
	private String generateTableRow(List<String> row, int count, boolean header) {
		StringBuilder sb = new StringBuilder();
		sb.append("<tr id=\"row-" + count + "\">");
		for (int i = 0; i < row.size(); i++) {
			String column = row.get(i);
			
			sb.append(header ? "<th>" : "<td>");
			
			// actions are displayed as buttons
			if (!header && i == actionIndex) {
				// store action value
				actionValues.add(column);
				
				// create button
				sb.append("<button type=\"button\" onclick=\"callAction(" + count + ")\">" + actionName + "</button>");
			} else {
				sb.append(column);
			}
			
			sb.append(header ? "</th>" : "</td>");
		}
		sb.append("</tr>");
		
		return sb.toString();
	}
	
	// This function assumes the results are ID, Name, Value
	private Collection<List<String>> pivotResults(Collection<List<String>> results) {
		// generate a map from ID -> (Name, Value)
		HashMap<String, HashMap<String, String>> map = new HashMap<String, HashMap<String, String>>();
		for (List<String> row : results) {
			String id = row.get(0);
			String name = row.get(1);
			String value = row.get(2);
			if (map.get(id) == null) {
				map.put(id, new HashMap<String, String>());
			}
			map.get(id).put(name, value);
		}
		
		// convert map to table rows
		ArrayList<List<String>> pivotResults = new ArrayList<List<String>>();
		for (String id : map.keySet()) {
			ArrayList<String> pivotRow = new ArrayList<String>();
			pivotRow.add(id);
			HashMap<String, String> nameValues = map.get(id);
			// add the columns in order by headers
			for (int i = 1; i < headers.size(); i++){
				String value = nameValues.get(headers.get(i));
				if (value == null) {
					pivotRow.add("");
				} else {
					pivotRow.add(value);
				}
			}
			pivotResults.add(pivotRow);
		}
		return pivotResults;
	}
	
	private String readFileFromAssets(String fileName) throws IOException {
		return StringUtil.streamToString(getContext().getAssets().open(fileName));
	}
	
	@Override
	public String getClickCallback() {
		return null;
	}

	@Override
	public void setClickCallback(String code) {
		
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
		return null;
	}

	@Override
	public String getBlurCallback() {
		return null;
	}

	@Override
	public void setFocusBlurCallbacks(String focusCode, String blurCode) {
	}

}
