package au.org.intersect.faims.android.ui.map;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import javax.microedition.khronos.opengles.GL10;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.SparseArray;
import android.view.View;
import android.widget.RelativeLayout;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.data.GeometryStyle;
import au.org.intersect.faims.android.exceptions.MapException;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.managers.FileManager;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.CustomGdalMapLayer;
import au.org.intersect.faims.android.nutiteq.CustomLine;
import au.org.intersect.faims.android.nutiteq.CustomOgrLayer;
import au.org.intersect.faims.android.nutiteq.CustomPoint;
import au.org.intersect.faims.android.nutiteq.CustomPolygon;
import au.org.intersect.faims.android.nutiteq.CustomSpatialiteLayer;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.map.tools.AzimuthTool;
import au.org.intersect.faims.android.ui.map.tools.CreateLineTool;
import au.org.intersect.faims.android.ui.map.tools.CreatePointTool;
import au.org.intersect.faims.android.ui.map.tools.CreatePolygonTool;
import au.org.intersect.faims.android.ui.map.tools.EditTool;
import au.org.intersect.faims.android.ui.map.tools.LineDistanceTool;
import au.org.intersect.faims.android.ui.map.tools.MapTool;
import au.org.intersect.faims.android.ui.map.tools.PointDistanceTool;
import au.org.intersect.faims.android.ui.map.tools.SelectTool;
import au.org.intersect.faims.android.util.ScaleUtil;

import com.nutiteq.MapView;
import com.nutiteq.components.Components;
import com.nutiteq.components.Constraints;
import com.nutiteq.components.MapPos;
import com.nutiteq.components.Options;
import com.nutiteq.components.Range;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.layers.Layer;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.ui.MapListener;
import com.nutiteq.utils.UnscaledBitmapLoader;

public class CustomMapView extends MapView implements FileManager.FileSelectionListener{

	public class InternalMapListener extends MapListener {

		@Override
		public void onDrawFrameAfter3D(GL10 arg0, float arg1) {

		}

		@Override
		public void onDrawFrameBefore3D(GL10 arg0, float arg1) {
		}

		@Override
		public void onLabelClicked(VectorElement arg0, boolean arg1) {
		}

		@Override
		public void onMapClicked(double arg0, double arg1, boolean arg2) {
			if (CustomMapView.this.mapListener != null) {
				CustomMapView.this.mapListener.onMapClicked(arg0, arg1, arg2);
			}
			if (CustomMapView.this.currentTool != null) {
				CustomMapView.this.currentTool.onMapClicked(arg0, arg1, arg2);
			}
		}

		@Override
		public void onMapMoved() {
			if (CustomMapView.this.currentTool != null) {
				CustomMapView.this.currentTool.onMapChanged();
			}
			CustomMapView.this.updateDrawView();
		}

		@Override
		public void onSurfaceChanged(GL10 arg0, int arg1, int arg2) {
		}

		@Override
		public void onVectorElementClicked(VectorElement arg0, double arg1,
				double arg2, boolean arg3) {
			if (CustomMapView.this.mapListener != null) {
				CustomMapView.this.mapListener.onVectorElementClicked(arg0,
						arg1, arg2, arg3);
			}
			if (CustomMapView.this.currentTool != null) {
				CustomMapView.this.currentTool.onVectorElementClicked(arg0,
						arg1, arg2, arg3);
			}
		}

	}

	public static class CustomMapListener {

		public void onMapClicked(double arg0, double arg1, boolean arg2) {
		}

		public void onVectorElementClicked(VectorElement arg0, double arg1,
				double arg2, boolean arg3) {
		}
	}

	// TODO what is this?
	private static int cacheId = 9991;

	private static int layerId = 1;

	private static int geomId = 1;

	private SparseArray<Layer> layerIdMap;

	private HashMap<String, Layer> layerNameMap;

	private SparseArray<Geometry> geometryIdMap;

	private HashMap<Geometry, Layer> geometryLayerMap;

	private DrawView drawView;

	private EditView editView;
	
	private MapNorthView northView;

	private ScaleBarView scaleView;

	private RelativeLayout toolsView;

	private ArrayList<Runnable> runnableList;
	private ArrayList<Thread> threadList;

	private boolean canRunThreads;

	private ArrayList<MapTool> tools;

	private MapTool currentTool;

	private InternalMapListener internalMapListener;
	private CustomMapListener mapListener;

	private Layer selectedLayer;
	
	private ArrayList<Geometry> selectedGeometryList;

	private ArrayList<Geometry> transformGeometryList;

	private boolean showDecimal;
	private LayerManagerView layerManager;
	private FileManager fm;

	private boolean showKm;
	
	public CustomMapView(Context context, DrawView drawView, EditView editView, MapNorthView northView, ScaleBarView scaleView, RelativeLayout toolsView) {

		this(context);

		layerIdMap = new SparseArray<Layer>();
		layerNameMap = new HashMap<String, Layer>();
		geometryIdMap = new SparseArray<Geometry>();
		geometryLayerMap = new HashMap<Geometry, Layer>();
        runnableList = new ArrayList<Runnable>();
        threadList = new ArrayList<Thread>();
        tools = new ArrayList<MapTool>();
        selectedGeometryList = new ArrayList<Geometry>();
		
		this.drawView = drawView;
		this.editView = editView;
		this.northView = northView;
		this.scaleView = scaleView;
		this.toolsView = toolsView;
		
		this.drawView.setMapView(this);
		this.editView.setMapView(this);
		
		this.editView.setColor(Color.GREEN);
		
		// TODO make this configurable
		scaleView.setBarWidthRange((int) ScaleUtil.getDip(context, 40),
				(int) ScaleUtil.getDip(context, 100));

		initTools();

		setViewLocked(true); // note: this is the default behaviour for maps

		internalMapListener = new InternalMapListener();
		getOptions().setMapListener(internalMapListener);
	}

	public CustomMapView(Context context) {
		super(context);

		this.setComponents(new Components());

		// Activate some mapview options to make it smoother - optional
		this.getOptions().setPreloading(true);
		this.getOptions().setSeamlessHorizontalPan(true);
		this.getOptions().setTileFading(true);
		this.getOptions().setKineticPanning(true);
		// this.getOptions().setDoubleClickZoomIn(true);
		// this.getOptions().setDualClickZoomOut(true);

		// set sky bitmap - optional, default - white
		this.getOptions().setSkyDrawMode(Options.DRAW_BITMAP);
		this.getOptions().setSkyOffset(4.86f);
		this.getOptions().setSkyBitmap(
				UnscaledBitmapLoader.decodeResource(getResources(),
						R.drawable.sky_small));

		// Map background, visible if no map tiles loaded - optional, default -
		// white
		this.getOptions().setBackgroundPlaneDrawMode(Options.DRAW_BITMAP);
		this.getOptions().setBackgroundPlaneBitmap(
				UnscaledBitmapLoader.decodeResource(getResources(),
						R.drawable.background_plane));
		this.getOptions().setClearColor(Color.WHITE);

		// configure texture caching - optional, suggested
		this.getOptions().setTextureMemoryCacheSize(40 * 1024 * 1024);
		this.getOptions().setCompressedMemoryCacheSize(8 * 1024 * 1024);

		// TODO find out how this works? can we pass different paths for
		// different maps?
		// this.getOptions().setPersistentCachePath(activity.getDatabasePath("mapcache").getPath());
		// set persistent raster cache limit to 100MB
		// this.getOptions().setPersistentCacheSize(100 * 1024 * 1024);
	}

	public static int nextId() {
		return cacheId++;
	}

	public static int nextLayerId() {
		return layerId++;
	}

	public static int nextGeomId() {
		return geomId++;
	}

	public int addLayer(Layer layer) throws Exception {
		if (layerIdMap.get(getLayerId(layer)) != null) {
			throw new MapException("Layer already exists");
		}

		layerIdMap.put(getLayerId(layer), layer);
		layerNameMap.put(getLayerName(layer), layer);

		return getLayerId(layer);
	}

	public void removeLayer(int layerId) throws Exception {
		Layer layer = layerIdMap.get(layerId);
		removeLayer(layer);
	}

	public void removeLayer(Layer layer) throws Exception {
		if (layer == null) {
			throw new MapException("Layer does not exist");
		}

		// can only remove base layer if its the only layer on the map
		CustomGdalMapLayer baseLayer = (CustomGdalMapLayer) getLayers()
				.getBaseLayer();
		if (baseLayer == layer) {
			throw new MapException("Cannot remove base layer");
		}

		this.getLayers().removeLayer(layer);
		int id = getLayerId(layer);
		String name = getLayerName(layer);
		this.layerIdMap.remove(id);
		this.layerNameMap.remove(name);
		
		// remove all geometry in layer if canvas layer
		if (layer instanceof CanvasLayer) {
			CanvasLayer canvas = (CanvasLayer) layer;
			for (Geometry geom : canvas.getGeometryList()) {
				removeGeometry(geom);
			}
		}
		
		if (layer == selectedLayer) {
			this.selectedLayer = null;
			updateTools();
		}
	}

	public Layer getLayer(int layerId) {
		return layerIdMap.get(layerId);
	}

	public Layer getLayer(String layerName) {
		return layerNameMap.get(layerName);
	}

	public String getLayerName(Layer layer) {
		String layerName = "N/A";
		if (layer instanceof CustomGdalMapLayer) {
			layerName = ((CustomGdalMapLayer) layer).getName();
		} else if (layer instanceof CustomOgrLayer) {
			layerName = ((CustomOgrLayer) layer).getName();
		} else if (layer instanceof CustomSpatialiteLayer) {
			layerName = ((CustomSpatialiteLayer) layer).getName();
		} else if (layer instanceof CanvasLayer) {
			layerName = ((CanvasLayer) layer).getName();
		}
		return layerName;
	}

	public void setLayerName(Layer layer, String layerName) {
		if (layer instanceof CustomGdalMapLayer) {
			((CustomGdalMapLayer) layer).setName(layerName);
		} else if (layer instanceof CustomOgrLayer) {
			((CustomOgrLayer) layer).setName(layerName);
		} else if (layer instanceof CustomSpatialiteLayer) {
			((CustomSpatialiteLayer) layer).setName(layerName);
		} else if (layer instanceof CanvasLayer) {
			((CanvasLayer) layer).setName(layerName);
		}
	}

	public int getLayerId(Layer layer) {
		int layerId = 0;
		if (layer instanceof CustomGdalMapLayer) {
			layerId = ((CustomGdalMapLayer) layer).getLayerId();
		} else if (layer instanceof CustomOgrLayer) {
			layerId = ((CustomOgrLayer) layer).getLayerId();
		} else if (layer instanceof CustomSpatialiteLayer) {
			layerId = ((CustomSpatialiteLayer) layer).getLayerId();
		} else if (layer instanceof CanvasLayer) {
			layerId = ((CanvasLayer) layer).getLayerId();
		}
		return layerId;
	}

	public int addGeometry(Layer layer, Geometry geom) throws Exception {
		if (geom == null) {
			throw new MapException("Geometry does not exist");
		}
		
		if (layer == null) {
			throw new MapException("Map does not exist");
		}
		
		if (geometryIdMap.get(getGeometryId(geom)) != null) {
			throw new MapException("Geometry already exists");
		}

		geometryIdMap.put(getGeometryId(geom), geom);
		geometryLayerMap.put(geom, layer);

		return getGeometryId(geom);
	}

	public void removeGeometry(int geomId) throws Exception {
		removeGeometry(getGeometry(geomId));
	}

	public void removeGeometry(Geometry geom) throws Exception {
		if (geom == null) {
			throw new MapException("Geometry does not exist");
		}

		geometryIdMap.remove(getGeometryId(geom));
		geometryLayerMap.remove(geom);
		
		clearSelection();
		clearSelectionTransform();
	}
	
	public void removeGeometryWithoutClearing(Geometry geom) throws Exception {
		if (geom == null) {
			throw new MapException("Geometry does not exist");
		}

		geometryIdMap.remove(getGeometryId(geom));
		geometryLayerMap.remove(geom);
	}

	public int getGeometryId(Geometry geom) {
		if (geom instanceof CustomPoint) {
			return ((CustomPoint) geom).getGeomId();
		} else if (geom instanceof CustomLine) {
			return ((CustomLine) geom).getGeomId();
		} else if (geom instanceof CustomPolygon) {
			return ((CustomPolygon) geom).getGeomId();
		}
		return 0;
	}

	public Geometry getGeometry(int geomId) {
		return geometryIdMap.get(geomId);
	}

	public void setViewLocked(boolean lock) {
		if (lock) {
			this.getConstraints().setTiltRange(new Range(90.0f, 90.0f));
			this.setTilt(90.0f);
		} else {
			this.getConstraints().setTiltRange(Constraints.DEFAULT_TILT_RANGE);
		}
	}

	public static void registerLicense(Context context) {
		final String LICENSE = "XTUMwQ0ZIRklrbEZ2T0dIdkZ3QkRieVBtcWJqdjZ1RUtBaFVBa1RreXdabUIraER4UjFmZ01aUk5oay83a2hzPQoKcGFja2FnZU5hbWU9YXUub3JnLmludGVyc2VjdC5mYWltcy5hbmRyb2lkCndhdGVybWFyaz1jdXN0b20KCg==";
		CustomMapView.registerLicense(LICENSE, context);
		Bitmap logo = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.ic_launcher);
		CustomMapView.setWatermark(logo, -1.0f, -1.0f, 0.2f);
	}

	public void updateMapOverlay() {
		northView.setMapRotation(this.getRotation());
		int width = this.getWidth();
		int height = this.getHeight();

		scaleView.setMapBoundary(this.getZoom(), width, height, GeometryUtil
				.distance(GeometryUtil.convertToWgs84(this.screenToWorld(0,
						height, 0)), GeometryUtil.convertToWgs84(this
						.screenToWorld(width, height, 0))));
	}

	public FileManager getFileManager() {
		return fm;
	}

	public void setFileManager(FileManager fm) {
		this.fm = fm;
	}

	public void startThread(Runnable runnable) {
		runnableList.add(runnable);

		// Note: the runnable will need to handle stopping the thread
		Thread t = new Thread(runnable);
		threadList.add(t);
		t.start();
	}

	public void restartThreads() {
		try {
			// wait for all threads to finish
			canRunThreads = false;
			while (true) {
				boolean allThreadsTerminated = true;
				for (Thread t : threadList) {
					if (t.getState() != Thread.State.TERMINATED) {
						allThreadsTerminated = false;
						break;
					}
				}
				if (allThreadsTerminated) {
					break;
				}
				FLog.d("Waiting to start map threads");
				Thread.sleep(1000);
			}
			canRunThreads = true;
			threadList.clear();
			for (Runnable r : runnableList) {
				Thread t = new Thread(r);
				threadList.add(t);
				t.start();
			}
		} catch (Exception e) {
			FLog.e("error restarting map threads", e);
		}
	}

	public void killThreads() {
		canRunThreads = false;
	}

	public boolean canRunThreads() {
		return canRunThreads;
	}

	public int addRasterMap(String layerName, String file) throws Exception {
		if (!new File(file).exists()) {
			throw new MapException("Error map does not exist " + file);
		}

		validateLayerName(layerName);

		CustomGdalMapLayer gdalLayer = new CustomGdalMapLayer(nextLayerId(),
				layerName, new EPSG3857(), 0, 18, CustomMapView.nextId(), file,
				this, true);
		gdalLayer.setShowAlways(true);
		this.getLayers().setBaseLayer(gdalLayer);

		return addLayer(gdalLayer);
	}

	public void setMapFocusPoint(float longitude, float latitude)
			throws Exception {
		if (latitude < -90.0f || latitude > 90.0f) {
			throw new MapException("Error map latitude out of range "
					+ latitude);
		}
		this.setFocusPoint(new EPSG3857().fromWgs84(longitude, latitude));
	}

	public int addShapeLayer(String layerName, String file,
			StyleSet<PointStyle> pointStyleSet,
			StyleSet<LineStyle> lineStyleSet,
			StyleSet<PolygonStyle> polygonStyleSet) throws Exception {

		if (!new File(file).exists()) {
			throw new MapException("Error file does not exist " + file);
		}

		validateLayerName(layerName);

		CustomOgrLayer ogrLayer = new CustomOgrLayer(nextLayerId(), layerName,
				new EPSG3857(), file, null, FaimsSettings.MAX_VECTOR_OBJECTS,
				pointStyleSet, lineStyleSet, polygonStyleSet);
		// ogrLayer.printSupportedDrivers();
		// ogrLayer.printLayerDetails(table);
		this.getLayers().addLayer(ogrLayer);
		return addLayer(ogrLayer);
	}

	public int addSpatialLayer(String layerName, String file, String tablename,
			String labelColumn, StyleSet<PointStyle> pointStyleSet,
			StyleSet<LineStyle> lineStyleSet,
			StyleSet<PolygonStyle> polygonStyleSet) throws Exception {
		if (!new File(file).exists()) {
			throw new MapException("Error file does not exist " + file);
		}

		validateLayerName(layerName);

		CustomSpatialiteLayer spatialLayer = new CustomSpatialiteLayer(
				nextLayerId(), layerName, new EPSG3857(), file, tablename,
				"Geometry", new String[] { labelColumn },
				FaimsSettings.MAX_VECTOR_OBJECTS, pointStyleSet, lineStyleSet,
				polygonStyleSet);
		this.getLayers().addLayer(spatialLayer);
		return addLayer(spatialLayer);
	}

	public int addCanvasLayer(String layerName) throws Exception {
		validateLayerName(layerName);

		CanvasLayer layer = new CanvasLayer(nextLayerId(), layerName,
				new EPSG3857());
		this.getLayers().addLayer(layer);
		return addLayer(layer);
	}

	public CustomPoint drawPoint(int layerId, MapPos point, GeometryStyle style) throws Exception {
		return drawPoint(getLayer(layerId), point, style);
	}
	
	public CustomPoint drawPoint(Layer layer, MapPos point, GeometryStyle style)  throws Exception {
		CanvasLayer canvas = (CanvasLayer) layer;
		if (canvas == null) {
			throw new MapException("Layer does not exist");
		}
		CustomPoint p = canvas.addPoint(nextGeomId(), point, style);
		addGeometry(layer, p);
		updateRenderer();
		return p;
	}
	
	public void restylePoint(CustomPoint point, GeometryStyle style) throws Exception {
		CanvasLayer canvas = (CanvasLayer) geometryLayerMap.get(point);
		if (canvas == null) {
			throw new MapException("Layer does not exist");
		}
		canvas.removeGeometry(point);
		removeGeometry(point);
		drawPoint(canvas, GeometryUtil.convertToWgs84(point.getMapPos()), style);
		addGeometry(canvas, point);
		updateRenderer();
	}

	public CustomLine drawLine(int layerId, List<MapPos> points, GeometryStyle style) throws Exception {
		return drawLine(getLayer(layerId), points, style);
	}
	
	public CustomLine drawLine(Layer layer, List<MapPos> points, GeometryStyle style) throws Exception {
		CanvasLayer canvas = (CanvasLayer) layer;
		if (canvas == null) {
			throw new MapException("Layer does not exist");
		}
		CustomLine l = canvas.addLine(nextGeomId(), points, style);
		addGeometry(layer, l);
		updateRenderer();
		return l;
	}

	public void restyleLine(CustomLine line, GeometryStyle style) throws Exception {
		CanvasLayer canvas = (CanvasLayer) geometryLayerMap.get(line);
		if (canvas == null) {
			throw new MapException("Layer does not exist");
		}
		canvas.removeGeometry(line);
		removeGeometry(line);
		drawLine(canvas, GeometryUtil.projectVertices(new EPSG3857(), line.getVertexList()), style);
		addGeometry(canvas, line);
		updateRenderer();
	}

	public CustomPolygon drawPolygon(int layerId, List<MapPos> points, GeometryStyle style) throws Exception {
		return drawPolygon(getLayer(layerId), points, style);
	}

	public CustomPolygon drawPolygon(Layer layer, List<MapPos> points, GeometryStyle style) throws Exception {
		CanvasLayer canvas = (CanvasLayer) layer;
		if (canvas == null) {
			throw new MapException("Layer does not exist");
		}
		CustomPolygon p = canvas.addPolygon(nextGeomId(), points, style);
		addGeometry(layer, p);
		updateRenderer();
		return p;
	}
	
	public void restylePolygon(CustomPolygon polygon, GeometryStyle style) throws Exception {
		CanvasLayer canvas = (CanvasLayer) geometryLayerMap.get(polygon);
		if (canvas == null) {
			throw new MapException("Layer does not exist");
		}
		canvas.removeGeometry(polygon);
		removeGeometry(polygon);
		drawPolygon(canvas, GeometryUtil.projectVertices(new EPSG3857(), polygon.getVertexList()), style);
		addGeometry(canvas, polygon);
		updateRenderer();
	}

	public void clearGeometry(int geomId) throws Exception {
		clearGeometry(getGeometry(geomId));
	}

	public void clearGeometry(Geometry geom) throws Exception {
		if (geom == null) {
			throw new MapException("Geometry does not exist");
		}

		CanvasLayer layer = (CanvasLayer) geometryLayerMap.get(geom);
		if (layer == null) {
			throw new MapException("Layer does not exist");
		}
		layer.removeGeometry(geom);

		removeGeometry(geom);

		updateRenderer();
	}

	public void clearGeometryList(List<? extends Geometry> geomList)
			throws Exception {
		for (Object geom : geomList) {
			clearGeometry((Geometry) geom);
		}
	}

	public List<Geometry> getGeometryList(int layerId) throws Exception {
		return getGeometryList(getLayer(layerId));
	}

	public List<Geometry> getGeometryList(Layer layer) throws Exception {
		CanvasLayer canvas = (CanvasLayer) layer;

		return GeometryUtil.projectGeometryList(new EPSG3857(),
				canvas.getGeometryList());
	}

	public List<MapTool> getTools() {
		return tools;
	}

	public void showLayersDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
		layerManager = new LayerManagerView(this.getContext(),fm);
		layerManager.attachToMap(this);

		builder.setTitle("Layer Manager");
		builder.setView(layerManager);
		builder.create().show();
	}

	private void validateLayerName(String name) throws Exception {
		if (name == null || "".equals(name)) {
			throw new MapException("Please specify a name for the layer");
		} else if (getLayer(name) != null) {
			throw new MapException("Layer " + name + " already exists");
		}
	}

	public void renameLayer(Layer layer, String layerName) throws Exception {
		if (layer == null) {
			throw new MapException("Layer does not exist");
		}

		validateLayerName(layerName);

		setLayerName(layer, layerName);
	}

	private void initTools() {
		tools.add(new SelectTool(this.getContext(), this));
		tools.add(new EditTool(this.getContext(), this));
		tools.add(new CreatePointTool(this.getContext(), this));
		tools.add(new CreateLineTool(this.getContext(), this));
		tools.add(new CreatePolygonTool(this.getContext(), this));
		tools.add(new PointDistanceTool(this.getContext(), this));
		tools.add(new LineDistanceTool(this.getContext(), this));
		tools.add(new AzimuthTool(this.getContext(), this));
	}

	public MapTool getTool(String name) {
		for (MapTool tool : tools) {
			if (tool.toString().equals(name)) {
				return tool;
			}
		}
		return null;
	}

	public void selectToolIndex(int index) {
		selectTool(tools.get(index).toString());
	}

	public void selectTool(String name) {
		if (currentTool != null) {
			currentTool.deactivate();
			currentTool = null;
		}

		toolsView.removeAllViews();

		MapTool tool = getTool(name);
		View ui = tool.getUI();
		if (ui != null) {
			toolsView.addView(ui);
		}
		tool.activate();

		currentTool = tool;
	}

	public void selectDefaultTool() {
		selectTool(CreatePointTool.NAME);
	}

	public void updateTools() {
		for (MapTool tool: tools) {
			tool.onLayersChanged();
		}
	}

	public void setMapListener(CustomMapListener customMapListener) {
		this.mapListener = customMapListener;
	}

	public void setSelectedLayer(Layer layer) {
		selectedLayer = layer;
	}

	public Layer getSelectedLayer() {
		return selectedLayer;
	}

	public void updateRenderer() {
		if (getComponents() != null) {
			getComponents().mapRenderers.getMapRenderer().frustumChanged();
		}
	}

	public void addSelection(int geomId) throws Exception {
		addSelection(getGeometry(geomId));
	}

	public void addSelection(Geometry geom) throws Exception {
		if (geom == null) {
			throw new MapException("Geometry does not exist");
		}
		
		if (hasSelection(geom)) return;
		
		if (transformGeometryList != null) {
			throw new MapException("Geometry selection is locked");
		}
		
		selectedGeometryList.add(geom);
		updateDrawView();
	}

	public void clearSelection() throws Exception {
		if (selectedGeometryList.isEmpty()) return;
		
		if (transformGeometryList != null) {
			throw new MapException("Geometry selection is locked");
		}
		
		selectedGeometryList = new ArrayList<Geometry>();
		updateDrawView();
	}
	
	public void removeSelection(Geometry geom) throws Exception {
		if (selectedGeometryList.isEmpty()) return;
		
		if (transformGeometryList != null) {
			throw new MapException("Geometry selection is locked");
		}
		
		selectedGeometryList.remove(geom);
		updateDrawView();
	}
	
	public boolean hasSelection(Geometry geom) {
		return selectedGeometryList.contains(geom);
	}
	
	public List<Geometry> getSelection() {
		return selectedGeometryList;
	}

	public void updateSelection() throws Exception {
		if (selectedGeometryList.isEmpty()) return;
		
		// note: remove geometry from list that no longer exist or are not visible and update others
		for (ListIterator<Geometry> iterator = selectedGeometryList.listIterator(); iterator.hasNext();) {
			Geometry geom = getGeometry(getGeometryId(iterator.next()));
			if (geom == null) {
				iterator.remove();
			} else {
				CanvasLayer canvas = (CanvasLayer) geometryLayerMap.get(geom);
				if (!canvas.isVisible()) {
					iterator.remove();
				} else {
					iterator.set(geom);
				}
			}
		}
		
		updateDrawView();
	}
	
	public void prepareSelectionTransform() {
		// keep a copy of the geometry at the current position
		transformGeometryList = GeometryUtil.transformGeometryList(selectedGeometryList, this, true);
		
		updateDrawView();
	}
	
	public void doSelectionTransform() throws Exception {
		if (transformGeometryList == null) return;
		
		ArrayList<Geometry> geomList = GeometryUtil.transformGeometryList(transformGeometryList, this, false);
		
		for (int i = 0; i < selectedGeometryList.size(); i++) {
			Geometry geom = selectedGeometryList.get(i);
			Geometry transformedGeom = geomList.get(i);
			CanvasLayer layer = (CanvasLayer) geometryLayerMap.get(geom);
			layer.removeGeometry(geom);
			removeGeometryWithoutClearing(geom);
			layer.addGeometry(transformedGeom);
			addGeometry(layer, transformedGeom);
		}
		
		transformGeometryList = null;
		selectedGeometryList = geomList;
		
		updateDrawView();
	}
	
	public void clearSelectionTransform() {
		transformGeometryList = null;
		updateRenderer();
	}
	
	private void updateDrawView() {
		editView.setDrawList(transformGeometryList);
		drawView.setDrawList(selectedGeometryList);
	}

	public int getDrawViewColor() {
		return drawView.getColor();
	}

	public void setDrawViewColor(int color) {
		drawView.setColor(color);
		updateDrawView();
	}
	
	public int getEditViewColor() {
		return editView.getColor();
	}

	public void setEditViewColor(int color) {
		editView.setColor(color);
		updateDrawView();
	}
	
	public float getDrawViewStrokeStyle() {
		return drawView.getStrokeSize();
	}
	
	public void setDrawViewStrokeStyle(float strokeSize) {
		drawView.setStrokeSize(strokeSize);
		updateDrawView();
	}
	
	public float getEditViewStrokeStyle() {
		return editView.getStrokeSize();
	}
	
	public void setEditViewStrokeStyle(float strokeSize) {
		editView.setStrokeSize(strokeSize);
		updateDrawView();
	}
	
	public float getDrawViewTextSize() {
		return drawView.getTextSize();
	}
	
	public void setDrawViewTextSize(float value) {
		drawView.setTextSize(value);
		updateDrawView();
	}
	
	public float getEditViewTextSize() {
		return editView.getTextSize();
	}
	
	public void setEditViewTextSize(float value) {
		editView.setTextSize(value);
		updateDrawView();
	}

	public boolean hasTransformGeometry() {
		return transformGeometryList != null;
	}

	public void setDrawViewDetail(boolean value) {
		drawView.setShowDetail(value);
		updateDrawView();
	}
	
	public void setEditViewDetail(boolean value) {
		editView.setShowDetail(value);
		updateDrawView();
	}

	public boolean showDecimal() {
		return showDecimal;
	}
	
	public void setShowDecimal(boolean value) {
		showDecimal = value;
		drawView.showDecimal(value);
		editView.showDecimal(value);
		updateDrawView();
	}
	
	@Override
	public void onFileChangesListener(boolean isSpatialFile) {
		layerManager.setSelectedFilePath(fm.getSelectedFile().getName(),isSpatialFile);
	}

	public boolean showKm() {
		return showKm;
	}

	public void setShowKm(boolean value) {
		showKm = value;
	}
}
