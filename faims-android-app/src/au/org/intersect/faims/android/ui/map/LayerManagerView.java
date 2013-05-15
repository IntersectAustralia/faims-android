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

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Parcelable;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import au.org.intersect.faims.android.ui.activity.ShowProjectActivity;
import au.org.intersect.faims.android.ui.dialog.ErrorDialog;
import au.org.intersect.faims.android.ui.form.CustomDragDropListView;

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
	private Spinner tableNameSpinner;
	private int pointColor = 0xFFFF0000;
	private float pointSize = 0.2f;
	private float pointPickingSize = 0.4f;
	private int lineColor = 0xFF00FF00;
	private float lineSize = 0.2f;
	private float linePickingSize = 0.4f;
	private float lineWidth = 0.05f;
	private float linePickingWidth = 0.1f;
	private boolean lineShowPoints;
	private int polygonColor = 0xFF0000FF;
	private int polygonLineColor = 0XFF000000;
	private float polygonLineWidth = 0.05f;
	private float polygonLinePickingWidth = 0.1f;
	private boolean polygonShowStroke;
	private static final double VRT_MAXERROR = 0.125;
    private static final int VRT_RESAMPLER = gdalconst.GRA_NearestNeighbour;
	private static final String EPSG_3785_WKT = "PROJCS[\"Google Maps Global Mercator\",    GEOGCS[\"WGS 84\",        DATUM[\"WGS_1984\",            SPHEROID[\"WGS 84\",6378137,298.257223563,                AUTHORITY[\"EPSG\",\"7030\"]],            AUTHORITY[\"EPSG\",\"6326\"]],        PRIMEM[\"Greenwich\",0,            AUTHORITY[\"EPSG\",\"8901\"]],        UNIT[\"degree\",0.01745329251994328,            AUTHORITY[\"EPSG\",\"9122\"]],        AUTHORITY[\"EPSG\",\"4326\"]],    PROJECTION[\"Mercator_2SP\"],    PARAMETER[\"standard_parallel_1\",0],    PARAMETER[\"latitude_of_origin\",0],    PARAMETER[\"central_meridian\",0],    PARAMETER[\"false_easting\",0],    PARAMETER[\"false_northing\",0],    UNIT[\"Meter\",1],    EXTENSION[\"PROJ4\",\"+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs\"],    AUTHORITY[\"EPSG\",\"3785\"]]";

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
						double[][] boundaries = getBoundaries((CustomGdalMapLayer) mapView.getLayers().getBaseLayer());
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
		tableNameSpinner = new Spinner(this.getContext());
		layout.addView(tableNameSpinner);
		
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
		styleLayout.addView(createPolygonStyle());
		
		layout.addView(styleLayout);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					if(fm.getSelectedFile() != null){
						StyleSet<PointStyle> ps = (StyleSet<PointStyle>) createStyleSet(10, createPointStyle(pointColor, pointSize, pointPickingSize));
						PointStyle linePointStyle = lineShowPoints ? createPointStyle(lineColor, lineSize, linePickingSize) : null;
						StyleSet<LineStyle> ls = (StyleSet<LineStyle>) createStyleSet(10, createLineStyle(lineColor, lineWidth, linePickingWidth, linePointStyle));
						LineStyle polygonLineStyle = polygonShowStroke ? createLineStyle(polygonLineColor, polygonLineWidth, polygonLinePickingWidth, null) : null;
						StyleSet<PolygonStyle> pos = (StyleSet<PolygonStyle>) createStyleSet(10, createPolygonStyle(polygonColor, polygonLineStyle));
						String layerName = editText.getText() != null ? editText.getText().toString() : null;
						String tableName = tableNameSpinner.getSelectedItem() != null ? (String) tableNameSpinner.getSelectedItem() : null;
						mapView.addSpatialLayer(layerName, fm.getSelectedFile().getPath(), tableName, null, ps, ls, pos);
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
				
				LinearLayout layout = new LinearLayout(LayerManagerView.this.getContext());
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				
				final EditText colorSetter = addSetter(LayerManagerView.this.getContext(), layout, "Point Color:", Integer.toHexString(pointColor));
				final SeekBar sizeBar = addSlider(LayerManagerView.this.getContext(), layout, "Point Size:", pointSize);
				final SeekBar pickingSizeBar = addSlider(LayerManagerView.this.getContext(), layout, "Point Picking Size:", pointPickingSize);
				
				builder.setView(layout);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = parseColor(colorSetter.getText().toString());
							float size = parseSize(sizeBar.getProgress());
							float pickingSize = parseSize(pickingSizeBar.getProgress());
							
							LayerManagerView.this.pointColor = color;
							LayerManagerView.this.pointSize = size;
							LayerManagerView.this.pointPickingSize = pickingSize;
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
				
				LinearLayout layout = new LinearLayout(LayerManagerView.this.getContext());
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				
				final EditText colorSetter = addSetter(LayerManagerView.this.getContext(), layout, "Line Color:", Integer.toHexString(lineColor));
				final SeekBar sizeBar = addSlider(LayerManagerView.this.getContext(), layout, "Point Size:", lineSize);
				final SeekBar pickingSizeBar = addSlider(LayerManagerView.this.getContext(), layout, "Point Picking Size:", linePickingSize);
				final SeekBar widthBar = addSlider(LayerManagerView.this.getContext(), layout, "Line Width:", lineWidth);
				final SeekBar pickingWidthBar = addSlider(LayerManagerView.this.getContext(), layout, "Line Picking Width:", linePickingWidth);
				final CheckBox showPointsBox = addCheckBox(LayerManagerView.this.getContext(), layout, "Show Points on Line:", lineShowPoints);
				
				builder.setView(layout);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = parseColor(colorSetter.getText().toString());
							float size = parseSize(sizeBar.getProgress());
							float pickingSize = parseSize(pickingSizeBar.getProgress());
							float width = parseSize(widthBar.getProgress());
							float pickingWidth = parseSize(pickingWidthBar.getProgress());
							boolean showPoints = showPointsBox.isChecked();
							
							LayerManagerView.this.lineColor = color;
							LayerManagerView.this.lineSize = size;
							LayerManagerView.this.linePickingSize = pickingSize;
							LayerManagerView.this.lineWidth = width;
							LayerManagerView.this.linePickingWidth = pickingWidth;
							LayerManagerView.this.lineShowPoints = showPoints;
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

	public Button createPolygonStyle(){
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
				
				LinearLayout layout = new LinearLayout(LayerManagerView.this.getContext());
				layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				layout.setOrientation(LinearLayout.VERTICAL);
				
				final EditText colorSetter = addSetter(LayerManagerView.this.getContext(), layout, "Polygon Color:", Integer.toHexString(polygonColor));
				final EditText strokeColorSetter = addSetter(LayerManagerView.this.getContext(), layout, "Stroke Color:", Integer.toHexString(polygonLineColor));
				final SeekBar widthBar = addSlider(LayerManagerView.this.getContext(), layout, "Stroke Width:", polygonLineWidth);
				final SeekBar pickingWidthBar = addSlider(LayerManagerView.this.getContext(), layout, "Stroke Picking Width:", polygonLinePickingWidth);
				final CheckBox showStrokeBox = addCheckBox(LayerManagerView.this.getContext(), layout, "Show Stroke on Polygon:", polygonShowStroke);
				
				builder.setView(layout);
				
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							int color = parseColor(colorSetter.getText().toString());
							int lineColor = parseColor(strokeColorSetter.getText().toString());
							float width = parseSize(widthBar.getProgress());
							float pickingWidth = parseSize(pickingWidthBar.getProgress());
							boolean showStroke = showStrokeBox.isChecked();
							
							LayerManagerView.this.polygonColor = color;
							LayerManagerView.this.polygonLineColor = lineColor;
							LayerManagerView.this.polygonLineWidth = width;
							LayerManagerView.this.polygonLinePickingWidth = pickingWidth;
							LayerManagerView.this.polygonShowStroke = showStroke;
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
	
	protected float parseSize(int value) throws Exception {
		if (value < 0 || value > 100) {
			throw new MapException("Invalid size");
		}
		
		return ((float) value) / 100;
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

	protected EditText addSetter(Context context, LinearLayout layout, String labelText, String defaultValue) {
		return addSetter(context, layout, labelText, defaultValue, -1);
	}

	protected EditText addSetter(Context context, LinearLayout layout, String labelText, String defaultValue, int type) {
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
	
	private void showMetadata(Layer layer) {
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);

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

			double[][] originalBounds = getBoundaries(gdalMapLayer);
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
		builder.setView(layout);
		builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {

			}
	        
	    });
		
		builder.create().show();
	}

	private double[][] getBoundaries(CustomGdalMapLayer gdalMapLayer) {
		Dataset originalData = gdal.Open(gdalMapLayer.getGdalSource(), gdalconstConstants.GA_ReadOnly);
		// get original bounds in Wgs84
		SpatialReference hLatLong = new SpatialReference(osr.SRS_WKT_WGS84);
		SpatialReference layerProjection = new SpatialReference(EPSG_3785_WKT);
		Dataset openData = gdal.AutoCreateWarpedVRT(originalData,null, layerProjection.ExportToWkt(),VRT_RESAMPLER, VRT_MAXERROR);
		double[][] originalBounds = gdalMapLayer.boundsWgs84(openData, hLatLong);
		return originalBounds;
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
				setSpinner();
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
	
	public void setSpinner() throws jsqlite.Exception{
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
}
