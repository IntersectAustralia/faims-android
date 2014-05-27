package au.org.intersect.faims.android.ui.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.util.StringUtil;

import com.google.inject.Inject;

public class Table extends WebView {
	
	@Inject
	DatabaseManager databaseManager;
	
	private String query;
	private int actionIndex;
	private String actionCallback;
	private List<String> headers;
	private boolean pivot;

	public Table(Context context) {
		super(context);
		FAIMSApplication.getInstance().injectMembers(this);
		
		setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1));
		
		// initialise settings
		WebSettings settings = getSettings();
		settings.setBuiltInZoomControls(true);
		settings.setSupportZoom(true);
		setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		setScrollbarFadingEnabled(false);
	}

	public void populate(String query, List<String> headers, int actionIndex, String actionCallback, boolean pivot) throws Exception {
		this.query = query;
		this.headers = headers;
		this.actionIndex = actionIndex;
		this.actionCallback = actionCallback;
		this.pivot = pivot;
		
		this.loadDataWithBaseURL("file:///android_asset/", generateTableHTML(), "text/html", "utf-8", null);
	}
	
	private String generateTableHTML() throws Exception {
		Collection<List<String>> results = databaseManager.fetchRecord().fetchAll(query);
		StringBuilder sb = new StringBuilder();
		sb.append(readHtmlFromAssets("table_header.html"));
		sb.append("<table class=\"CSSTableGenerator\">");
		if (results != null) {
			sb.append(generateQueryRow(headers));	
			
			if (pivot) {
				results = pivotResults(results);
			}
			
			for (List<String> row : results) {
				sb.append(generateQueryRow(row));
			}
		}
		sb.append("</table>");
		sb.append(readHtmlFromAssets("table_footer.html"));
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
	
	private String generateQueryRow(List<String> row) {
		StringBuilder sb = new StringBuilder();
		sb.append("<tr>");
		for (String column : row) {
			sb.append("<td>");
			sb.append(column);
			sb.append("</td>");
		}
		sb.append("</tr>");
		return sb.toString();
	}
	
	private String readHtmlFromAssets(String fileName) throws IOException {
		return StringUtil.streamToString(getContext().getAssets().open(fileName));
	}

}
