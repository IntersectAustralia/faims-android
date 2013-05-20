package au.org.intersect.faims.android.ui.map;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import jsqlite.Database;
import jsqlite.Stmt;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Parcelable;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.exceptions.MapException;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.FileManager;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.CustomGdalMapLayer;
import au.org.intersect.faims.android.nutiteq.CustomSpatialiteLayer;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.nutiteq.GeometryTextStyle;
import au.org.intersect.faims.android.ui.activity.ShowProjectActivity;
import au.org.intersect.faims.android.ui.dialog.ErrorDialog;
import au.org.intersect.faims.android.ui.form.CustomDragDropListView;

import com.nutiteq.layers.Layer;

public class LayerManagerView extends LinearLayout {
	
	private static final int MAX_ZOOM = 18;
	
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
	private Spinner tableNameSpinner;
	private GeometryStyle pointStyle;
	private GeometryStyle lineStyle;
	private GeometryStyle polygonStyle;
	private GeometryTextStyle textStyle;

	public LayerManagerView(Context context) {
		super(context);
		
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		setOrientation(LinearLayout.VERTICAL);
		
		pointStyle = new GeometryStyle(12);
		lineStyle = new GeometryStyle(12);
		polygonStyle = new GeometryStyle(12);
		
		pointStyle.pointColor = 0xAAFF0000;
		pointStyle.size = 0.2f;
		pointStyle.pickingSize = 0.6f;
		
		lineStyle.pointColor = 0xAA00FF00;
		lineStyle.lineColor = 0xAA00FF00;
		lineStyle.size = 0.2f;
		lineStyle.pickingSize = 0.6f;
		lineStyle.width = 0.05f;
		lineStyle.pickingWidth = 0.3f;
		lineStyle.showPoints = false;
		
		polygonStyle.pointColor = 0xAA0000FF;
		polygonStyle.lineColor = 0xAA0000FF;
		polygonStyle.polygonColor = 0x440000FF;
		polygonStyle.size = 0.2f;
		polygonStyle.pickingSize = 0.6f;
		polygonStyle.width = 0.05f;
		polygonStyle.pickingWidth = 0.3f;
		polygonStyle.showStroke = true;
		polygonStyle.showPoints = false;
		
		textStyle = new GeometryTextStyle(12);
		textStyle.color = Color.WHITE;
		textStyle.size = 40;
		textStyle.font = Typeface.DEFAULT;
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
				List<Layer> layers = mapView.getAllLayers();
				int last = layers.size() - 1;
				final Layer layer = layers.get(last - position);
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
				List<Layer> layers = mapView.getAllLayers();
				int last = layers.size() - 1;
				final Layer layer = layers.get(last - position);
				
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
				
				Button showMetadataButton = new Button(context);
				showMetadataButton.setText("Show Metadata");
				showMetadataButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						d.dismiss();
						showMetadata(layer);
					}
				});

				layout.addView(removeButton);
				layout.addView(renameButton);
				layout.addView(showMetadataButton);
				
				d.show();
				return true;
			}
			
		});

		layout.addView(listView);
	}
	
	// Drop Listener
	private CustomDragDropListView.DropListener dropListener = new CustomDragDropListView.DropListener() {
		public void drop(int from, int to) {
			List<Layer> layers = mapView.getAllLayers();
			int last = layers.size() - 1;
			if(from != last && to != last){
				Collections.swap(layers, last - from, last - to);
				layers.remove(0);
				mapView.setAllLayers(layers);
				redrawLayers();
			}
		}
	};
	private Spinner labelColumnSpinner;

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
		List<Layer> layers = mapView.getAllLayers();
		List<Layer> shownLayer = new ArrayList<Layer>(layers);
		Collections.reverse(shownLayer);
		LayersAdapter layersAdapter = new LayersAdapter(shownLayer);
		listView.setAdapter(layersAdapter);
	}
	
	private void addLayer() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle("Add Layer");

		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		builder.setView(scrollView);
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
		
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		builder.setView(scrollView);
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
				showFileBrowser(ShowProjectActivity.RASTER_FILE_BROWSER_REQUEST_CODE);
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
						double[][] boundaries = ((CustomGdalMapLayer) mapView.getLayers().getBaseLayer()).getBoundaries();
						mapView.setMapFocusPoint(((float)boundaries[0][0]+(float)boundaries[3][0])/2, ((float)boundaries[0][1]+(float)boundaries[3][1])/2);
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
	
//	private void addShapeLayer(){
//		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
//		
//		builder.setTitle("Layer Manager");
//		builder.setMessage("Add shape layer:");
//		
//		LinearLayout layout = new LinearLayout(getContext());
//		layout.setOrientation(LinearLayout.VERTICAL);
//		
//		builder.setView(layout);
//		final Dialog d = builder.create();
//		
//		TextView textView = new TextView(this.getContext());
//		textView.setText("Shape layer name:");
//		layout.addView(textView);
//		final EditText editText = new EditText(LayerManagerView.this.getContext());
//		layout.addView(editText);
//		
//		Button browserButton = new Button(getContext());
//		browserButton.setText("browse");
//		browserButton.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View arg0) {
//				d.dismiss();
//				showFileBrowser(ShowProjectActivity.RASTER_FILE_BROWSER_REQUEST_CODE);
//			}
//		});
//		layout.addView(browserButton);
//		selectedFileText = new TextView(this.getContext());
//		layout.addView(selectedFileText);
//
//		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//
//			@SuppressWarnings("unchecked")
//			@Override
//			public void onClick(DialogInterface arg0, int arg1) {
//				try {
//					if(fm.getSelectedFile() != null){
//						StyleSet<PointStyle> ps = (StyleSet<PointStyle>) createStyleSet(10, createPointStyle(Color.RED, 0.05f, 0.1f));
//						StyleSet<LineStyle> ls = (StyleSet<LineStyle>) createStyleSet(10, createLineStyle(Color.GREEN, 0.01f, 0.01f, null));
//						StyleSet<PolygonStyle> pos = (StyleSet<PolygonStyle>) createStyleSet(10, createPolygonStyle(Color.BLUE, createLineStyle(Color.BLACK, 0.01f, 0.01f, null)));
//						mapView.addShapeLayer(editText.getText().toString(), fm.getSelectedFile().getPath(), ps, ls, pos);
//						fm.setSelectedFile(null);
//						redrawLayers();
//					}
//				} catch (Exception e) {
//					showErrorDialog(e.getMessage());
//				}
//			}
//	        
//	    });
//		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//	        public void onClick(DialogInterface dialog, int id) {
//	           // ignore
//	        }
//	    });
//		
//		builder.create().show();
//	}
	
	private void addSpatialLayer(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Add spatial layer:");
		
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		builder.setView(scrollView);
		final Dialog d = builder.create();
		
		TextView textView = new TextView(this.getContext());
		textView.setText("Spatial layer name:");
		layout.addView(textView);
		final EditText editText = new EditText(LayerManagerView.this.getContext());
		layout.addView(editText);
		
		TextView tableTextView = new TextView(this.getContext());
		tableTextView.setText("Spatial table name:");
		layout.addView(tableTextView);
		tableNameSpinner = new Spinner(this.getContext());
		tableNameSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int index,
					long arg3) {
				try {
					String tableName = (String) tableNameSpinner.getAdapter().getItem(index);
					setLabelSpinner(tableName);
				} catch (Exception e) {
					FLog.e("error getting table columns", e);
					showErrorDialog("Error getting table columns");
				}
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		layout.addView(tableNameSpinner);
		
		TextView labelTextView = new TextView(this.getContext());
		labelTextView.setText("Spatial label column:");
		layout.addView(labelTextView);
		labelColumnSpinner = new Spinner(this.getContext());
		
		layout.addView(labelColumnSpinner);
		
		Button browserButton = new Button(getContext());
		browserButton.setText("browse");
		browserButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				showFileBrowser(ShowProjectActivity.SPATIAL_FILE_BROWSER_REQUEST_CODE);
			}
		});
		layout.addView(browserButton);
		selectedFileText = new TextView(this.getContext());
		layout.addView(selectedFileText);
		
		LinearLayout styleLayout = new LinearLayout(this.getContext());
		styleLayout.setOrientation(LinearLayout.HORIZONTAL);
		styleLayout.addView(createPointStyleButton());
		styleLayout.addView(createLineStyleButton());
		styleLayout.addView(createPolygonStyleButton());
		styleLayout.addView(createTextStyleButton());
		
		layout.addView(styleLayout);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					if(fm.getSelectedFile() != null){
						String layerName = editText.getText() != null ? editText.getText().toString() : null;
						String tableName = tableNameSpinner.getSelectedItem() != null ? (String) tableNameSpinner.getSelectedItem() : null;
						String labelName = labelColumnSpinner.getSelectedItem() != null ? (String) labelColumnSpinner.getSelectedItem() : null;
						mapView.addSpatialLayer(layerName, fm.getSelectedFile().getPath(), tableName, new String[] { labelName }, 
								pointStyle.toPointStyleSet(), lineStyle.toLineStyleSet(), polygonStyle.toPolygonStyleSet(), textStyle.toStyleSet());
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
	
	public Button createPointStyleButton(){
		Button button = new Button(this.getContext());
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
		button.setText("Style Point");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
				builder.setTitle("Style Settings");
				
				ScrollView scrollView = new ScrollView(LayerManagerView.this.getContext());
				LinearLayout layout = new LinearLayout(LayerManagerView.this.getContext());
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				scrollView.addView(layout);
				
				final SeekBar zoomBar = addRange(LayerManagerView.this.getContext(), layout, "Min Zoom:", pointStyle.minZoom, MAX_ZOOM);
				final EditText colorSetter = addEdit(LayerManagerView.this.getContext(), layout, "Point Color:", Integer.toHexString(pointStyle.pointColor));
				final SeekBar sizeBar = addSlider(LayerManagerView.this.getContext(), layout, "Point Size:", pointStyle.size);
				final SeekBar pickingSizeBar = addSlider(LayerManagerView.this.getContext(), layout, "Point Picking Size:", pointStyle.pickingSize);
				
				builder.setView(scrollView);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int minZoom = parseRange(zoomBar.getProgress(), MAX_ZOOM);
							int color = parseColor(colorSetter.getText().toString());
							float size = parseSlider(sizeBar.getProgress());
							float pickingSize = parseSlider(pickingSizeBar.getProgress());
							
							LayerManagerView.this.pointStyle.minZoom = minZoom;
							LayerManagerView.this.pointStyle.pointColor = color;
							LayerManagerView.this.pointStyle.size = size;
							LayerManagerView.this.pointStyle.pickingSize = pickingSize;
						} catch (Exception e) {
							showErrorDialog(e.getMessage());
						}
					}
				});
				
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// ignore
					}
				});
				
				builder.create().show();
			}
				
		});
		return button;
	}
	
	public Button createLineStyleButton(){
		Button button = new Button(this.getContext());
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
		button.setText("Style Line");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
				builder.setTitle("Style Settings");
				
				ScrollView scrollView = new ScrollView(LayerManagerView.this.getContext());
				LinearLayout layout = new LinearLayout(LayerManagerView.this.getContext());
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				scrollView.addView(layout);
				
				final SeekBar zoomBar = addRange(LayerManagerView.this.getContext(), layout, "Min Zoom:", lineStyle.minZoom, MAX_ZOOM);
				final EditText colorSetter = addEdit(LayerManagerView.this.getContext(), layout, "Line Color:", Integer.toHexString(lineStyle.lineColor));
				final SeekBar sizeBar = addSlider(LayerManagerView.this.getContext(), layout, "Point Size:", lineStyle.size);
				final SeekBar pickingSizeBar = addSlider(LayerManagerView.this.getContext(), layout, "Point Picking Size:", lineStyle.pickingSize);
				final SeekBar widthBar = addSlider(LayerManagerView.this.getContext(), layout, "Line Width:", lineStyle.width);
				final SeekBar pickingWidthBar = addSlider(LayerManagerView.this.getContext(), layout, "Line Picking Width:", lineStyle.pickingWidth);
				final CheckBox showPointsBox = addCheckBox(LayerManagerView.this.getContext(), layout, "Show Points on Line:", lineStyle.showPoints);
				
				builder.setView(scrollView);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int minZoom = parseRange(zoomBar.getProgress(), MAX_ZOOM);
							int color = parseColor(colorSetter.getText().toString());
							float size = parseSlider(sizeBar.getProgress());
							float pickingSize = parseSlider(pickingSizeBar.getProgress());
							float width = parseSlider(widthBar.getProgress());
							float pickingWidth = parseSlider(pickingWidthBar.getProgress());
							boolean showPoints = showPointsBox.isChecked();
							
							LayerManagerView.this.lineStyle.minZoom = minZoom;
							LayerManagerView.this.lineStyle.pointColor = color;
							LayerManagerView.this.lineStyle.lineColor = color;
							LayerManagerView.this.lineStyle.size = size;
							LayerManagerView.this.lineStyle.pickingSize = pickingSize;
							LayerManagerView.this.lineStyle.width = width;
							LayerManagerView.this.lineStyle.pickingWidth = pickingWidth;
							LayerManagerView.this.lineStyle.showPoints = showPoints;
						} catch (Exception e) {
							showErrorDialog(e.getMessage());
						}
					}
				});
				
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// ignore
					}
				});
				
				builder.create().show();
			}
				
		});
		return button;
	}

	public Button createPolygonStyleButton(){
		Button button = new Button(this.getContext());
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
		button.setText("Style Polygon");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
				builder.setTitle("Style Settings");
				
				ScrollView scrollView = new ScrollView(LayerManagerView.this.getContext());
				LinearLayout layout = new LinearLayout(LayerManagerView.this.getContext());
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				scrollView.addView(layout);
				
				final SeekBar zoomBar = addRange(LayerManagerView.this.getContext(), layout, "Min Zoom:", polygonStyle.minZoom, MAX_ZOOM);
				final EditText colorSetter = addEdit(LayerManagerView.this.getContext(), layout, "Polygon Color:", Integer.toHexString(polygonStyle.polygonColor));
				final SeekBar sizeBar = addSlider(LayerManagerView.this.getContext(), layout, "Point Size:", polygonStyle.size);
				final SeekBar pickingSizeBar = addSlider(LayerManagerView.this.getContext(), layout, "Point Picking Size:", polygonStyle.pickingSize);
				final EditText strokeColorSetter = addEdit(LayerManagerView.this.getContext(), layout, "Stroke Color:", Integer.toHexString(polygonStyle.lineColor));
				final SeekBar widthBar = addSlider(LayerManagerView.this.getContext(), layout, "Stroke Width:", polygonStyle.width);
				final SeekBar pickingWidthBar = addSlider(LayerManagerView.this.getContext(), layout, "Stroke Picking Width:", polygonStyle.pickingWidth);
				final CheckBox showStrokeBox = addCheckBox(LayerManagerView.this.getContext(), layout, "Show Stroke on Polygon:", polygonStyle.showStroke);
				final CheckBox showPointsBox = addCheckBox(LayerManagerView.this.getContext(), layout, "Show Points on Polygon:", polygonStyle.showPoints);
				
				builder.setView(scrollView);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int minZoom = parseRange(zoomBar.getProgress(), MAX_ZOOM);
							float size = parseSlider(sizeBar.getProgress());
							float pickingSize = parseSlider(pickingSizeBar.getProgress());
							int color = parseColor(colorSetter.getText().toString());
							int lineColor = parseColor(strokeColorSetter.getText().toString());
							float width = parseSlider(widthBar.getProgress());
							float pickingWidth = parseSlider(pickingWidthBar.getProgress());
							boolean showStroke = showStrokeBox.isChecked();
							boolean showPoints = showPointsBox.isChecked();
							
							LayerManagerView.this.polygonStyle.minZoom = minZoom;
							LayerManagerView.this.polygonStyle.polygonColor = color;
							LayerManagerView.this.polygonStyle.pointColor = lineColor;
							LayerManagerView.this.polygonStyle.lineColor = lineColor;
							LayerManagerView.this.polygonStyle.size = size;
							LayerManagerView.this.polygonStyle.pickingSize = pickingSize;
							LayerManagerView.this.polygonStyle.width = width;
							LayerManagerView.this.polygonStyle.pickingWidth = pickingWidth;
							LayerManagerView.this.polygonStyle.showStroke = showStroke;
							LayerManagerView.this.polygonStyle.showPoints = showPoints;
						} catch (Exception e) {
							showErrorDialog(e.getMessage());
						}
					}
				});
				
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// ignore
					}
				});
				
				builder.create().show();
			}
				
		});
		return button;
	}
	
	public Button createTextStyleButton(){
		Button button = new Button(this.getContext());
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
		button.setText("Style Text");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
				builder.setTitle("Style Settings");
				
				ScrollView scrollView = new ScrollView(LayerManagerView.this.getContext());
				LinearLayout layout = new LinearLayout(LayerManagerView.this.getContext());
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				scrollView.addView(layout);
				
				final SeekBar zoomBar = addRange(LayerManagerView.this.getContext(), layout, "Min Zoom:", textStyle.minZoom, MAX_ZOOM);
				final EditText colorSetter = addEdit(LayerManagerView.this.getContext(), layout, "Text Color:", Integer.toHexString(textStyle.color));
				final SeekBar sizeBar = addRange(LayerManagerView.this.getContext(), layout, "Text Size:", textStyle.size, 100);
				
				builder.setView(scrollView);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int minZoom = parseRange(zoomBar.getProgress(), MAX_ZOOM);
							int color = parseColor(colorSetter.getText().toString());
							int size = parseRange(sizeBar.getProgress(), 100);
							
							LayerManagerView.this.textStyle.minZoom = minZoom;
							LayerManagerView.this.textStyle.color = color;
							LayerManagerView.this.textStyle.size = size;
						} catch (Exception e) {
							showErrorDialog(e.getMessage());
						}
					}
				});
				
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// ignore
					}
				});
				
				builder.create().show();
			}
				
		});
		return button;
	}

	protected int parseColor(String value) throws Exception {
		Integer color = (int) Long.parseLong(value, 16);
		if (color == null) {
			throw new MapException("Invalid color specified");
		}
		return color;
	}
	
	protected float parseSlider(int value) throws Exception {
		if (value < 0 || value > 100) {
			throw new MapException("Invalid size");
		}
		
		return ((float) value) / 100;
	}
	
	protected int parseRange(int value, int range) throws Exception {
		if (value < 0 || value > range) {
			throw new MapException("Invalid range");
		}
		
		return value;
	}

	protected CheckBox addCheckBox(Context context, LinearLayout layout, String labelText, boolean defaultValue) {
		TextView label = new TextView(context);
		label.setText(labelText);
		
		CheckBox box = new CheckBox(context);
		box.setChecked(defaultValue);
		
		layout.addView(label);
		layout.addView(box);
		
		return box;
	}

	protected EditText addEdit(Context context, LinearLayout layout, String labelText, String defaultValue) {
		return addEdit(context, layout, labelText, defaultValue, -1);
	}

	protected EditText addEdit(Context context, LinearLayout layout, String labelText, String defaultValue, int type) {
		TextView label = new TextView(context);
		label.setText(labelText);
		
		EditText text = new EditText(context);
		text.setText(defaultValue.toUpperCase(Locale.ENGLISH));
		
		if (type >= 0) text.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		
		layout.addView(label);
		layout.addView(text);
		
		return text;
	}

	protected SeekBar addSlider(Context context, LinearLayout layout, final String labelText, float defaultValue) {
		final TextView label = new TextView(context);
		label.setText(labelText + " " + Float.toString(defaultValue));
		
		final SeekBar seekBar = new SeekBar(context);
		seekBar.setMax(100);
		seekBar.setProgress((int) (defaultValue * 100));
		
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				label.setText(labelText + " " + Float.toString(((float) seekBar.getProgress()) / 100));
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		layout.addView(label);
		layout.addView(seekBar);
		
		return seekBar;
	}
	
	protected SeekBar addRange(Context context, LinearLayout layout, final String labelText, int defaultValue, final int range) {
		final TextView label = new TextView(context);
		label.setText(labelText + " " + Float.toString(defaultValue));
		
		final SeekBar seekBar = new SeekBar(context);
		seekBar.setMax(range);
		seekBar.setProgress(defaultValue);
		
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				label.setText(labelText + " " + Integer.toString(seekBar.getProgress()));
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		layout.addView(label);
		layout.addView(seekBar);
		
		return seekBar;
	}
	
	private void createLayer(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Enter layer name:");
		
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		builder.setView(scrollView);
		
		final EditText editText = new EditText(LayerManagerView.this.getContext());
		layout.addView(editText);
		
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
		if(layer instanceof CustomGdalMapLayer){
			CustomGdalMapLayer gdalMapLayer = (CustomGdalMapLayer) layer;
			editText.setText(gdalMapLayer.getName());
		}else if(layer instanceof CustomSpatialiteLayer){
			CustomSpatialiteLayer spatialiteLayer = (CustomSpatialiteLayer) layer;
			editText.setText(spatialiteLayer.getName());
		}else if(layer instanceof CanvasLayer){
			CanvasLayer canvasLayer = (CanvasLayer) layer;
			editText.setText(canvasLayer.getName());
		}
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
	
	private void showMetadata(Layer layer) {
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);

		TextView layerTypeTextView = new TextView(this.getContext());
		layerTypeTextView.setText("Layer type:");
		layout.addView(layerTypeTextView);

		EditText layerTypeEditText = new EditText(LayerManagerView.this.getContext());
		layerTypeEditText.setEnabled(false);
		if(layer instanceof CustomGdalMapLayer){
			layerTypeEditText.setText("raster layer");
		}else if(layer instanceof CustomSpatialiteLayer){
			layerTypeEditText.setText("spatial layer");
		}else if(layer instanceof CanvasLayer){
			layerTypeEditText.setText("canvas layer");
		}
		layout.addView(layerTypeEditText);
		
		TextView layerNameTextView = new TextView(this.getContext());
		layerNameTextView.setText("Layer name:");
		layout.addView(layerNameTextView);

		if(layer instanceof CustomGdalMapLayer){
			CustomGdalMapLayer gdalMapLayer = (CustomGdalMapLayer) layer;

			EditText layerNameEditText = new EditText(LayerManagerView.this.getContext());
			layerNameEditText.setEnabled(false);
			layerNameEditText.setText(gdalMapLayer.getName());
			layout.addView(layerNameEditText);

			TextView fileNameTextView = new TextView(this.getContext());
			fileNameTextView.setText("File name:");
			layout.addView(fileNameTextView);

			File file = new File(gdalMapLayer.getGdalSource());
			EditText fileNameEditText = new EditText(LayerManagerView.this.getContext());
			fileNameEditText.setEnabled(false);
			fileNameEditText.setText(file.getName());
			layout.addView(fileNameEditText);

			TextView fileSizeTextView = new TextView(this.getContext());
			fileSizeTextView.setText("File size:");
			layout.addView(fileSizeTextView);

			EditText fileSizeEditText = new EditText(LayerManagerView.this.getContext());
			fileSizeEditText.setEnabled(false);
			fileSizeEditText.setText(file.length()/(1024 * 1024) + " MB");
			layout.addView(fileSizeEditText);

			double[][] originalBounds = gdalMapLayer.getBoundaries();
	        TextView upperLeftTextView = new TextView(this.getContext());
	        upperLeftTextView.setText("Upper left boundary:");
			layout.addView(upperLeftTextView);

			EditText upperLeftEditText = new EditText(LayerManagerView.this.getContext());
			upperLeftEditText.setEnabled(false);
			upperLeftEditText.setText(originalBounds[0][0] + "," + originalBounds[0][1]);
			layout.addView(upperLeftEditText);

			TextView bottomRightTextView = new TextView(this.getContext());
			bottomRightTextView.setText("Bottom right boundary:");
			layout.addView(bottomRightTextView);

			EditText bottomRightEditText = new EditText(LayerManagerView.this.getContext());
			bottomRightEditText.setEnabled(false);
			bottomRightEditText.setText(originalBounds[3][0] + "," + originalBounds[3][1]);
			layout.addView(bottomRightEditText);

		}else if(layer instanceof CustomSpatialiteLayer){
			CustomSpatialiteLayer spatialiteLayer = (CustomSpatialiteLayer) layer;

			EditText layerNameEditText = new EditText(LayerManagerView.this.getContext());
			layerNameEditText.setEnabled(false);
			layerNameEditText.setText(spatialiteLayer.getName());
			layout.addView(layerNameEditText);

			TextView fileNameTextView = new TextView(this.getContext());
			fileNameTextView.setText("File name:");
			layout.addView(fileNameTextView);

			File file = new File(spatialiteLayer.getDbPath());
			EditText fileNameEditText = new EditText(LayerManagerView.this.getContext());
			fileNameEditText.setEnabled(false);
			fileNameEditText.setText(file.getName());
			layout.addView(fileNameEditText);

			TextView fileSizeTextView = new TextView(this.getContext());
			fileSizeTextView.setText("File size:");
			layout.addView(fileSizeTextView);

			EditText fileSizeEditText = new EditText(LayerManagerView.this.getContext());
			fileSizeEditText.setEnabled(false);
			fileSizeEditText.setText(file.length()/(1024 * 1024) + " MB");
			layout.addView(fileSizeEditText);

			TextView tableNameTextView = new TextView(this.getContext());
			tableNameTextView.setText("Table name:");
			layout.addView(tableNameTextView);

			EditText tableNameEditText = new EditText(LayerManagerView.this.getContext());
			tableNameEditText.setEnabled(false);
			tableNameEditText.setText(spatialiteLayer.getTableName());
			layout.addView(tableNameEditText);

		}else if(layer instanceof CanvasLayer){
			CanvasLayer canvasLayer = (CanvasLayer) layer;

			EditText layerNameEditText = new EditText(LayerManagerView.this.getContext());
			layerNameEditText.setEnabled(false);
			layerNameEditText.setText(canvasLayer.getName());
			layout.addView(layerNameEditText);
		}else{
			showErrorDialog("wrong type of layer");
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerView.this.getContext());
		builder.setTitle("Layer Metadata");
		builder.setView(scrollView);
		builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {

			}
	        
	    });
		
		builder.create().show();
	}

	private void showErrorDialog(String message) {
		new ErrorDialog(LayerManagerView.this.getContext(), "Layer Manager Error", message).show();
	}
	
	private void showFileBrowser(int requestCode){
		Intent intent = new Intent((ShowProjectActivity)this.getContext(), FileChooserActivity.class);
		intent.putExtra(FileChooserActivity._Rootpath, (Parcelable) new LocalFile("/"));
		((ShowProjectActivity) this.getContext()).startActivityForResult(intent, requestCode);
	}

	public FileManager getFileManager() {
		return fm;
	}

	public void setFileManager(FileManager fm) {
		this.fm = fm;
	}

	public void setSelectedFilePath(String filename, boolean isSpatial) {
		if(isSpatial){
			try {
				setTableSpinner();
			} catch (jsqlite.Exception e) {
				FLog.e("Not a valid spatial layer file");
				AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
				
				builder.setTitle("Error");
				builder.setMessage("Not a valid spatial layer file");
				builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				               // User clicked OK button
				           }
				       });
				builder.create().show();
			}
		}
		this.selectedFileText.setText(filename);
	}
	
	public void setTableSpinner() throws jsqlite.Exception{
			synchronized(DatabaseManager.class) {
				List<String> tableName = new ArrayList<String>();
				Stmt st = null;
				Database db = null;
				try {
					db = new jsqlite.Database();
					db.open(this.fm.getSelectedFile().getPath(), jsqlite.Constants.SQLITE_OPEN_READWRITE);
					
					String query = "select name from sqlite_master where type = 'table' and sql like '%\"Geometry\"%';";
					st = db.prepare(query);
					
					while(st.step()){
						tableName.add(st.column_string(0));
					}
					st.close();
					st = null;
				} finally {
					try {
						if (st != null) st.close();
					} catch(Exception e) {
						FLog.e("error closing statement", e);
					}
					try {
						if (db != null) {
							db.close();
							db = null;
						}
					} catch (Exception e) {
						FLog.e("error closing database", e);
					}
				}
				if(tableName.isEmpty()){
					throw new jsqlite.Exception("Not tables found");
				}else{
					ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
							this.getContext(),
							android.R.layout.simple_spinner_dropdown_item,
							tableName);
					tableNameSpinner.setAdapter(arrayAdapter);
					tableNameSpinner.setSelection(0);
				}
			}
	}
	
	public void setLabelSpinner(String tableName) throws jsqlite.Exception{
		synchronized(DatabaseManager.class) {
			List<String> columnNames = new ArrayList<String>();
			Stmt st = null;
			Database db = null;
			try {
				db = new jsqlite.Database();
				db.open(this.fm.getSelectedFile().getPath(), jsqlite.Constants.SQLITE_OPEN_READWRITE);
				
				String query = "pragma table_info(" + tableName + ")";
				st = db.prepare(query);
				
				while(st.step()){
					columnNames.add(st.column_string(1));
				}
				
				st.close();
				st = null;
			} finally {
				try {
					if (st != null) st.close();
				} catch(Exception e) {
					FLog.e("error closing statement", e);
				}
				try {
					if (db != null) {
						db.close();
						db = null;
					}
				} catch (Exception e) {
					FLog.e("error closing database", e);
				}
			}
			if(columnNames.isEmpty()){
				throw new jsqlite.Exception("Not labels found");
			}else{
				ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
						this.getContext(),
						android.R.layout.simple_spinner_dropdown_item,
						columnNames);
				labelColumnSpinner.setAdapter(arrayAdapter);
				labelColumnSpinner.setSelection(0);
			}
		}
}
}
