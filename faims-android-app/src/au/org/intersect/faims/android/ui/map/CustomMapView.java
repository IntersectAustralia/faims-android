package au.org.intersect.faims.android.ui.map;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.util.SparseArray;
import android.view.View;
import android.widget.RelativeLayout;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.exceptions.MapException;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.CustomGdalMapLayer;
import au.org.intersect.faims.android.nutiteq.CustomOgrLayer;
import au.org.intersect.faims.android.nutiteq.CustomSpatialiteLayer;
import au.org.intersect.faims.android.nutiteq.GeometryUtil;
import au.org.intersect.faims.android.ui.map.tools.CreateLineTool;
import au.org.intersect.faims.android.ui.map.tools.CreatePointTool;
import au.org.intersect.faims.android.ui.map.tools.CreatePolygonTool;
import au.org.intersect.faims.android.ui.map.tools.MapTool;
import au.org.intersect.faims.android.util.Dip;

import com.nutiteq.MapView;
import com.nutiteq.components.Components;
import com.nutiteq.components.Constraints;
import com.nutiteq.components.MapPos;
import com.nutiteq.components.Options;
import com.nutiteq.components.Range;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.layers.Layer;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.ui.MapListener;
import com.nutiteq.utils.UnscaledBitmapLoader;
import com.nutiteq.vectorlayers.GeometryLayer;

public class CustomMapView extends MapView {
	
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
		}

		@Override
		public void onSurfaceChanged(GL10 arg0, int arg1, int arg2) {
		}

		@Override
		public void onVectorElementClicked(VectorElement arg0, double arg1,
				double arg2, boolean arg3) {
			if (CustomMapView.this.mapListener != null) {
				CustomMapView.this.mapListener.onVectorElementClicked(arg0, arg1, arg2, arg3);
			}
			if (CustomMapView.this.currentTool != null) {
				CustomMapView.this.currentTool.onVectorElementClicked(arg0, arg1, arg2, arg3);
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
	
	private int vectorId = 1;
	
	private SparseArray<GeometryLayer> vectorLayerArray;
	
	private DrawView drawView;
	
	private MapNorthView northView;
	
	private ScaleBarView scaleView;
	
	private RelativeLayout toolsView;

	private Geometry overlayGeometry;
	
	private ArrayList<Runnable> runnableList;
	private ArrayList<Thread> threadList;

	private boolean canRunThreads;
	
	private ArrayList<MapTool> tools;

	private MapTool currentTool;

	private InternalMapListener internalMapListener;
	private CustomMapListener mapListener;

	private Layer selectedLayer;

	private Geometry selectedGeom;
	
	public CustomMapView(Context context, DrawView drawView, MapNorthView northView, ScaleBarView scaleView, RelativeLayout toolsView) {
		this(context);
		
		vectorLayerArray = new SparseArray<GeometryLayer>();
        runnableList = new ArrayList<Runnable>();
        threadList = new ArrayList<Thread>();
        tools = new ArrayList<MapTool>();
		
		this.drawView = drawView;
		this.northView = northView;
		this.scaleView = scaleView;
		this.toolsView = toolsView;
		
		// TODO make this configurable
		scaleView.setBarWidthRange(Dip.getDip(context, 40), Dip.getDip(context, 100));
		
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
		//this.getOptions().setDoubleClickZoomIn(true);
		//this.getOptions().setDualClickZoomOut(true);
		
		 // set sky bitmap - optional, default - white
		this.getOptions().setSkyDrawMode(Options.DRAW_BITMAP);
		this.getOptions().setSkyOffset(4.86f);
		this.getOptions().setSkyBitmap(
                UnscaledBitmapLoader.decodeResource(getResources(),
                        R.drawable.sky_small));
		
		// Map background, visible if no map tiles loaded - optional, default - white
		this.getOptions().setBackgroundPlaneDrawMode(Options.DRAW_BITMAP);
		this.getOptions().setBackgroundPlaneBitmap(
                UnscaledBitmapLoader.decodeResource(getResources(),
                        R.drawable.background_plane));
		this.getOptions().setClearColor(Color.WHITE);
		
		// configure texture caching - optional, suggested 
        this.getOptions().setTextureMemoryCacheSize(40 * 1024 * 1024);
        this.getOptions().setCompressedMemoryCacheSize(8 * 1024 * 1024);
        
        // TODO find out how this works? can we pass different paths for different maps?
        //this.getOptions().setPersistentCachePath(activity.getDatabasePath("mapcache").getPath());
        // set persistent raster cache limit to 100MB
        //this.getOptions().setPersistentCacheSize(100 * 1024 * 1024);
	}

	public static int nextId() {
		return cacheId;
	}
	
	public int addVectorLayer(GeometryLayer layer) {
		this.getLayers().addLayer(layer);
		vectorLayerArray.put(vectorId, layer);
		return vectorId++;
	}

	public void removeVectorLayer(int id) {
		this.getLayers().removeLayer(vectorLayerArray.get(id));
		vectorLayerArray.remove(id);
	}
	
	public void removeVectorLayer(String layerName) {
		for(int i = 0; i < vectorLayerArray.size(); i++) {
			int key = vectorLayerArray.keyAt(i);
			GeometryLayer layer = vectorLayerArray.get(key);
			if (getLayerName(layer).equals(layerName)) {
				vectorLayerArray.remove(key);
				break;
			}
		}
	}

	public GeometryLayer getVectorLayer(int layerId) {
		return vectorLayerArray.get(layerId);
	}

	public void setLayerVisible(int layerId, boolean visible) {
		GeometryLayer layer = vectorLayerArray.get(layerId);
		layer.setVisible(visible);
	}

	public void setViewLocked(boolean lock) {
		if (lock) {
			this.getConstraints().setTiltRange(new Range(90.0f, 90.0f));
			this.setTilt(90.0f);
		} else {
			this.getConstraints().setTiltRange(Constraints.DEFAULT_TILT_RANGE);
		}
	}

	public int getGeometryId(Geometry geometry) {
		for(int i = 0; i < vectorLayerArray.size(); i++) {
			int key = vectorLayerArray.keyAt(i);
			GeometryLayer layer = vectorLayerArray.get(key);
			if (layer instanceof CanvasLayer) {
				int geomId = ((CanvasLayer) layer).getGeometryId(geometry);
				if (geomId > 0) return geomId;
			}
		}
		return 0;
	}

	public Geometry getGeometry(int geomId) {
		for(int i = 0; i < vectorLayerArray.size(); i++) {
			int key = vectorLayerArray.keyAt(i);
			GeometryLayer layer = vectorLayerArray.get(key);
			if (layer instanceof CanvasLayer) {
				Geometry geom = ((CanvasLayer) layer).getGeometry(geomId);
				if (geom != null) return geom;
			}
		}
		return null;
	}

	public void drawGeometrOverlay(Geometry geom) {
		if (geom == null) {
			this.overlayGeometry = null;
			drawView.drawGeometry(null);
		} else {
			this.overlayGeometry = GeometryUtil.worldToScreen(geom, this);
			drawView.drawGeometry(overlayGeometry);
		}
	}
	
	public void replaceGeometryOverlay(int geomId) throws MapException {
		if (this.getGeometry(geomId) == null) {
			throw new MapException("Cannot find geometry overlay");
		}
		
		for(int i = 0; i < vectorLayerArray.size(); i++) {
			int key = vectorLayerArray.keyAt(i);
			GeometryLayer layer = vectorLayerArray.get(key);
			if (layer instanceof CanvasLayer) {
				CanvasLayer canvas = (CanvasLayer) layer;
				Geometry geom = canvas.getGeometry(geomId);
				if (geom != null) {
					canvas.replaceGeometry(geomId, GeometryUtil.screenToWorld(overlayGeometry, this));
					updateRenderer();
				}
			}
		}
	}
	
	public static void registerLicense(Context context){
    	final String LICENSE = "XTUMwQ0ZIRklrbEZ2T0dIdkZ3QkRieVBtcWJqdjZ1RUtBaFVBa1RreXdabUIraER4UjFmZ01aUk5oay83a2hzPQoKcGFja2FnZU5hbWU9YXUub3JnLmludGVyc2VjdC5mYWltcy5hbmRyb2lkCndhdGVybWFyaz1jdXN0b20KCg==";
		CustomMapView.registerLicense(LICENSE, context);
        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        CustomMapView.setWatermark(logo, -1.0f, -1.0f, 0.2f);
	}
	
	public void updateMapOverlay() {
		northView.setMapRotation(this.getRotation());
		int width = this.getWidth();
		int height = this.getHeight();
		
		scaleView.setMapBoundary(this.getZoom(), width, height, 
				distance(convertToWgs84(this.screenToWorld(0,  height, 0)), convertToWgs84(this.screenToWorld(width, height, 0))));
	}
	
	private double distance(MapPos p1, MapPos p2) {
		float[] results = new float[3];
		Location.distanceBetween(p1.y, p1.x, p2.y, p2.x, results);
		return results[0] / 1000;
	}
	
	private MapPos convertToWgs84(MapPos p) {
		return (new EPSG3857()).toWgs84(p.x, p.y);
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
			while(true) {
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

	public void addRasterMap(String layerName, String file) throws Exception {
		if (!new File(file).exists()) {
			throw new MapException("Error map does not exist " + file);
		}
		
		validateLayerName(layerName);
		
		CustomGdalMapLayer gdalLayer = new CustomGdalMapLayer(layerName, new EPSG3857(), 0, 18, CustomMapView.nextId(), file, this, true);
        gdalLayer.setShowAlways(true);
        this.getLayers().setBaseLayer(gdalLayer);
	}
	
	public void setMapFocusPoint(float longitude, float latitude) throws Exception {
		if (latitude < -90.0f || latitude > 90.0f) {
			throw new MapException("Error map latitude out of range " + latitude);
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
		
		CustomOgrLayer ogrLayer = new CustomOgrLayer(layerName, new EPSG3857(), file, null, 
				FaimsSettings.MAX_VECTOR_OBJECTS, pointStyleSet, lineStyleSet, polygonStyleSet);
        // ogrLayer.printSupportedDrivers();
        // ogrLayer.printLayerDetails(table);
		return addVectorLayer(ogrLayer);
	}

	public int addSpatialLayer(String layerName, String file, String tablename,
			String labelColumn, StyleSet<PointStyle> pointStyleSet,
			StyleSet<LineStyle> lineStyleSet,
			StyleSet<PolygonStyle> polygonStyleSet) throws Exception {
		if (!new File(file).exists()) {
			throw new MapException("Error file does not exist " + file);
		}
		
		validateLayerName(layerName);
		
		CustomSpatialiteLayer spatialLayer = new CustomSpatialiteLayer(layerName, new EPSG3857(), file, tablename, "Geometry",
                new String[]{labelColumn}, FaimsSettings.MAX_VECTOR_OBJECTS, pointStyleSet, lineStyleSet, polygonStyleSet);
		return addVectorLayer(spatialLayer);
	}

	public int drawPoint(int layerId, MapPos point,
			StyleSet<PointStyle> styleSet) {
		CanvasLayer canvas = (CanvasLayer) this.getVectorLayer(layerId);
		
		int id = canvas.addPoint(point, styleSet);
		updateRenderer();
		return id;
	}
	
	private CanvasLayer getVectorLayerForGeom(Geometry geom) {
		for(int i = 0; i < vectorLayerArray.size(); i++) {
			int key = vectorLayerArray.keyAt(i);
			GeometryLayer layer = vectorLayerArray.get(key);
			if (layer instanceof CanvasLayer) {
				if (((CanvasLayer) layer).hasGeometry(geom)) {
					return (CanvasLayer) layer;
				}
			}
		}
		return null;
	}
	
	private CanvasLayer getVectorLayerForGeomId(int geomId) {
		for(int i = 0; i < vectorLayerArray.size(); i++) {
			int key = vectorLayerArray.keyAt(i);
			GeometryLayer layer = vectorLayerArray.get(key);
			if (layer instanceof CanvasLayer) {
				if (((CanvasLayer) layer).hasGeometryId(geomId)) {
					return (CanvasLayer) layer;
				}
			}
		}
		return null;
	}
	
	public void restylePoint(Point point, StyleSet<PointStyle> styleSet) {
		CanvasLayer canvas = getVectorLayerForGeom(point);
		canvas.restyleGeometry(point, styleSet);
		updateRenderer();
	}

	public int drawLine(int layerId, List<MapPos> points,
			StyleSet<LineStyle> styleSet) {
		CanvasLayer canvas = (CanvasLayer) this.getVectorLayer(layerId);
		
		int id = canvas.addLine(points, styleSet);
		updateRenderer();
		return id;
	}
	
	public void restyleLine(Line line, StyleSet<LineStyle> styleSet) {
		CanvasLayer canvas = getVectorLayerForGeom(line);
		canvas.restyleGeometry(line, styleSet);
		updateRenderer();
	}

	public int drawPolygon(int layerId, List<MapPos> points,
			StyleSet<PolygonStyle> styleSet) {
		CanvasLayer canvas = (CanvasLayer) this.getVectorLayer(layerId);
		
		int id = canvas.addPolygon(points, styleSet);
		updateRenderer();
		return id;
	}
	
	public void restylePolygon(Polygon polygon, StyleSet<PolygonStyle> styleSet) {
		CanvasLayer canvas = getVectorLayerForGeom(polygon);
		canvas.restyleGeometry(polygon, styleSet);
		updateRenderer();
	}

	public void clearGeometry(int layerId, int geomId) {
		CanvasLayer canvas = (CanvasLayer) this.getVectorLayer(layerId);
		
		canvas.removeGeometry(geomId);
		updateRenderer();
	}
	
	public void clearGeometry(int geomId) {
		CanvasLayer canvas = (CanvasLayer) getVectorLayerForGeomId(geomId);
		
		canvas.removeGeometry(geomId);
		updateRenderer();
	}

	public void clearGeometryList(int layerId, List<Integer> geomList) {
		CanvasLayer canvas = (CanvasLayer) this.getVectorLayer(layerId);
		
		if (geomList.size() > 0) {
			for (Integer geomId : geomList) {
				canvas.removeGeometry(geomId);
			}
			updateRenderer();
		}
	}
	
	public void clearGeometryList(CanvasLayer canvas, List<Integer> geomList) {
		if (geomList.size() > 0) {
			for (Integer geomId : geomList) {
				canvas.removeGeometry(geomId);
			}
			updateRenderer();
		}
	}

	public List<Geometry> getGeometryList(int layerId) {
		CanvasLayer canvas = (CanvasLayer) this.getVectorLayer(layerId);
		
		return canvas.getTransformedGeometryList();
	}

	public Geometry getGeometry(int layerId, int geomId) {
		CanvasLayer canvas = (CanvasLayer) this.getVectorLayer(layerId);
		
		return canvas.getTransformedGeometry(geomId);
	}

	public void drawGeometrOverlay(int geomId) throws Exception {
		Geometry geom = this.getGeometry(geomId);
		if (geom == null) {
			throw new MapException("Cannot find geometry to overlay");
		}
		this.drawGeometrOverlay(geom);
	}

	public List<MapTool> getTools() {
		return tools;
	}

	public void showLayersDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
		LayerManagerView layerManager = new LayerManagerView(this.getContext());
		layerManager.attachToMap(this);
		
		builder.setTitle("Layer Manager");
		builder.setView(layerManager);
		builder.create().show();
	}
	
	private boolean doesVectorLayerExist(String name) {
		for(int i = 0; i < vectorLayerArray.size(); i++) {
			int key = vectorLayerArray.keyAt(i);
			GeometryLayer layer = vectorLayerArray.get(key);
			if (layer instanceof CanvasLayer) {
				if (name.equals(((CanvasLayer) layer).getName())) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void validateLayerName(String name) throws Exception {
		if (name == null || "".equals(name)) {
			throw new MapException("Please specify a name for the layer");
		} else if (doesVectorLayerExist(name)) {
			throw new MapException("Layer " + name + " already exists");
		}
	}

	public int addCanvasLayer(String layerName) throws Exception {
		validateLayerName(layerName);
		
		CanvasLayer layer = new CanvasLayer(layerName, new EPSG3857());
		return addVectorLayer(layer);
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

	public void removeLayer(Layer layer) throws Exception {
		String layerName = getLayerName(layer);
		
		// check if layer is base layer
		CustomGdalMapLayer baseLayer = (CustomGdalMapLayer) getLayers().getBaseLayer();
		if (baseLayer.getName().equals(layerName)) {
			throw new MapException("Cannot remove base layer");
		}
		
		getLayers().removeLayer(layer);
		
		removeVectorLayer(layerName);
		
		if (layer == selectedLayer) selectedLayer = null;
	}

	public void renameLayer(Layer layer, String layerName) throws Exception {
		// check if layer is base layer
		validateLayerName(layerName);
		
		setLayerName(layer, layerName);
	}
	
	private void initTools() {
		tools.add(new CreatePointTool(this.getContext(), this));
		tools.add(new CreateLineTool(this.getContext(), this));
		tools.add(new CreatePolygonTool(this.getContext(), this));
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

	public void setMapListener(CustomMapListener customMapListener) {
		this.mapListener = customMapListener;
	}

	public void setSelectedLayer(Layer layer) {
		selectedLayer = layer;
	}

	public Layer getSelectedLayer() {
		return selectedLayer;
	}
	
	private int getVectorLayerId(Layer layer) {
		for(int i = 0; i < vectorLayerArray.size(); i++) {
			int key = vectorLayerArray.keyAt(i);
			GeometryLayer l = vectorLayerArray.get(key);
			if (l == layer) {
				return key;
			}
		}
		return 0;
	}

	public int drawPoint(CanvasLayer layer, MapPos point,
			StyleSet<PointStyle> styleSet) {
		return drawPoint(getVectorLayerId(layer), point, styleSet);
	}

	public void setSelectedGeometry(Geometry geom) {
		selectedGeom = geom;
	}

	public Geometry getSelectedGeometry() {
		return selectedGeom;
	}
	
	public void updateRenderer() {
		if (getComponents() != null) {
			getComponents().mapRenderers.getMapRenderer().frustumChanged();
		}
	}

	public int drawLine(CanvasLayer layer, ArrayList<MapPos> points,
			StyleSet<LineStyle> styleSet) {
		return drawLine(getVectorLayerId(layer), points, styleSet);
	}

	public int drawPolygon(CanvasLayer layer, ArrayList<MapPos> points,
			StyleSet<PolygonStyle> styleSet) {
		return drawPolygon(getVectorLayerId(layer), points, styleSet);
	}
	
}
