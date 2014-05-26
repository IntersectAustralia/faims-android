package au.org.intersect.faims.android.ui.map;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jsqlite.Database;
import jsqlite.Stmt;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.User;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.exceptions.MapException;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.FileManager;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.CustomGdalMapLayer;
import au.org.intersect.faims.android.nutiteq.CustomSpatialiteLayer;
import au.org.intersect.faims.android.nutiteq.DatabaseLayer;
import au.org.intersect.faims.android.nutiteq.GeometryStyle;
import au.org.intersect.faims.android.nutiteq.GeometryTextStyle;
import au.org.intersect.faims.android.nutiteq.TrackLogDatabaseLayer;
import au.org.intersect.faims.android.ui.activity.ShowModuleActivity;
import au.org.intersect.faims.android.ui.dialog.ErrorDialog;
import au.org.intersect.faims.android.ui.dialog.LineStyleDialog;
import au.org.intersect.faims.android.ui.dialog.PointStyleDialog;
import au.org.intersect.faims.android.ui.dialog.PolygonStyleDialog;
import au.org.intersect.faims.android.ui.dialog.TextStyleDialog;
import au.org.intersect.faims.android.ui.view.CustomDragDropListView;
import au.org.intersect.faims.android.ui.view.CustomListView;
import au.org.intersect.faims.android.util.GeometryUtil;

import com.google.inject.Inject;
import com.nutiteq.components.MapPos;
import com.nutiteq.layers.Layer;

public class LayerManagerDialog extends AlertDialog {
	
	private class LayersAdapter extends BaseAdapter {
		
		private List<Layer> layers;
		private ArrayList<View> itemViews;

		public LayersAdapter(List<Layer> layers) {
			this.layers = layers;
			this.itemViews = new ArrayList<View>();
			
			for (Layer layer : layers) {
				LayerListItem item = new LayerListItem(LayerManagerDialog.this.getContext());
				item.init(listView,layer,LayerManagerDialog.this.mapView);
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
		public View getView(int position, View view, ViewGroup arg2) {
			LayerListItem item = (LayerListItem) itemViews.get(position);
			if(LayerManagerDialog.this.mapView.getSelectedLayer() != null){
				if(LayerManagerDialog.this.mapView.getSelectedLayer() .equals(item.getLayer())){
					for(View itemView : itemViews){
						LayerListItem layerItem = (LayerListItem) itemView;
						layerItem.setItemSelected(false);
					}
					item.setItemSelected(true);
				}
			}else{
				for(View itemView : itemViews){
					LayerListItem layerItem = (LayerListItem) itemView;
					layerItem.setItemSelected(false);
				}
			}
			return item;
		}
		
		
	}

	private class UsersListAdapter extends BaseAdapter {
		
		private Map<User, Boolean> users;
		private ArrayList<View> itemViews;

		public UsersListAdapter(Map<User, Boolean> users) {
			this.users = users;
			this.itemViews = new ArrayList<View>();
			
			for (Entry<User, Boolean> user : users.entrySet()) {
				UserListItem item = new UserListItem(LayerManagerDialog.this.getContext());
				item.init(user.getKey(), user.getValue());
				itemViews.add(item);
			} 
		}
		
		@Override
		public int getCount() {
			return users.size();
		}

		@Override
		public Object getItem(int position) {
			return users.keySet().toArray()[position];
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

	@Inject
	DatabaseManager databaseManager;

	private CustomMapView mapView;
	private LinearLayout layout;
	private LinearLayout buttonsLayout;
	private CustomDragDropListView listView;
	private TextView selectedFileText;
	private Spinner tableNameSpinner;
	private Spinner labelColumnSpinner;
	private Spinner idColumnSpinner;
	protected File rasterFile;
	protected File spatialFile;
	protected GeometryStyle pointStyle;
	protected GeometryStyle lineStyle;
	protected GeometryStyle polygonStyle;
	protected GeometryTextStyle textStyle;
	protected PolygonStyleDialog polygonStyleDialog;
	protected LineStyleDialog lineStyleDialog;
	protected PointStyleDialog pointStyleDialog;
	protected TextStyleDialog textStyleDialog;

	public LayerManagerDialog(Context context) {
		super(context);
		
		FAIMSApplication.getInstance().injectMembers(this);
		
		pointStyle = GeometryStyle.defaultPointStyle();
		lineStyle = GeometryStyle.defaultLineStyle();
		polygonStyle = GeometryStyle.defaultPolygonStyle();
		textStyle = GeometryTextStyle.defaultStyle();
	}
	
	public void attachToMap(CustomMapView mapView) {
		this.mapView = mapView;
		
		ShowModuleActivity activity = mapView.getActivity();
		activity.getFileManager().addListener(ShowModuleActivity.RASTER_FILE_BROWSER_REQUEST_CODE, new FileManager.FileManagerListener() {
			@Override
	           public void onFileSelected(File file) {
					LayerManagerDialog.this.rasterFile = file;
					LayerManagerDialog.this.selectedFileText.setText(file.getName());
	           }
		});
		activity.getFileManager().addListener(ShowModuleActivity.SPATIAL_FILE_BROWSER_REQUEST_CODE, new FileManager.FileManagerListener() {
			
			@Override
			public void onFileSelected(File file) {
				LayerManagerDialog.this.spatialFile = file;
				LayerManagerDialog.this.selectedFileText.setText(file.getName());
				try {
					setTableSpinner();
				} catch (jsqlite.Exception e) {
					FLog.e("Not a valid spatial layer file", e);
					showErrorDialog("Not a valid spatial layer file");
				}
			}
		});
		
		layout = new LinearLayout(this.getContext());
		layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		layout.setOrientation(LinearLayout.VERTICAL);
		this.setView(layout);
		
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
		listView.setDivider(new ColorDrawable(Color.WHITE));
		listView.setDividerHeight(1);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position,
					long arg3) {
				List<Layer> layers = mapView.getAllLayers();
				int last = layers.size() - 1;
				final Layer layer = layers.get(last - position);
				mapView.setSelectedLayer(layer);
				mapView.updateLayers();
				((LayersAdapter)listView.getAdapter()).getView(position, view, listView);
			}
			
		});
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				List<Layer> layers = mapView.getAllLayers();
				int last = layers.size() - 1;
				if (last - position < 0 || last - position >= layers.size()) {
					FLog.e("Error on layer long click");
					return true;
				}
				
				final Layer layer = layers.get(last - position);
				
				Context context = LayerManagerDialog.this.getContext();
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Layer Options");

				LinearLayout layout = new LinearLayout(context);
				layout.setOrientation(LinearLayout.VERTICAL);
				
				builder.setView(layout);
				builder.setPositiveButton("Done", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// dismiss dialog
					}
				});

				final Dialog d = builder.create();
				
				Button removeButton = new Button(context);
				removeButton.setText("Remove");
				removeButton.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View arg0) {
						d.dismiss();
						removeLayer(layer);
					}
					
				});
				
				Button renameButton = new Button(context);
				renameButton.setText("Rename");
				renameButton.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View arg0) {
						d.dismiss();
						renameLayer(layer);
					}
					
				});
				
				Button showMetadataButton = new Button(context);
				showMetadataButton.setText("Show Metadata");
				showMetadataButton.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View arg0) {
						d.dismiss();
						showMetadata(layer);
					}
				});
				
				layout.addView(removeButton);
				layout.addView(renameButton);
				layout.addView(showMetadataButton);
				
				if (layer instanceof CustomGdalMapLayer){
					CheckBox checkBox = new CheckBox(context);
					checkBox.setText("Experimental Fast Raster Loading");
					checkBox.setChecked(!((CustomGdalMapLayer)layer).isShowAlways());
					checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							((CustomGdalMapLayer)layer).setShowAlways(!isChecked);
						}
					});
					layout.addView(checkBox);

				}else if (layer instanceof CustomSpatialiteLayer) {
					final ToggleButton showLabelsButton = new ToggleButton(context);
					showLabelsButton.setTextOn("Hide Labels");
					showLabelsButton.setTextOff("Show Labels");
					showLabelsButton.setChecked(((CustomSpatialiteLayer) layer).getTextVisible());
					showLabelsButton.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View arg0) {
							((CustomSpatialiteLayer) layer).setTextVisible(showLabelsButton.isChecked());
						}
					});
					layout.addView(showLabelsButton);
					
					Button configButton = new Button(context);
					configButton.setText("Config");
					configButton.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View arg0) {
							d.dismiss();
							config(layer);
						}
					});
					layout.addView(configButton);
				} else if (layer instanceof DatabaseLayer) {
					final ToggleButton showLabelsButton = new ToggleButton(context);
					showLabelsButton.setTextOn("Hide Labels");
					showLabelsButton.setTextOff("Show Labels");
					showLabelsButton.setChecked(((DatabaseLayer) layer).getTextVisible());
					showLabelsButton.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View arg0) {
							((DatabaseLayer) layer).setTextVisible(showLabelsButton.isChecked());
						}
					});
					layout.addView(showLabelsButton);
					if(layer instanceof TrackLogDatabaseLayer){
						Button userTrackLogButton = new Button(context);
						userTrackLogButton.setText("Show/hide user's track log");
						userTrackLogButton.setOnClickListener(new View.OnClickListener() {

							@Override
							public void onClick(View arg0) {
								try {
									showOrHideUserListView();
								} catch (Exception e) {
									FLog.e("error creating user list view", e);
								}
							}

						});
						layout.addView(userTrackLogButton);
					}
					Button configButton = new Button(context);
					configButton.setText("Config");
					configButton.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View arg0) {
							d.dismiss();
							config(layer);
						}
					});
					layout.addView(configButton);
				}
				d.setCanceledOnTouchOutside(true);
				d.show();
				return true;
			}
			
		});

		layout.addView(listView);
	}

	private void createAddButton() {
		Button addButton = new Button(this.getContext());
		addButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		addButton.setText("Add");
		addButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				addLayer();
			}
			
		});
		
		buttonsLayout.addView(addButton);
	}
	
	private void createOrderButton(){
		ToggleButton orderButton = new ToggleButton(this.getContext());
		orderButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
		orderButton.setTextOn("Re-Order");
		orderButton.setTextOff("Re-Order");
		orderButton.setChecked(false);
		orderButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					listView.setDropListener(new CustomDragDropListView.DropListener() {
						public void drop(int from, int to) {
							List<Layer> layers = mapView.getAllLayers();
							int last = layers.size() - 1;
							boolean isFromBaseLayer = layers.get(last - from) == mapView.getLayers().getBaseLayer();
							boolean isToBaseLayer = layers.get(last - to) == mapView.getLayers().getBaseLayer();
							if(!(isFromBaseLayer || isToBaseLayer)){
								Collections.swap(layers, last - from, last - to);
								mapView.setAllLayers(layers);
								redrawLayers();
							}
						}
					});
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
		d.setCanceledOnTouchOutside(true);
		
		Button loadBaseLayerButton = new Button(getContext());
		loadBaseLayerButton.setText("Load Base Layer");
		loadBaseLayerButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				addBaseLayer();
			}
			
		});
		
		Button loadRasterLayerButton = new Button(getContext());
		loadRasterLayerButton.setText("Load Raster Layer");
		loadRasterLayerButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				addRasterLayer();
			}
			
		});
		
		// TODO Fix the bug with loading shape layer
//		Button loadShapeLayerButton = new Button(getContext());
//		loadShapeLayerButton.setText("Load Shape Layer");
//		loadShapeLayerButton.setOnClickListener(new View.OnClickListener() {
//
//			@Override
//			public void onClick(View arg0) {
//				d.dismiss();
//				addShapeLayer();
//			}
//			
//		});
		
		Button loadSpatialLayerButton = new Button(getContext());
		loadSpatialLayerButton.setText("Load Vector Layer");
		loadSpatialLayerButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				addSpatialLayer();
			}
			
		});
		
		Button loadDatabaseLayerButton = new Button(getContext());
		loadDatabaseLayerButton.setText("Load Database Layer");
		loadDatabaseLayerButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				addDatabaseLayer();
			}
			
		});
		
		Button createLayerButton = new Button(getContext());
		createLayerButton.setText("Create Canvas Layer");
		createLayerButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				d.dismiss();
				createLayer();
			}
			
		});
		
		Button viewTrackLogButton = new Button(this.getContext());
		viewTrackLogButton.setText("View Track Log");
		viewTrackLogButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					d.dismiss();
					if(mapView.getUserTrackLogLayer() == null){
						addViewTrackLogLayer();
					}else{
						mapView.setLayerVisible(mapView.getUserTrackLogLayer(), true);
						redrawLayers();
					}
				} catch (Exception e) {
					FLog.e("can not view track log layer",e);
				}
			}
			
		});
		
		layout.addView(loadBaseLayerButton);
		layout.addView(loadRasterLayerButton);
		layout.addView(loadSpatialLayerButton);
		layout.addView(loadDatabaseLayerButton);
		layout.addView(createLayerButton);
		layout.addView(viewTrackLogButton);
		
		d.show();
	}
	
	private void addBaseLayer(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerDialog.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Add base layer:");
		
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		builder.setView(scrollView);
		
		TextView textView = new TextView(this.getContext());
		textView.setText("Base layer name:");
		layout.addView(textView);
		final EditText editText = new EditText(LayerManagerDialog.this.getContext());
		layout.addView(editText);
		
		Button browserButton = new Button(getContext());
		browserButton.setText("browse");
		browserButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				showFileBrowser(ShowModuleActivity.RASTER_FILE_BROWSER_REQUEST_CODE);
			}
		});
		layout.addView(browserButton);
		selectedFileText = new TextView(this.getContext());
		layout.addView(selectedFileText);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// ignore
			}
	        
	    });
		
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		rasterFile = null;
		
		final AlertDialog d = builder.create();
		d.setCanceledOnTouchOutside(true);
		d.show();
		d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					String layerName = editText.getText().toString();
					
					if (layerName == null || "".equals(layerName)) {
						showErrorDialog("Please enter a layer name.");
					} else if (rasterFile == null) {
						showErrorDialog("Please select a raster file.");
					} else {
						mapView.addBaseMap(layerName, rasterFile.getPath());
						redrawLayers();
						d.dismiss();
					}
				} catch (MapException e) {
					FLog.e(e.getMessage(), e);
					showErrorDialog(e.getMessage());
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
					showErrorDialog("Could not load raster layer.");
				}
			}
			
		});
	}
	
	private void addRasterLayer(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerDialog.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Add raster layer:");
		
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		builder.setView(scrollView);
		
		TextView textView = new TextView(this.getContext());
		textView.setText("Raster layer name:");
		layout.addView(textView);
		final EditText editText = new EditText(LayerManagerDialog.this.getContext());
		layout.addView(editText);
		
		Button browserButton = new Button(getContext());
		browserButton.setText("browse");
		browserButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				showFileBrowser(ShowModuleActivity.RASTER_FILE_BROWSER_REQUEST_CODE);
			}
		});
		layout.addView(browserButton);
		selectedFileText = new TextView(this.getContext());
		layout.addView(selectedFileText);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// ignore
			}
	        
	    });
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		rasterFile = null;
		
		final AlertDialog d = builder.create();
		d.setCanceledOnTouchOutside(true);
		d.show();
		
		d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					String layerName = editText.getText().toString();
					
					if (layerName == null || "".equals(layerName)) {
						showErrorDialog("Please enter a layer name.");
					} else if (rasterFile == null) {
						showErrorDialog("Please select a raster file.");
					} else {
						mapView.addRasterMap(layerName, rasterFile.getPath());
						redrawLayers();
						d.dismiss();
					}
				} catch (MapException e) {
					FLog.e(e.getMessage(), e);
					showErrorDialog(e.getMessage());
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
					showErrorDialog("Could not load raster layer.");
				}
			}
			
		});
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
//		
//		TextView textView = new TextView(this.getContext());
//		textView.setText("Shape layer name:");
//		layout.addView(textView);
//		final EditText editText = new EditText(LayerManagerView.this.getContext());
//		layout.addView(editText);
//		
//		Button browserButton = new Button(getContext());
//		browserButton.setText("browse");
//		browserButton.setOnClickListener(new View.OnClickListener() {
//
//			@Override
//			public void onClick(View arg0) {
//				showFileBrowser(ShowModuleActivity.RASTER_FILE_BROWSER_REQUEST_CODE);
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
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerDialog.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Add spatial layer:");
		
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		builder.setView(scrollView);
		
		TextView textView = new TextView(this.getContext());
		textView.setText("Spatial layer name:");
		layout.addView(textView);
		final EditText editText = new EditText(LayerManagerDialog.this.getContext());
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
					setTableColumnSpinners(tableName);
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
		
		TextView idTextView = new TextView(this.getContext());
		idTextView.setText("Spatial id column:");
		layout.addView(idTextView);
		idColumnSpinner = new Spinner(this.getContext());
		
		layout.addView(idColumnSpinner);
		
		TextView labelTextView = new TextView(this.getContext());
		labelTextView.setText("Spatial label column:");
		layout.addView(labelTextView);
		labelColumnSpinner = new Spinner(this.getContext());
		
		layout.addView(labelColumnSpinner);
		
		Button browserButton = new Button(getContext());
		browserButton.setText("browse");
		browserButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				showFileBrowser(ShowModuleActivity.SPATIAL_FILE_BROWSER_REQUEST_CODE);
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
				// ignore
			}
	        
	    });
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		spatialFile = null;
		
		final AlertDialog d = builder.create();
		d.setCanceledOnTouchOutside(true);
		d.show();
		
		d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					String layerName = editText.getText() != null ? editText.getText().toString() : null;
					String tableName = tableNameSpinner.getSelectedItem() != null ? (String) tableNameSpinner.getSelectedItem() : null;
					String idColumn = idColumnSpinner.getSelectedItem() != null ? (String) idColumnSpinner.getSelectedItem() : null;
					String labelColumn = labelColumnSpinner.getSelectedItem() != null ? (String) labelColumnSpinner.getSelectedItem() : null;
					
					if (layerName == null || "".equals(layerName)) {
						showErrorDialog("Please enter a layer name.");
					} else if (spatialFile == null) {
						showErrorDialog("Please select a spatialite db file.");
					} else if (tableName == null || "".equals(tableName)) {
						showErrorDialog("Please select a table.");
					} else if (idColumn == null || "".equals(idColumn)) {
						showErrorDialog("Please select a id column.");
					} else if (labelColumn == null || "".equals(labelColumn)) {
						showErrorDialog("Please select a label column.");
					} else {
						mapView.addSpatialLayer(layerName, spatialFile.getPath(), tableName, idColumn, labelColumn, 
								pointStyle, lineStyle, polygonStyle, 
								textStyle.toStyleSet());
						redrawLayers();
						d.dismiss();
					}
				} catch (MapException e) {
					FLog.e(e.getMessage(), e);
					showErrorDialog(e.getMessage());
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
					showErrorDialog("Could not load vector layer.");
				}
			}
			
		});
	}
	
	private void addDatabaseLayer(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerDialog.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Add database layer:");
		
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		builder.setView(scrollView);
		
		TextView textView = new TextView(this.getContext());
		textView.setText("Database layer name:");
		layout.addView(textView);
		final EditText editText = new EditText(LayerManagerDialog.this.getContext());
		layout.addView(editText);
		
		TextView typeTextView = new TextView(this.getContext());
		typeTextView.setText("Database layer type:");
		layout.addView(typeTextView);
		final Spinner typeSpinner = new Spinner(this.getContext());
		ArrayList<String> types = new ArrayList<String>();
		types.add("Entity");
		types.add("Relationship");
		typeSpinner.setAdapter(new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_dropdown_item, types));
		layout.addView(typeSpinner);
		
		TextView queryTextView = new TextView(this.getContext());
		queryTextView.setText("Database query:");
		layout.addView(queryTextView);
		final Spinner querySpinner = new Spinner(this.getContext());
		List<String> queryNames = mapView.getDatabaseLayerQueryNames();
		querySpinner.setAdapter(new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_dropdown_item, queryNames));
		layout.addView(querySpinner);
		
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
				// ignore
			}
	        
	    });
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		final AlertDialog d = builder.create();
		d.setCanceledOnTouchOutside(true);
		d.show();
		
		d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					String layerName = editText.getText() != null ? editText.getText().toString() : null;
					String type = typeSpinner.getSelectedItem() != null ? (String) typeSpinner.getSelectedItem() : null;
					String query = querySpinner.getSelectedItem() != null ? (String) querySpinner.getSelectedItem() : null;
					
					if (layerName == null || "".equals(layerName)) {
						showErrorDialog("Please enter a layer name.");
					} else if (type == null || "".equals(type)) {
						showErrorDialog("Please select a type.");
					} else {
						mapView.addDatabaseLayer(layerName, "Entity".equals(type), query, mapView.getDatabaseLayerQuery(query), 
								pointStyle, lineStyle, polygonStyle, 
								textStyle.toStyleSet());
						redrawLayers();
						d.dismiss();
					}
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
					showErrorDialog("Could not load database layer.");
				}
			}
			
		});
	}
	
	protected void addViewTrackLogLayer() throws Exception {
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerDialog.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Add track log layer:");
		
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		builder.setView(scrollView);
		
		TextView textView = new TextView(this.getContext());
		textView.setText("Track log layer name:");
		layout.addView(textView);
		final EditText editText = new EditText(LayerManagerDialog.this.getContext());
		layout.addView(editText);
		layout.addView(createUserSelectionButton());
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// ignore
			}
	        
	    });
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		final AlertDialog d = builder.create();
		d.setCanceledOnTouchOutside(true);
		d.show();
		
		d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					String layerName = editText.getText() != null ? editText.getText().toString() : null;
					
					if (layerName == null || "".equals(layerName)) {
						showErrorDialog("Please enter a layer name.");
					} else {
						createUsersTrackLogLayer(layerName);
						redrawLayers();
						d.dismiss();
					}
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
					showErrorDialog("Could not load track log layer.");
				}
			}
			
		});
	}

	private void createUsersTrackLogLayer(String layerName) throws Exception {
		mapView.setUserTrackLogLayer(mapView.addDataBaseLayerForTrackLog(layerName, mapView.getUserCheckedList(), mapView.getTrackLogQueryName(), mapView.getTrackLogQuerySql(),
				pointStyle, lineStyle, polygonStyle, textStyle.toStyleSet()));
	}

	private View createUserSelectionButton() throws Exception {
		Button button = new Button(this.getContext());
		button.setText("Show/hide user's track log");
		List<User> users = databaseManager.fetchRecord().fetchAllUser();
		if(mapView.getUserCheckedList().isEmpty()){
			for(User user : users){
				mapView.putUserCheckList(user, true);
			}
		}
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					createUserListView();
				} catch (Exception e) {
					FLog.e("error creating user list view", e);
				}
			}
				
		});
		return button;
	}

	private void createUserListView(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerDialog.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Show/hide user track log:");
		
		CustomListView listView = new CustomListView(this.getContext());
		UsersListAdapter adapter = new UsersListAdapter(mapView.getUserCheckedList());
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int arg2,
					long arg3) {
				UserListItem itemView = (UserListItem) view;
				itemView.toggle();
				if(itemView.isChecked()){
					mapView.putUserCheckList(itemView.getUser(),true);
				}else{
					mapView.putUserCheckList(itemView.getUser(),false);
				}
			}
		});

		builder.setView(listView);
		
		builder.setNeutralButton("Done", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// ignore
			}
	        
	    });
		AlertDialog d = builder.create();
		d.setCanceledOnTouchOutside(true);
		d.show();
	}
	
	private void showOrHideUserListView() {
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerDialog.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Show/hide user track log:");
		
		CustomListView listView = new CustomListView(this.getContext());
		UsersListAdapter adapter = new UsersListAdapter(mapView.getUserCheckedList());
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int arg2,
					long arg3) {
				UserListItem itemView = (UserListItem) view;
				itemView.toggle();
				TrackLogDatabaseLayer trackLogDatabaseLayer = (TrackLogDatabaseLayer) mapView.getLayer(mapView.getUserTrackLogLayer());
				if(itemView.isChecked()){
					mapView.putUserCheckList(itemView.getUser(),true);
					trackLogDatabaseLayer.toggleUser(itemView.getUser(), true);
				}else{
					mapView.putUserCheckList(itemView.getUser(),false);
					trackLogDatabaseLayer.toggleUser(itemView.getUser(), false);
				}
				mapView.refreshMap();
			}
		});

		builder.setView(listView);
		
		builder.setNeutralButton("Done", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
			}
	        
	    });
		AlertDialog d = builder.create();
		d.setCanceledOnTouchOutside(true);
		d.show();
	}

	public Button createPointStyleButton(){
		Button button = new Button(this.getContext());
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
		button.setText("Style Point");
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				PointStyleDialog.Builder builder = new PointStyleDialog.Builder(LayerManagerDialog.this.getContext(), pointStyle);
				pointStyleDialog = (PointStyleDialog) builder.create();
				pointStyleDialog.show();
			}
				
		});
		return button;
	}
	
	public Button createLineStyleButton(){
		Button button = new Button(this.getContext());
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
		button.setText("Style Line");
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				LineStyleDialog.Builder builder = new LineStyleDialog.Builder(LayerManagerDialog.this.getContext(), lineStyle);
				lineStyleDialog = (LineStyleDialog) builder.create();
				lineStyleDialog.show();
			}
				
		});
		return button;
	}

	public Button createPolygonStyleButton(){
		Button button = new Button(this.getContext());
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
		button.setText("Style Polygon");
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				PolygonStyleDialog.Builder builder = new PolygonStyleDialog.Builder(LayerManagerDialog.this.getContext(), polygonStyle);
				polygonStyleDialog = (PolygonStyleDialog) builder.create();
				polygonStyleDialog.show();
			}
				
		});
		return button;
	}
	
	public Button createTextStyleButton(){
		Button button = new Button(this.getContext());
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layoutParams.weight = 1;
		button.setLayoutParams(layoutParams);
		button.setText("Style Text");
		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				TextStyleDialog.Builder builder = new TextStyleDialog.Builder(LayerManagerDialog.this.getContext(), textStyle);
				textStyleDialog = (TextStyleDialog) builder.create();
				textStyleDialog.show();
			}
				
		});
		return button;
	}
	
	private void createLayer(){
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerDialog.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Enter layer name:");
		
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		builder.setView(scrollView);
		
		final EditText editText = new EditText(LayerManagerDialog.this.getContext());
		layout.addView(editText);
		
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// ignore
			}
	        
	    });
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		final AlertDialog d = builder.create();
		d.setCanceledOnTouchOutside(true);
		d.show();
		
		d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					String layerName = editText.getText() != null ? editText.getText().toString() : null;
					
					if (layerName == null || "".equals(layerName)) {
						showErrorDialog("Please enter a layer name.");
					} else {
						mapView.addCanvasLayer(layerName);
						redrawLayers();
						d.dismiss();
					}
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
					showErrorDialog(e.getMessage());
				}
			}
			
		});
	}

	private void removeLayer(final Layer layer) {
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerDialog.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Do you want to delete layer?");
		
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				try {
					mapView.removeLayer(layer);
					if(layer instanceof TrackLogDatabaseLayer){
						mapView.setUserTrackLogLayer(null);
						mapView.clearUserCheckedList();
					}
					redrawLayers();
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
					showErrorDialog(e.getMessage());
				}
			}
	        
	    });
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		AlertDialog d = builder.create();
		d.setCanceledOnTouchOutside(true);
		d.show();
	}

	private void renameLayer(final Layer layer) {
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerDialog.this.getContext());
		
		builder.setTitle("Layer Manager");
		builder.setMessage("Enter layer name:");
		
		final EditText editText = new EditText(LayerManagerDialog.this.getContext());
		if(layer instanceof CustomGdalMapLayer){
			CustomGdalMapLayer gdalMapLayer = (CustomGdalMapLayer) layer;
			editText.setText(gdalMapLayer.getName());
		}else if(layer instanceof CustomSpatialiteLayer){
			CustomSpatialiteLayer spatialiteLayer = (CustomSpatialiteLayer) layer;
			editText.setText(spatialiteLayer.getName());
		}else if(layer instanceof CanvasLayer){
			CanvasLayer canvasLayer = (CanvasLayer) layer;
			editText.setText(canvasLayer.getName());
		}else if (layer instanceof DatabaseLayer) {
			DatabaseLayer databaseLayer = (DatabaseLayer) layer;
			editText.setText(databaseLayer.getName());
		}
		builder.setView(editText);
		
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// ignore
			}
	        
	    });
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	           // ignore
	        }
	    });
		
		final AlertDialog d = builder.create();
		d.setCanceledOnTouchOutside(true);
		d.show();
		
		d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					String layerName = editText.getText() != null ? editText.getText().toString() : null;
					
					if (layerName == null || "".equals(layerName)) {
						showErrorDialog("Please enter a layer name.");
					} else {
						mapView.renameLayer(layer, layerName);
						mapView.updateLayers();
						redrawLayers();
						d.dismiss();
					}
				} catch (Exception e) {
					FLog.e(e.getMessage(), e);
					showErrorDialog(e.getMessage());
				}
			}
			
		});
	}
	
	private void config(final Layer layer) {
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);
		
		TextView vectorLimitLabel = new TextView(this.getContext());
		vectorLimitLabel.setText("Max vector limit:");
		layout.addView(vectorLimitLabel);
		
		final EditText vectorLimitText = new EditText(LayerManagerDialog.this.getContext());
		if (layer instanceof CustomSpatialiteLayer) {
			vectorLimitText.setText("" + ((CustomSpatialiteLayer) layer).getMaxObjects());
		} else if (layer instanceof DatabaseLayer) {
			vectorLimitText.setText("" + ((DatabaseLayer) layer).getMaxObjects());
		}
		layout.addView(vectorLimitText);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerDialog.this.getContext());
		builder.setTitle("Layer Config");
		builder.setView(scrollView);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// ignore
			}
	        
	    });
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// ignore
			}
	        
	    });
		
		final AlertDialog d = builder.create();
		d.setCanceledOnTouchOutside(true);
		d.show();
		
		d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					int limit = Integer.parseInt(vectorLimitText.getText().toString());
					if (limit <= 0) {
						throw new Exception("Limit must be greater than 0");
					} else if (limit >= FaimsSettings.MAX_VECTOR_OBJECTS) {
						throw new Exception("Limit must be less than " + FaimsSettings.MAX_VECTOR_OBJECTS);
					}
					if (layer instanceof CustomSpatialiteLayer) {
						((CustomSpatialiteLayer) layer).setMaxObjects(limit);
					} else if (layer instanceof DatabaseLayer) {
						((DatabaseLayer) layer).setMaxObjects(limit);
					}
					mapView.refreshMap();
					d.dismiss();
				} catch (NumberFormatException e) {
					FLog.e("error setting config", e);
					showErrorDialog("Please enter an integer value");
				} catch (Exception e) {
					FLog.e("error setting config", e);
					showErrorDialog(e.getMessage());
				}
			}
			
		});
	}
	
	private void showMetadata(Layer layer) {
		ScrollView scrollView = new ScrollView(this.getContext());
		LinearLayout layout = new LinearLayout(this.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(layout);

		TextView layerTypeTextView = new TextView(this.getContext());
		layerTypeTextView.setText("Layer type:");
		layout.addView(layerTypeTextView);

		EditText layerTypeEditText = new EditText(LayerManagerDialog.this.getContext());
		layerTypeEditText.setEnabled(false);
		if(layer instanceof CustomGdalMapLayer){
			layerTypeEditText.setText("raster layer");
		}else if(layer instanceof CustomSpatialiteLayer){
			layerTypeEditText.setText("spatial layer");
		}else if(layer instanceof CanvasLayer){
			layerTypeEditText.setText("canvas layer");
		}else if (layer instanceof DatabaseLayer) {
			layerTypeEditText.setText("database layer");
		}
		layout.addView(layerTypeEditText);
		
		TextView layerNameTextView = new TextView(this.getContext());
		layerNameTextView.setText("Layer name:");
		layout.addView(layerNameTextView);

		if(layer instanceof CustomGdalMapLayer){
			CustomGdalMapLayer gdalMapLayer = (CustomGdalMapLayer) layer;

			EditText layerNameEditText = new EditText(LayerManagerDialog.this.getContext());
			layerNameEditText.setEnabled(false);
			layerNameEditText.setText(gdalMapLayer.getName());
			layout.addView(layerNameEditText);

			TextView fileNameTextView = new TextView(this.getContext());
			fileNameTextView.setText("File name:");
			layout.addView(fileNameTextView);

			File file = new File(gdalMapLayer.getGdalSource());
			EditText fileNameEditText = new EditText(LayerManagerDialog.this.getContext());
			fileNameEditText.setEnabled(false);
			fileNameEditText.setText(file.getName());
			layout.addView(fileNameEditText);

			TextView fileSizeTextView = new TextView(this.getContext());
			fileSizeTextView.setText("File size:");
			layout.addView(fileSizeTextView);

			EditText fileSizeEditText = new EditText(LayerManagerDialog.this.getContext());
			fileSizeEditText.setEnabled(false);
			fileSizeEditText.setText(file.length()/(1024 * 1024) + " MB");
			layout.addView(fileSizeEditText);

			double[][] originalBounds = gdalMapLayer.getBoundary();
			
			MapPos upperLeft = databaseManager.spatialRecord().convertFromProjToProj(GeometryUtil.EPSG4326, mapView.getModuleSrid(), new MapPos(originalBounds[0][0], originalBounds[0][1]));
			MapPos bottomRight = databaseManager.spatialRecord().convertFromProjToProj(GeometryUtil.EPSG4326, mapView.getModuleSrid(), new MapPos(originalBounds[3][0], originalBounds[3][1]));
			
	        TextView upperLeftTextView = new TextView(this.getContext());
	        upperLeftTextView.setText("Upper left boundary:");
			layout.addView(upperLeftTextView);

			EditText upperLeftEditText = new EditText(LayerManagerDialog.this.getContext());
			upperLeftEditText.setEnabled(false);
			upperLeftEditText.setText(upperLeft != null ? (upperLeft.x + "," + upperLeft.y) : "N/A");
			layout.addView(upperLeftEditText);

			TextView bottomRightTextView = new TextView(this.getContext());
			bottomRightTextView.setText("Bottom right boundary:");
			layout.addView(bottomRightTextView);

			EditText bottomRightEditText = new EditText(LayerManagerDialog.this.getContext());
			bottomRightEditText.setEnabled(false);
			bottomRightEditText.setText(bottomRight != null ? (bottomRight.x + "," + bottomRight.y) : "N/A");
			layout.addView(bottomRightEditText);

		}else if(layer instanceof CustomSpatialiteLayer){
			CustomSpatialiteLayer spatialiteLayer = (CustomSpatialiteLayer) layer;

			EditText layerNameEditText = new EditText(LayerManagerDialog.this.getContext());
			layerNameEditText.setEnabled(false);
			layerNameEditText.setText(spatialiteLayer.getName());
			layout.addView(layerNameEditText);

			TextView fileNameTextView = new TextView(this.getContext());
			fileNameTextView.setText("File name:");
			layout.addView(fileNameTextView);

			File file = new File(spatialiteLayer.getDbPath());
			EditText fileNameEditText = new EditText(LayerManagerDialog.this.getContext());
			fileNameEditText.setEnabled(false);
			fileNameEditText.setText(file.getName());
			layout.addView(fileNameEditText);

			TextView fileSizeTextView = new TextView(this.getContext());
			fileSizeTextView.setText("File size:");
			layout.addView(fileSizeTextView);

			EditText fileSizeEditText = new EditText(LayerManagerDialog.this.getContext());
			fileSizeEditText.setEnabled(false);
			fileSizeEditText.setText(file.length()/(1024 * 1024) + " MB");
			layout.addView(fileSizeEditText);

			TextView tableNameTextView = new TextView(this.getContext());
			tableNameTextView.setText("Table name:");
			layout.addView(tableNameTextView);

			EditText tableNameEditText = new EditText(LayerManagerDialog.this.getContext());
			tableNameEditText.setEnabled(false);
			tableNameEditText.setText(spatialiteLayer.getTableName());
			layout.addView(tableNameEditText);

		}else if(layer instanceof CanvasLayer){
			CanvasLayer canvasLayer = (CanvasLayer) layer;

			EditText layerNameEditText = new EditText(LayerManagerDialog.this.getContext());
			layerNameEditText.setEnabled(false);
			layerNameEditText.setText(canvasLayer.getName());
			layout.addView(layerNameEditText);
		}else if(layer instanceof DatabaseLayer){
			DatabaseLayer databaseLayer = (DatabaseLayer) layer;

			EditText layerNameEditText = new EditText(LayerManagerDialog.this.getContext());
			layerNameEditText.setEnabled(false);
			layerNameEditText.setText(databaseLayer.getName());
			layout.addView(layerNameEditText);
			
			TextView tableNameTextView = new TextView(this.getContext());
			tableNameTextView.setText("Query name:");
			layout.addView(tableNameTextView);

			EditText tableNameEditText = new EditText(LayerManagerDialog.this.getContext());
			tableNameEditText.setEnabled(false);
			tableNameEditText.setText(databaseLayer.getQueryName());
			layout.addView(tableNameEditText);
		}else{
			showErrorDialog("wrong type of layer");
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(LayerManagerDialog.this.getContext());
		builder.setTitle("Layer Metadata");
		builder.setView(scrollView);
		builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {

			}
	        
	    });
		
		AlertDialog d = builder.create();
		d.setCanceledOnTouchOutside(true);
		d.show();
	}

	private void showErrorDialog(String message) {
		new ErrorDialog(LayerManagerDialog.this.getContext(), "Layer Manager Error", message).show();
	}
	
	private void showFileBrowser(int requestCode){
		mapView.getActivity().showFileBrowser(requestCode);
	}
	
	public void setTableSpinner() throws jsqlite.Exception{
			synchronized(DatabaseManager.class) {
				List<String> tableName = new ArrayList<String>();
				Stmt st = null;
				Database db = null;
				try {
					db = new jsqlite.Database();
					db.open(spatialFile.getPath(), jsqlite.Constants.SQLITE_OPEN_READWRITE);
					db.exec("PRAGMA temp_store = 2", null);
					
					String query = "select f_table_name from geometry_columns;";
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
	
	public void setTableColumnSpinners(String tableName) throws jsqlite.Exception{
		synchronized(DatabaseManager.class) {
			List<String> columnNames = new ArrayList<String>();
			Stmt st = null;
			Database db = null;
			try {
				db = new jsqlite.Database();
				db.open(spatialFile.getPath(), jsqlite.Constants.SQLITE_OPEN_READWRITE);
				db.exec("PRAGMA temp_store = 2", null);
				
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
				idColumnSpinner.setAdapter(arrayAdapter);
				idColumnSpinner.setSelection(0);
			}
		}
	}
}
