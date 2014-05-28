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
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.util.StringUtil;

import com.google.inject.Inject;

@SuppressLint("SetJavaScriptEnabled")
public class Table extends WebView {
	
	class TableInterface {
		
		@JavascriptInterface
		public void callAction(int row) {
			beanShellLinker.execute(actionCallback);
		}
		
	}
	
	@Inject
	DatabaseManager databaseManager;
	
	@Inject
	BeanShellLinker beanShellLinker;
	
	private String query;
	private int actionIndex;
	private String actionCallback;
	private List<String> actionValues;
	private List<String> headers;
	private boolean pivot;

	public Table(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		
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
	}

	public void populate(String query, List<String> headers, int actionIndex, String actionCallback, boolean pivot) throws Exception {
		this.query = query;
		this.headers = headers;
		this.actionIndex = actionIndex;
		this.actionCallback = actionCallback;
		this.pivot = pivot;
		
		this.loadDataWithBaseURL("file:///android_asset/", generateTable(), "text/html", "utf-8", null);
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
		sb.append("<table class=\"table\">");
		
		actionValues = new ArrayList<String>();
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
				sb.append("<button type=\"button\" onclick=\"callAction(" + count + ")\">Click Me!</button>");
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

}
