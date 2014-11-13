package au.org.intersect.faims.android.ui.dialog;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.VideoView;
import au.org.intersect.faims.android.two.R;
import au.org.intersect.faims.android.data.NameValuePair;
import au.org.intersect.faims.android.util.ScaleUtil;

public class FileGalleryPreviewDialog extends AlertDialog {

	TabHost tabHost;
	
	private VideoView videoView;
	
	public FileGalleryPreviewDialog(Context context) {
		super(context);
		
		View dialogView = getLayoutInflater().inflate(R.layout.label_dialog, null);
		setView(dialogView);
		
		setButton(DialogInterface.BUTTON_POSITIVE, getContext().getResources().getString(R.string.confirm_dialog_button),
				new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do nothing
					}
				});
		
		tabHost = (TabHost) dialogView.findViewById(R.id.label_tabhost);
		tabHost.setup();
		
		tabHost.setOnTabChangedListener(new OnTabChangeListener() {
			
			@Override
			public void onTabChanged(String tabId) {
				// Hide/show video preview so it's not visible in background on tab change
				if ("video_preview".equals(tabId)) {
					videoView.setVisibility(View.VISIBLE);
					videoView.start();
				} else {
					if (videoView != null) {
						videoView.setVisibility(View.GONE);
					}
				}
			}
		});
	}

	public void addCameraPreview(final View view) {
		TabHost.TabSpec tab = tabHost.newTabSpec("camera_preview");
		tab.setIndicator("Image Preview");
		tab.setContent(new TabContentFactory() {
			
			@Override
			public View createTabContent(String tag) {
				ScrollView scrollView = new ScrollView(getContext());
				LinearLayout layout = new LinearLayout(getContext());
				layout.setOrientation(LinearLayout.VERTICAL);
				scrollView.addView(layout);
				
				layout.addView(view);
				
				return scrollView;
			}
		});
		tabHost.addTab(tab);
	}
	
	public void addVideoPreview(View view) {
		if (view instanceof VideoView) {
			videoView = (VideoView) view;
		}
		
		TabHost.TabSpec tab = tabHost.newTabSpec("video_preview");
		tab.setIndicator("Video Preview");
		tab.setContent(new TabContentFactory() {
			
			@Override
			public View createTabContent(String tag) {
				LinearLayout layout = new LinearLayout(getContext());
				layout.setOrientation(LinearLayout.VERTICAL);
				
				layout.addView(videoView, new LayoutParams(LayoutParams.MATCH_PARENT,
						LayoutParams.MATCH_PARENT));
				
				return layout;
			}
		});
		tabHost.addTab(tab);
	}
	
	public void addFileMetadataTab(final ArrayList<NameValuePair> metadata) {
		TabHost.TabSpec tab = tabHost.newTabSpec("file_metadata");
		tab.setIndicator("File Metadata");
		tab.setContent(new TabContentFactory() {
			
			@Override
			public View createTabContent(String tag) {
				LinearLayout layout = new LinearLayout(getContext());
				layout.setOrientation(LinearLayout.VERTICAL);
				
				TableLayout table = new TableLayout(getContext());
				LayoutInflater inflater = (LayoutInflater) FileGalleryPreviewDialog.this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				for (NameValuePair dataItem : metadata) {
					TableRow rowView = (TableRow) inflater.inflate(R.layout.static_module_data_row, null);
					TextView label = (TextView) rowView.findViewById(R.id.static_data_label);
					label.setText(dataItem.getName());
					TextView value = (TextView) rowView.findViewById(R.id.static_data_value);
					value.setText(dataItem.getValue());
					table.addView(rowView);
				}
				
				layout.addView(table);
				return layout;
			}
		});
		tabHost.addTab(tab);
	}
	
	public void addActionsTab(final String label, final View.OnClickListener listener) {
		TabHost.TabSpec tab = tabHost.newTabSpec("file_actions");
		tab.setIndicator("Actions");
		tab.setContent(new TabContentFactory() {
			
			@Override
			public View createTabContent(String tag) {
				LinearLayout layout = new LinearLayout(getContext());
				layout.setOrientation(LinearLayout.VERTICAL);
				
				Button remove = (Button) LayoutInflater.from(getContext()).inflate(R.layout.button_danger, null);
				LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				params.topMargin = (int) ScaleUtil.getDip(getContext(), 10);
				remove.setLayoutParams(params);
				remove.setText(label);
				remove.setOnClickListener(listener);
				layout.addView(remove);
				
				return layout;
			}
		});
		tabHost.addTab(tab);
	}
}
