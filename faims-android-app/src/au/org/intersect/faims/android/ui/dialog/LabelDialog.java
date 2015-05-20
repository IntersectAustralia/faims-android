package au.org.intersect.faims.android.ui.dialog;

import java.util.ArrayList;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.util.ScaleUtil;

public class LabelDialog extends AlertDialog {
	
	private static final String ANNOTATION_TAB = "annotation";
	private static final String CERTAINTY_TAB = "certainty";
	private static final float SCALE = 0.9f;
	
	TabHost tabHost;

	protected SeekBar certaintySeekBar;
	private TextView certaintyText;
	protected EditText annotationText;

	protected ArrayList<String> tabs;
	
	@SuppressLint("InflateParams")
	public LabelDialog(Context context) {
		super(context);
		
		tabs = new ArrayList<String>();
		
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
		// Hide soft keyboard on tab change
		tabHost.setOnTabChangedListener(new OnTabChangeListener() {
			
			@Override
			public void onTabChanged(String tabId) {
				handleTabChange(tabId);
			}
		});
	}
	
	private void handleTabChange(String tabId) {
		View focus = getCurrentFocus();
		if (focus != null) {
			InputMethodManager keyboard = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
	        keyboard.hideSoftInputFromWindow(focus.getWindowToken(), 0);
		}
		onTabChanged(tabId);
	}
	
	protected void onTabChanged(String tabId) {
		
	}
	
	protected void addTab(String id, String title, TabContentFactory content) {
		tabs.add(id);
		TabHost.TabSpec tab = tabHost.newTabSpec(id);
		tab.setIndicator(title);
		tab.setContent(content);
		tabHost.addTab(tab);
	}
	
	public void addCertaintyTab() {
		addTab(CERTAINTY_TAB, "Certainty", new TabContentFactory() {

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
		addTab(ANNOTATION_TAB, "Annotation", new TabContentFactory() {

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

	protected void updateAnnotation() {
	}
	
	protected void updateCertainty() {
	}

	protected void updateCertainty(float certainty) {
		if (certaintySeekBar != null && certaintyText != null) {
			certaintySeekBar.setProgress((int) (certainty * 100));
			certaintyText.setText("    Certainty: " + certainty);
		}
	}
	
	protected void fullScreen() {
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindow().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		
		WindowManager.LayoutParams params = getWindow().getAttributes();
		params.width = (int) (displaymetrics.widthPixels * SCALE);
		params.height = (int) (displaymetrics.heightPixels * SCALE);
		getWindow().setAttributes(params);
	}
	
	protected void defaultScreen() {
		getWindow().setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}
	
}
