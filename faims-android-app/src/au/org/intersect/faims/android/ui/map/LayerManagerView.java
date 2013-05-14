package au.org.intersect.faims.android.ui.map;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.FileManager;
import au.org.intersect.faims.android.ui.activity.ShowProjectActivity;
import au.org.intersect.faims.android.ui.dialog.ErrorDialog;
import au.org.intersect.faims.android.ui.form.CustomDragDropListView;

import com.nutiteq.components.Color;
import com.nutiteq.layers.Layer;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.Style;
import com.nutiteq.style.StyleSet;

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
	private ToggleButton orderButton;
	private CustomMapView mapView;
	private CustomDragDropListView listView;
	private LinearLayout layout;
	private LinearLayout buttonsLayout;
	private FileManager fm;
	private TextView selectedFileText;

	public LayerManagerView(Context context) {
		super(context);
		
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		setOrientation(LinearLayout.VERTICAL);
	}
	
	public LayerManagerView(Context context, FileManager fm) {
		this(context);
		this.fm = fm;
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
		createOrderButton();
		createListView();
		
		redrawLayers();
	}
	
	private void createListView() {
		listView = new CustomDragDropListView(this.getContext(),null);
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
	
	// Drop Listener
	private CustomDragDropListView.DropListener dropListener = new CustomDragDropListView.DropListener() {
		public void drop(int from, int to) {
			if(from != 0 && to != 0){
				List<Layer> unmodifiableLayers = mapView.getLayers().getAllLayers();
				List<Layer> modifiedLayers = new ArrayList<Layer>(unmodifiableLayers);
				Collections.swap(modifiedLayers, from, to);
				modifiedLayers.remove(0);
				mapView.getLayers().setLayers(modifiedLayers);
				redrawLayers();
			}
		}
	};

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
	
	private void createOrderButton(){
		orderButton = new ToggleButton(this.getContext());
		orderButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		orderButton.setTextOn("Order ON");
		orderButton.setTextOff("Order OFF");
		orderButton.setChecked(false);
		orderButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					listView.setDropListener(dropListener);
				}else{
					listView.removeDropListener();
				}
			}
		});
		buttonsLayout.addView(orderButton);
	}
	
	public void redrawLayers() {
		List<Layer> layers = mapView.getLayers().getAllLayers();
		LayersAdapter layersAdapter = new LayersAdapter(layers);
		listView.setAdapter(layersAdapter);
	}
	
	private void addLayer() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle("Add Layer");

		LinearLayout layout = new LinearLayout(getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		
		builder.setView(layout);
		final Dialog d = builder.create();
		
		Button loadRasterLayerButton = new Button(getContext());
		loadRasterLayerButton.setText("Load Raster Layer");
		loadRasterLayerButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				addRasterLayer();
			}
			
		});
		
		// TODO Fix the bug with loading shape layer
//		Button loadShapeLayerButton = new Button(getContext());
//		loadShapeLayerButton.setText("Load Shape Layer");
//		loadShapeLayerButton.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View arg0) {
//				d.dismiss();
//				addShapeLayer();
//			}
//			
//		});
		
		Button loadSpatialLayerButton = new Button(getContext());
		loadSpatialLayerButton.setText("Load Spatial Layer");
		loadSpatialLayerButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				addSpatialLayer();
			}
			
		});
		
		Button createLayerButton = new Button(getContext());
		createLayerButton.setText("Create Layer");
		createLayerButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				createLayer();
			}
			
		});
		
		layout.addView(loadRasterLayerButton);
//		layout.addView(loadShapeLayerButton);
		layout.addView(loadSpatialLayerButton);
		layout.addView(createLayerButton);
		
		d.show();
	}
	
	private void addRasterLayer(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Add raster layer:");
		
		LinearLayout layout = new LinearLayout(getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		
		builder.setView(layout);
		final Dialog d = builder.create();
		
		TextView textView = new TextView(this.getContext());
		textView.setText("Raster layer name:");
		layout.addView(textView);
		final EditText editText = new EditText(LayerManagerView.this.getContext());
		layout.addView(editText);
		
		Button browserButton = new Button(getContext());
		browserButton.setText("browse");
		browserButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				showFileBrowser();
			}
		});
		layout.addView(browserButton);
		selectedFileText = new TextView(this.getContext());
		layout.addView(selectedFileText);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					if(fm.getSelectedFile() != null){
						mapView.addRasterMap(editText.getText().toString(), fm.getSelectedFile().getPath());
						fm.setSelectedFile(null);
						redrawLayers();
					}
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
	
	/*
	private void addShapeLayer(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Add shape layer:");
		
		LinearLayout layout = new LinearLayout(getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		
		builder.setView(layout);
		final Dialog d = builder.create();
		
		TextView textView = new TextView(this.getContext());
		textView.setText("Shape layer name:");
		layout.addView(textView);
		final EditText editText = new EditText(LayerManagerView.this.getContext());
		layout.addView(editText);
		
		Button browserButton = new Button(getContext());
		browserButton.setText("browse");
		browserButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				showFileBrowser();
			}
		});
		layout.addView(browserButton);
		selectedFileText = new TextView(this.getContext());
		layout.addView(selectedFileText);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					if(fm.getSelectedFile() != null){
						StyleSet<PointStyle> ps = (StyleSet<PointStyle>) createStyleSet(10, createPointStyle(Color.RED, 0.05f, 0.1f));
						StyleSet<LineStyle> ls = (StyleSet<LineStyle>) createStyleSet(10, createLineStyle(Color.GREEN, 0.01f, 0.01f, null));
						StyleSet<PolygonStyle> pos = (StyleSet<PolygonStyle>) createStyleSet(10, createPolygonStyle(Color.BLUE, createLineStyle(Color.BLACK, 0.01f, 0.01f, null)));
						mapView.addShapeLayer(editText.getText().toString(), fm.getSelectedFile().getPath(), ps, ls, pos);
						fm.setSelectedFile(null);
						redrawLayers();
					}
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
	*/
	
	private void addSpatialLayer(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Add spatial layer:");
		
		LinearLayout layout = new LinearLayout(getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		
		builder.setView(layout);
		final Dialog d = builder.create();
		
		TextView textView = new TextView(this.getContext());
		textView.setText("Spatial layer name:");
		layout.addView(textView);
		final EditText editText = new EditText(LayerManagerView.this.getContext());
		layout.addView(editText);
		
		TextView tableTextView = new TextView(this.getContext());
		tableTextView.setText("Spatial table name:");
		layout.addView(tableTextView);
		final EditText tableEditText = new EditText(LayerManagerView.this.getContext());
		layout.addView(tableEditText);
		
		Button browserButton = new Button(getContext());
		browserButton.setText("browse");
		browserButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				showFileBrowser();
			}
		});
		layout.addView(browserButton);
		selectedFileText = new TextView(this.getContext());
		layout.addView(selectedFileText);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					if(fm.getSelectedFile() != null){
						StyleSet<PointStyle> ps = (StyleSet<PointStyle>) createStyleSet(10, createPointStyle(Color.RED, 0.05f, 0.1f));
						StyleSet<LineStyle> ls = (StyleSet<LineStyle>) createStyleSet(10, createLineStyle(Color.GREEN, 0.01f, 0.01f, null));
						StyleSet<PolygonStyle> pos = (StyleSet<PolygonStyle>) createStyleSet(10, createPolygonStyle(Color.BLUE, createLineStyle(Color.BLACK, 0.01f, 0.01f, null)));
						mapView.addSpatialLayer(editText.getText().toString(), fm.getSelectedFile().getPath(), tableEditText.getText().toString(), null, ps, ls, pos);
						fm.setSelectedFile(null);
						redrawLayers();
					}
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
	
	public PointStyle createPointStyle(int color, float size, float pickSize) {
		return PointStyle.builder().setColor(color).setSize(size).setPickingSize(pickSize).build();
	}
	
	public LineStyle createLineStyle(int color, float width, float pickWidth) {
		return LineStyle.builder().setColor(color).setWidth(width).setPickingWidth(pickWidth).build();
	}
	
	public LineStyle createLineStyle(int color, float width, float pickWidth, PointStyle pointStyle) {
		return LineStyle.builder().setColor(color).setWidth(width).setPickingWidth(pickWidth).setPointStyle(pointStyle).build();
	}
	
	public PolygonStyle createPolygonStyle(int color) {
		return PolygonStyle.builder().setColor(color).build();
	}
	
	public PolygonStyle createPolygonStyle(int color, LineStyle lineStyle) {
		return PolygonStyle.builder().setColor(color).setLineStyle(lineStyle).build();
	}
	
	public StyleSet<? extends Style> createStyleSet(int minZoom, Style style) {
		if (style instanceof PointStyle) {
			StyleSet<PointStyle> pointStyleSet = new StyleSet<PointStyle>();
			pointStyleSet.setZoomStyle(minZoom, (PointStyle) style);
			return pointStyleSet;
		} else if (style instanceof LineStyle) {
			StyleSet<LineStyle> lineStyleSet = new StyleSet<LineStyle>();
			lineStyleSet.setZoomStyle(minZoom, (LineStyle) style);
			return lineStyleSet;
		} else if (style instanceof PolygonStyle) {
			StyleSet<PolygonStyle> polygonStyleSet = new StyleSet<PolygonStyle>();
			polygonStyleSet.setZoomStyle(minZoom, (PolygonStyle) style);
			return polygonStyleSet;
		} else {
			FLog.e("cannot create style set");
			return null;
		}
	}
	
	private void createLayer(){
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
	
	private void showFileBrowser(){
		Intent intent = new Intent((ShowProjectActivity)this.getContext(), FileChooserActivity.class);
		intent.putExtra(FileChooserActivity._Rootpath, (Parcelable) new LocalFile("/"));
		((ShowProjectActivity) this.getContext()).startActivityForResult(intent, ShowProjectActivity.MAP_FILE_BROWSER_REQUEST_CODE);
	}

	public FileManager getFileManager() {
		return fm;
	}

	public void setFileManager(FileManager fm) {
		this.fm = fm;
	}

	public void setSelectedFilePath(String filename) {
		this.selectedFileText.setText(filename);
	}
}
