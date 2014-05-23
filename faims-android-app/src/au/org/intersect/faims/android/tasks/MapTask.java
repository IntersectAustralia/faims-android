package au.org.intersect.faims.android.tasks;

import java.util.Map;

import android.os.AsyncTask;
import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.nutiteq.CanvasLayer;
import au.org.intersect.faims.android.nutiteq.CustomGdalMapLayer;
import au.org.intersect.faims.android.nutiteq.CustomSpatialLiteDb;
import au.org.intersect.faims.android.nutiteq.CustomSpatialiteLayer;
import au.org.intersect.faims.android.nutiteq.DatabaseLayer;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.util.GeometryUtil;

import com.google.inject.Inject;
import com.nutiteq.components.MapPos;
import com.nutiteq.db.DBLayer;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.layers.Layer;

public class MapTask extends AsyncTask<Void, Void, Void> {

	@Inject
	DatabaseManager databaseManager;

	private static final double BUFFER_SCALE = 1.2;
	private CustomMapView mapView;
	private ITaskListener listener;
	private Float longitude = null;
	private Float latitude = null;
	private Float zoomLevel = null;
	private Double min_x = null;
	private Double min_y = null;
	private Double max_x = null;
	private Double max_y = null;
	private boolean showToast = false;

	public MapTask(CustomMapView mapView, ITaskListener listener){
		FAIMSApplication.getInstance().injectMembers(this);
		
		this.mapView = mapView;
		this.listener = listener;
	}
	
	@Override
	protected Void doInBackground(Void... arg0) {
		Layer selectedLayer = this.mapView.getSelectedLayer();
		if ( selectedLayer != null){
			try{
				if(selectedLayer instanceof CustomGdalMapLayer){
					getZoomAndPositionFromGdalLayer((CustomGdalMapLayer)selectedLayer);
				}else if(selectedLayer instanceof CanvasLayer){
					getZoomAndPositionFromCanvasLayer((CanvasLayer)selectedLayer);
				}else if(selectedLayer instanceof DatabaseLayer){
					getZoomAndPositionFromDatabaseLayer((DatabaseLayer)selectedLayer);
				}else if(selectedLayer instanceof CustomSpatialiteLayer){
					getZoomAndPositionFromSpatialiteLayer((CustomSpatialiteLayer) selectedLayer);
				}
			}catch(Exception e){
				FLog.e("exception when obtaining data from layer", e);
			}
		}
		return null;
	}

	private void getZoomAndPositionFromGdalLayer(CustomGdalMapLayer gdalMapLayer) throws Exception {
		double[][] boundaries = gdalMapLayer.getBoundary();
		longitude = ((float)boundaries[0][0]+(float)boundaries[3][0])/2;
		latitude = ((float)boundaries[0][1]+(float)boundaries[3][1])/2;
		double mapWidth = databaseManager.spatialRecord().computePointDistance(
				new MapPos(boundaries[0][0],boundaries[0][1]), 
				new MapPos(boundaries[2][0],boundaries[2][1]), 
				GeometryUtil.EPSG4326) / 1000.0;
		double mapHeight = databaseManager.spatialRecord().computePointDistance(
				new MapPos(boundaries[0][0],boundaries[0][1]), 
				new MapPos(boundaries[1][0],boundaries[1][1]), 
				GeometryUtil.EPSG4326) / 1000.0;
		getZoomLevel( mapWidth, mapHeight, gdalMapLayer.getBestZoom());
	}

	private void getZoomAndPositionFromCanvasLayer(CanvasLayer canvasLayer) throws Exception {
		
		if(canvasLayer.getGeometryList() != null){
			for(Geometry geometry : canvasLayer.getGeometryList()){
				if(geometry instanceof Point){
					Point p = (Point) geometry;
					setMinMax(GeometryUtil.convertToWgs84(p.getMapPos()));
				}else if(geometry instanceof Line){
					Line line = (Line) geometry;
					for (MapPos mapPos : line.getVertexList()) {
						setMinMax(GeometryUtil.convertToWgs84(mapPos));
					}
				}else{
					Polygon polygon = (Polygon) geometry;
					for (MapPos mapPos : polygon.getVertexList()) {
						setMinMax(GeometryUtil.convertToWgs84(mapPos));
					}
				}
			}
			setPositionAndZoom();
		}
	}

	private void getZoomAndPositionFromDatabaseLayer(DatabaseLayer databaseLayer) throws Exception {
		Geometry geometry = null;
		if(databaseLayer.getType().equals(DatabaseLayer.Type.ENTITY)){
			geometry = databaseManager.entityRecord().getBoundaryForVisibleEntityGeometry(databaseLayer.getQuerySQL());
		}else if(databaseLayer.getType().equals(DatabaseLayer.Type.RELATIONSHIP)){
			geometry = databaseManager.relationshipRecord().getBoundaryForVisibleRelnGeometry(databaseLayer.getQuerySQL());
		}else if(databaseLayer.getType().equals(DatabaseLayer.Type.GPS_TRACK)){
			geometry = databaseManager.entityRecord().getBoundaryForVisibleEntityGeometry(databaseLayer.getQuerySQL());
		}
		if(geometry != null){
			if(geometry instanceof Polygon){
				Polygon polygon = (Polygon) geometry;
				for (MapPos mapPos : polygon.getVertexList()) {
					setMinMax(mapPos);
				}
			}
			setPositionAndZoom();
		}
	}

	private void getZoomAndPositionFromSpatialiteLayer(
			CustomSpatialiteLayer customSpatialiteLayer) throws Exception {
		CustomSpatialLiteDb spatialLite = new CustomSpatialLiteDb(customSpatialiteLayer.getDbPath());
		Map<String, DBLayer> dbLayers = spatialLite.qrySpatialLayerMetadata();
		DBLayer dbLayer = null;
		for (String layerKey : dbLayers.keySet()) {
			DBLayer layer = dbLayers.get(layerKey);
			if (layer.table.equalsIgnoreCase(customSpatialiteLayer.getTableName())
					&& layer.geomColumn.equalsIgnoreCase(customSpatialiteLayer.getGeometryColumn())) {
				dbLayer = layer;
				break;
			}
		}
		if(dbLayer != null){
			Geometry geometry = spatialLite.getBoundariesFromDataBase(dbLayer);
			if(geometry != null){
				if(geometry instanceof Polygon){
					Polygon polygon = (Polygon) geometry;
					for (MapPos mapPos : polygon.getVertexList()) {
						setMinMax(GeometryUtil.convertToWgs84(mapPos));
					}
					setPositionAndZoom();
				}
			}
		}
		
	}

	private void setPositionAndZoom() throws Exception {
		if(min_x != null && min_y != null && max_x != null && max_y != null){
			longitude = (float) ((min_x + max_x) / 2);
			latitude = (float) ((min_y + max_y) / 2);
			double mapWidth = databaseManager.spatialRecord().computePointDistance(
					new MapPos(min_x,min_y), 
					new MapPos(max_x,min_y), 
					GeometryUtil.EPSG4326) / 1000.0;
			double mapHeight = databaseManager.spatialRecord().computePointDistance(
					new MapPos(min_x,min_y), 
					new MapPos(min_x,max_y), 
					GeometryUtil.EPSG4326) / 1000.0;
			if(!min_x.equals(max_x) || !min_y.equals(max_y)){
				getZoomLevel( mapWidth, mapHeight, null);
			}else{
				zoomLevel = mapView.getZoom();
			}
		}
	}

	private void setMinMax(MapPos mapPos) {
		if(min_x == null && min_y == null && max_x == null && max_y == null){
			min_x = mapPos.x;
			min_y = mapPos.y;
			max_x = mapPos.x;
			max_y = mapPos.y;
		}else{
			if(mapPos.x < min_x){
				min_x = mapPos.x;
			}else if(mapPos.x > max_x){
				max_x = mapPos.x;
			}
			if(mapPos.y < min_y){
				min_y = mapPos.y;
			}else if(mapPos.y > max_y){
				max_y = mapPos.y;
			}
		}
		
	}

	private void getZoomLevel(double mapWidth, double mapHeight, Double bestZoom) throws Exception {
		int width = mapView.getWidth();
		int height = mapView.getHeight();
		double currentWidth = databaseManager.spatialRecord().computePointDistance(
				GeometryUtil.convertToWgs84(mapView.screenToWorld(0, height, 0)), 
				GeometryUtil.convertToWgs84(mapView.screenToWorld(width, height, 0)), 
				GeometryUtil.EPSG4326) / 1000.0;
		double currentHeight = databaseManager.spatialRecord().computePointDistance(
				GeometryUtil.convertToWgs84(mapView.screenToWorld(0, 0, 0)), 
				GeometryUtil.convertToWgs84(mapView.screenToWorld(0, height, 0)), 
				GeometryUtil.EPSG4326) / 1000.0;
		float widthZoomLevel = (float) (mapView.getZoom() - (Math.log(mapWidth * BUFFER_SCALE/currentWidth)/Math.log(2)));
		float heightZoomLevel = (float) (mapView.getZoom() - (Math.log(mapHeight * BUFFER_SCALE/currentHeight)/Math.log(2)));
		zoomLevel = widthZoomLevel > heightZoomLevel ? heightZoomLevel : widthZoomLevel;
		if (bestZoom != null && zoomLevel < bestZoom){
			zoomLevel = Float.valueOf(Double.toString(bestZoom));
			showToast = true;
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		listener.handleTaskCompleted(showToast);
	}

	public Float getLongitude() {
		return longitude;
	}

	public Float getLatitude() {
		return latitude;
	}

	public Float getZoomLevel() {
		return zoomLevel;
	}
}
