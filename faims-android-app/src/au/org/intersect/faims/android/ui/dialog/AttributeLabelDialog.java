package au.org.intersect.faims.android.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TabHost.TabContentFactory;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.ui.view.ICustomView;

public class AttributeLabelDialog extends LabelDialog {
	
	class AttributeLabelDialogClickListener implements OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (certaintySeekBar != null) {
				AttributeLabelDialog.this.view.setCertainty((float)certaintySeekBar.getProgress() / 100);
			}
			if (annotationText != null) {
				AttributeLabelDialog.this.view.setAnnotation(annotationText.getText().toString());
			}
		}
		
	}
	
	private ICustomView view;
	
	protected EditText dirtyReasonText;
	
	public AttributeLabelDialog(Context context, ICustomView view) {
		super(context);
		
		this.view = view;
		
		setButton(DialogInterface.BUTTON_POSITIVE, getContext().getResources().getString(R.string.confirm_dialog_button), new AttributeLabelDialogClickListener());
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
		if (tabs.indexOf("dirty") != -1) {
			tabHost.getTabWidget().getChildAt(tabs.indexOf("dirty")).setVisibility(hasDirtyReasonText() ? View.VISIBLE : View.GONE);
		}
		updateAnnotation();
		updateCertainty();
		updateDirtyReason();
		tabHost.setCurrentTab(0);
		super.show();
	}
	
	@Override
	protected void updateAnnotation() {
		if (annotationText != null) {
			annotationText.setText(view.getAnnotation());
		}
	}
	
	@Override
	protected void updateCertainty() {
		updateCertainty(view.getCertainty());
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
		if (view != null) {
			return view.getDirtyReason() != null && !view.getDirtyReason().isEmpty();
		}
		return false;
	}
	
}
