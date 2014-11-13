package au.org.intersect.faims.android.ui.map.tools;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.LegacyQueryBuilder;
import au.org.intersect.faims.android.ui.map.QueryBuilder;
import au.org.intersect.faims.android.ui.map.button.PropertiesButton;
import au.org.intersect.faims.android.ui.map.button.ToolBarButton;
import au.org.intersect.faims.android.util.ScaleUtil;

public class LegacySelectionTool extends SelectionTool {

	public static final String NAME = "Legacy Selection";
	private PropertiesButton queryButton;

	public LegacySelectionTool(Context context, CustomMapView mapView) {
		super(context, mapView, NAME);
		
		queryButton = createQueryButton(context);
		RelativeLayout.LayoutParams queryParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		queryParams.alignWithParent = true;
		queryParams.addRule(RelativeLayout.ALIGN_LEFT);
		queryParams.topMargin = (int) ScaleUtil.getDip(context, buttons.size() * HEIGHT + TOP_MARGIN);
		queryButton.setLayoutParams(queryParams);
		buttons.add(queryButton);
		
		updateLayout();
	}
	
	protected void updateLayout() {
		if (queryButton != null) {
			layout.removeAllViews();
			layout.addView(selectionManagerButton);
			layout.addView(queryButton);
			layout.addView(infoLayout);
		}
	}
	
	private PropertiesButton createQueryButton(final Context context) {
		PropertiesButton button = new PropertiesButton(context);
		button.setLabel("Query");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Query Selection Tool");
				
				ScrollView scrollView = new ScrollView(context);
				builder.setView(scrollView);
				
				final LinearLayout queryLayout = new LinearLayout(context);
				queryLayout.setOrientation(LinearLayout.VERTICAL);
				scrollView.addView(queryLayout);

				final Spinner sqlSpinner = new Spinner(context);
				queryLayout.addView(sqlSpinner);
				
				final TextView removeLabel = new TextView(context);
				removeLabel.setText("Remove from selection");
				queryLayout.addView(removeLabel);
				
				final CheckBox removeBox = new CheckBox(context);
				queryLayout.addView(removeBox);
				
				final Button runButton = new Button(context);
				runButton.setText("Run Query");
				queryLayout.addView(runButton);
				
				final Dialog d = builder.create();
				
				final ArrayList<EditText> textViews = new ArrayList<EditText>();
				final List<LegacyQueryBuilder> builders = mapView.getLegacySelectQueryBuilders();
				ArrayList<String> names = new ArrayList<String>();
				for (QueryBuilder qb : builders) {
					names.add(qb.getName());
				}
				sqlSpinner.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, names));
				sqlSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int position, long arg3) {
						queryLayout.removeAllViews();
						textViews.clear();
						
						queryLayout.addView(sqlSpinner);
						
						LegacyQueryBuilder qb = builders.get(position);
						
						List<QueryBuilder.Parameter> parameters = qb.getParameters();
						for (QueryBuilder.Parameter p : parameters) {
							TextView label = new TextView(context);
							label.setText(p.name);
							queryLayout.addView(label);

							EditText text = new EditText(context);
							text.setInputType(InputType.TYPE_CLASS_TEXT);
							if (p.defaultValue != null) text.setText(p.defaultValue);
							textViews.add(text);
							queryLayout.addView(text);
						}
						
						queryLayout.addView(removeLabel);
						queryLayout.addView(removeBox);
						queryLayout.addView(runButton);
						
						d.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						// TODO Auto-generated method stub
						
					}
					
				});
				
				runButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						String name = sqlSpinner.getSelectedItem() != null ? (String) sqlSpinner.getSelectedItem() : null;
						ArrayList<String> values = new ArrayList<String>();
						for (EditText t : textViews) {
							String text = t.getText().toString();
							values.add(text);
						}
						
						try {
							mapView.runLegacySelectionQuery(name, values, removeBox.isChecked());
						} catch (Exception e) {
							FLog.e(e.getMessage(), e);
							showError(e.getMessage());
						}
						
					}
					
				});
				
				if (mapView.getLastSelectionQuery() != null) {
					sqlSpinner.setSelection(names.indexOf(mapView.getLastSelectionQuery()));
				}
				
				d.setCanceledOnTouchOutside(true);
				d.show();
			}
			
		});
		return button;
		
	}
	
	public ToolBarButton getButton(Context context) {
		ToolBarButton button = new ToolBarButton(context);
		button.setLabel("Legacy");
		button.setSelectedState(R.drawable.tools_select_legacy_s);
		button.setNormalState(R.drawable.tools_select_legacy);
		return button;
	}

}
