package au.org.intersect.faims.android.ui.map;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.nutiteq.layers.Layer;

public class LayerManagerView extends LinearLayout {
	
	private class LayersAdapter extends BaseAdapter {
		
		private List<Layer> layers;
		private ArrayList<View> itemViews;

		public LayersAdapter(List<Layer> layers) {
			this.layers = layers;
			this.itemViews = new ArrayList<View>();
			
			for (Layer layer : layers) {
				LayerListItem item = new LayerListItem(LayerManagerView.this.getContext());
				item.init(layer);
				itemViews.add(item);
			} 
		}
		
		@Override
		public int getCount() {
			return layers.size();
		}

		@Override
		public Object getItem(int position) {
			return layers.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View arg1, ViewGroup arg2) {
			return itemViews.get(position);
		}
		
	}

	private Button addButton;
	private Button removeButton;
	private Button renameButton;
	private CustomMapView mapView;
	private ListView listView;
	private LinearLayout layout;
	private LinearLayout buttonsLayout;

	public LayerManagerView(Context context) {
		super(context);
		
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		setOrientation(LinearLayout.VERTICAL);
	}
	
	public void attachToMap(CustomMapView mapView) {
		this.mapView = mapView;
		
		layout = new LinearLayout(this.getContext());
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		layout.setOrientation(LinearLayout.VERTICAL);
		addView(layout);
		
		buttonsLayout = new LinearLayout(this.getContext());
		buttonsLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
		layout.addView(buttonsLayout);
		
		createAddButton();
		createRemoveButton();
		createRenameButton();
		createListView();
		
		redrawLayers();
	}
	
	private void createListView() {
		listView = new ListView(this.getContext());
		listView.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		layout.addView(listView);
	}
	
	private void createAddButton() {
		addButton = new Button(this.getContext());
		addButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		addButton.setText("Add");
		addButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
				
				builder.setTitle("Layer Manager");
				builder.setMessage("Enter layer name:");
				
				final EditText editText = new EditText(LayerManagerView.this.getContext());
				builder.setView(editText);
				
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						try {
							mapView.addCanvasLayer(editText.getText().toString());
							redrawLayers();
						} catch (Exception e) {
							showErrorDialog(e.getMessage());
						}
					}
			        
			    });
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int id) {
			           // ignore
			        }
			    });
				
				builder.create().show();
			}
			
		});
		
		buttonsLayout.addView(addButton);
	}
	
	private void createRemoveButton() {
		removeButton = new Button(this.getContext());
		removeButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		removeButton.setText("Remove");
		
		removeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				// TODO
			}
			
		});
		buttonsLayout.addView(removeButton);
	}
	
	private void createRenameButton() {
		renameButton = new Button(this.getContext());
		renameButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		renameButton.setText("Rename");
		
		renameButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
				
				builder.setTitle("Layer Manager");
				builder.setMessage("Enter layer name:");
				
				final EditText editText = new EditText(LayerManagerView.this.getContext());
				builder.setView(editText);
				
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// TODO
					}
			        
			    });
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int id) {
			           // ignore
			        }
			    });
				
				builder.create().show();
			}
			
		});
		buttonsLayout.addView(renameButton);
	}
	
	private void showErrorDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager Error");
		builder.setMessage(message);
		builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		builder.create().show();
	}
	
	private void redrawLayers() {
		List<Layer> layers = mapView.getLayers().getAllLayers();
		LayersAdapter layersAdapter = new LayersAdapter(layers);
		listView.setAdapter(layersAdapter);
	}

}
