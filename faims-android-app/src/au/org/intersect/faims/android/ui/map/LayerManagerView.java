package au.org.intersect.faims.android.ui.map;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import au.org.intersect.faims.android.ui.dialog.ErrorDialog;

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
				item.init(layer, LayerManagerView.this);
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
		createListView();
		
		redrawLayers();
	}
	
	private void createListView() {
		listView = new ListView(this.getContext());
		listView.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position,
					long arg3) {
				final Layer layer = mapView.getLayers().getAllLayers().get(position);
				LayerListItem itemView = (LayerListItem) view;
				itemView.toggle();
				layer.setVisible(itemView.isChecked());
				mapView.updateTools();
			}
			
		});
		
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				final Layer layer = mapView.getLayers().getAllLayers().get(position);
				
				Context context = LayerManagerView.this.getContext();
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Layer Options");

				LinearLayout layout = new LinearLayout(context);
				layout.setOrientation(LinearLayout.VERTICAL);
				
				builder.setView(layout);
				final Dialog d = builder.create();
				
				Button removeButton = new Button(context);
				removeButton.setText("Remove");
				removeButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						d.dismiss();
						removeLayer(layer);
					}
					
				});
				
				Button renameButton = new Button(context);
				renameButton.setText("Rename");
				renameButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						d.dismiss();
						renameLayer(layer);
					}
					
				});
				
				layout.addView(removeButton);
				layout.addView(renameButton);
				
				d.show();
				return true;
			}
			
		});

		layout.addView(listView);
	}
	
	private void createAddButton() {
		addButton = new Button(this.getContext());
		addButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		addButton.setText("Add");
		addButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				addLayer();
			}
			
		});
		
		buttonsLayout.addView(addButton);
	}
	
	public void redrawLayers() {
		List<Layer> layers = mapView.getLayers().getAllLayers();
		LayersAdapter layersAdapter = new LayersAdapter(layers);
		listView.setAdapter(layersAdapter);
	}
	
	private void addLayer() {
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

	private void removeLayer(final Layer layer) {
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Do you want to delete layer?");
		
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					mapView.removeLayer(layer);
					redrawLayers();
				} catch (Exception e) {
					showErrorDialog(e.getMessage());
				}
			}
	        
	    });
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		builder.create().show();
	}

	private void renameLayer(final Layer layer) {
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Enter layer name:");
		
		final EditText editText = new EditText(LayerManagerView.this.getContext());
		builder.setView(editText);
		
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					mapView.renameLayer(layer, editText.getText().toString());
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
	
	private void showErrorDialog(String message) {
		new ErrorDialog(LayerManagerView.this.getContext(), "Layer Manager Error", message).show();
	}

}
