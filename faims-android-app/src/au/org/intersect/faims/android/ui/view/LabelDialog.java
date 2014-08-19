package au.org.intersect.faims.android.ui.view;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.util.ScaleUtil;

public class LabelDialog extends AlertDialog {
	
	TabHost tabHost;

	private ICustomView view;
	
	private SeekBar certaintySeekBar;
	private TextView certaintyText;
	private EditText annotationText;
	private EditText dirtyReasonText;

	private ArrayList<String> tabs;
	
	public LabelDialog(Context context, ICustomView view) {
		super(context);
		this.view = view;
		
		tabs = new ArrayList<String>();
		
		setButton(DialogInterface.BUTTON_POSITIVE, getContext().getResources().getString(R.string.confirm_dialog_button),
				new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (certaintySeekBar != null) {
					System.out.println((float)certaintySeekBar.getProgress() / 100);
					LabelDialog.this.view.setCertainty((float)certaintySeekBar.getProgress() / 100);
				}
				if (annotationText != null) {
					LabelDialog.this.view.setAnnotation(annotationText.getText().toString());
				}
			}
		});
		
		setButton(DialogInterface.BUTTON_NEGATIVE, getContext().getResources().getString(R.string.cancel_dialog_button),
				new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// User cancelled the dialog
			}
		});

		View labelDialogView = getLayoutInflater().inflate(R.layout.label_dialog, null);
		setView(labelDialogView);
		
		tabHost = (TabHost) labelDialogView.findViewById(R.id.label_tabhost);
		tabHost.setup();
		tabHost.setOnTabChangedListener(new OnTabChangeListener() {
			
			@Override
			public void onTabChanged(String tabId) {
				View focus = getCurrentFocus();
				if (focus != null) {
					InputMethodManager keyboard = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			        keyboard.hideSoftInputFromWindow(focus.getWindowToken(), 0);
				}
			}
		});
	}
	
	public void addTab(String id, String title, TabContentFactory content) {
		tabs.add(id);
		TabHost.TabSpec tab = tabHost.newTabSpec(id);
		tab.setIndicator(title);
		tab.setContent(content);
		tabHost.addTab(tab);
	}
	
	public void addCertaintyTab() {
		addTab("certainty", "Certainty", new TabContentFactory() {

			@Override
			public View createTabContent(String tag) {
				LinearLayout layout = new LinearLayout(getContext());
				layout.setOrientation(LinearLayout.VERTICAL);
				certaintySeekBar = new SeekBar(getContext());
				certaintySeekBar.setMax(100);
				certaintySeekBar.setMinimumWidth((int) ScaleUtil.getDip(getContext(), 400));
				certaintyText = new TextView(getContext());
				
				updateCertainty();
				
				certaintySeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
					}
					
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}
					
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress,
							boolean fromUser) {
						updateCertainty(((float) progress)/100);
					}
				});
				layout.addView(certaintyText);
				layout.addView(certaintySeekBar);
				return layout;
			}
			
		});
	}
	
	public void addAnnotationTab() {
		addTab("annotation", "Annotation", new TabContentFactory() {

			@Override
			public View createTabContent(String tag) {
				LinearLayout layout = new LinearLayout(getContext());
				layout.setOrientation(LinearLayout.VERTICAL);
				
				TextView textView = new TextView(getContext());
				textView.setText("    Set the annotation text for the field");
				layout.addView(textView);
				
				annotationText = new EditText(getContext());
				layout.addView(annotationText);
				
				updateAnnotation();
				
				return layout;
			}
		});
	}

	public void addInfoTab(final String description, final String moduleDir) {
		addTab("info", "Info", new TabContentFactory() {

			@Override
			public View createTabContent(String tag) {
				ScrollView scrollView = new ScrollView(getContext());
				LinearLayout layout = new LinearLayout(getContext());
				WebView webView = new WebView(tabHost.getContext());
				webView.loadDataWithBaseURL("file:///" + moduleDir + "/", description, "text/html", null, null);
				layout.addView(webView);
				scrollView.addView(layout);
				return scrollView;
			}
		});
	}

	public void addDirtyTab() {
		addTab("dirty", "Dirty", new TabContentFactory() {

			@Override
			public View createTabContent(String tag) {
				ScrollView scrollView = new ScrollView(getContext());
				LinearLayout layout = new LinearLayout(getContext());
				layout.setOrientation(LinearLayout.VERTICAL);
				
				TextView label = new TextView(getContext());
				label.setText("Dirty Reason:");
				layout.addView(label);
				
				dirtyReasonText = new EditText(getContext());
				dirtyReasonText.setEnabled(false);
				layout.addView(dirtyReasonText);
				
				scrollView.addView(layout);
				
				updateDirtyReason();
				
				return scrollView;
			}
		});
	}
	
	private void setDirtyTextArea(EditText text, String value) {
		if (value == null || "".equals(value)) return;
		
		String[] lines = value.split(";");
		StringBuffer sb = new StringBuffer();
		int count = 0;
		for (String l : lines) {
			if (l.trim().equals("")) continue;
			sb.append(l);
			sb.append("\n");
			count++;
		}
		text.setLines(count);
		text.setText(sb.toString());
	}
	
	@Override
	public void show() {
		if (!hasNonDirtyTab() && !hasDirtyReasonText()) {
			return;
		}
		tabHost.getTabWidget().getChildAt(tabs.indexOf("dirty")).setVisibility(hasDirtyReasonText() ? View.VISIBLE : View.GONE);
		updateCertainty();
		updateAnnotation();
		updateDirtyReason();
		super.show();
	}
	
	private void updateCertainty() {
		updateCertainty(view.getCertainty());
	}
	
	private void updateCertainty(float certainty) {
		if (certaintySeekBar != null && certaintyText != null) {
			certaintySeekBar.setProgress((int) (certainty * 100));
			certaintyText.setText("    Certainty: " + certainty);
		}
	}
	
	private void updateAnnotation() {
		if (annotationText != null) {
			annotationText.setText(view.getAnnotation());
		}
	}
	
	private void updateDirtyReason() {
		if (dirtyReasonText != null) {
			setDirtyTextArea(dirtyReasonText, view.getDirtyReason());
		}
	}
	
	private boolean hasNonDirtyTab() {
		for (String tab : tabs) {
			if (!"dirty".equals(tab)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean hasDirtyReasonText() {
		if (dirtyReasonText != null) {
			String text = dirtyReasonText.getText().toString();
			if (text != null && !text.isEmpty()) {
				return true;
			}
		}
		return false;
	}

}
