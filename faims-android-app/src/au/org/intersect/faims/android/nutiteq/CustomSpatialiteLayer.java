package au.org.intersect.faims.android.nutiteq;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.exceptions.MapException;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.GeometrySelection;

import com.nutiteq.components.Envelope;
import com.nutiteq.components.MapPos;
import com.nutiteq.db.DBLayer;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.log.Log;
import com.nutiteq.projections.Projection;
import com.nutiteq.tasks.Task;
import com.nutiteq.vectorlayers.GeometryLayer;

public class CustomSpatialiteLayer extends GeometryLayer {

	private String name;
	private int layerId;
	private String dbPath;
	private String tableName;
	private SpatialiteTextLayer textLayer;
	private boolean textVisible;

	private CustomSpatialLiteDb spatialLite;
	private DBLayer dbLayer;

	private GeometryStyle pointStyle;
	private GeometryStyle lineStyle;
	private GeometryStyle polygonStyle;

	private int minZoom;
	private int maxObjects;
	private String[] userColumns;

	private CustomMapView mapView;

	private int autoSimplifyPixels;
	private int screenWidth;
	
	private boolean renderAll;
	private boolean renderOnce;

	public CustomSpatialiteLayer(int layerId, String name, Projection proj, CustomMapView mapView, String dbPath,
			String tableName, String geomColumnName, String[] userColumns,
			int maxObjects, GeometryStyle pointStyle,
			GeometryStyle lineStyle,
			GeometryStyle polygonStyle) {
		super(proj);
		this.name = name;
		this.layerId = layerId;
		this.dbPath = dbPath;
		this.tableName = tableName;
		this.mapView = mapView;

		this.userColumns = userColumns;
		this.pointStyle = pointStyle;
		this.lineStyle = lineStyle;
		this.polygonStyle = polygonStyle;
		this.maxObjects = maxObjects;

		if (pointStyle != null) {
			minZoom = pointStyle.toPointStyleSet().getFirstNonNullZoomStyleZoom();
		}
		if (lineStyle != null) {
			minZoom = lineStyle.toLineStyleSet().getFirstNonNullZoomStyleZoom();
		}
		if (polygonStyle != null) {
			minZoom = polygonStyle.toPolygonStyleSet().getFirstNonNullZoomStyleZoom();
		}

		spatialLite = new CustomSpatialLiteDb(dbPath);
		Map<String, DBLayer> dbLayers = spatialLite.qrySpatialLayerMetadata();
		for (String layerKey : dbLayers.keySet()) {
			DBLayer layer = dbLayers.get(layerKey);
			if (layer.table.equalsIgnoreCase(tableName)
					&& layer.geomColumn.equalsIgnoreCase(geomColumnName)) {
				this.dbLayer = layer;
				break;
			}
		}

		if (this.dbLayer == null) {
			Log.error("SpatialiteLayer: Could not find a matching layer " + tableName + "." + geomColumnName);
		}

		// fix/add SDK SRID definition for conversions
		spatialLite.defineEPSG3857();
	}

	public String getName() {
		return name;
	}

	public void setName(String layerName) {
		this.name = layerName;
	}

	public int getLayerId() {
		return layerId;
	}

	public String getDbPath() {
		return dbPath;
	}

	public String getTableName() {
		return tableName;
	}

	public String getIdColumn() {
		return userColumns[0];
	}

	public String getLabelColumn() {
		return userColumns[1];
	}

	public String getGeometryColumn() {
		return dbLayer.geomColumn;
	}

	public SpatialiteTextLayer getTextLayer() {
		return textLayer;
	}

	public void setTextLayer(SpatialiteTextLayer textLayer) {
		this.textLayer = textLayer;
		this.textVisible = textLayer.isVisible();
	}

	public boolean getTextVisible() {
		return textVisible;
	}

	public void setTextVisible(boolean visible) {
		this.textVisible = visible;
		updateTextLayer();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		updateTextLayer();
	}

	private void updateTextLayer() {
		if (this.isVisible() && this.textVisible) {
			this.textLayer.setVisible(true);
		} else {
			this.textLayer.setVisible(false);
		}
	}

	public int getMaxObjects() {
		return maxObjects;
	}

	public void setMaxObjects(int value) {
		this.maxObjects = value;
	}

	public void add(Geometry element) {
		throw new UnsupportedOperationException();
	}

	public void remove(Geometry element) {
		throw new UnsupportedOperationException();
	}
	
	public void renderAllVectors(boolean value) {
		renderAll = value;
	}
	
	public void renderOnce() {
		renderOnce = true;
	}

	@Override
	public void calculateVisibleElements(Envelope envelope, int zoom) {
		if (dbLayer == null) {
			return;
		}

		if (zoom < minZoom) {
			setVisibleElementsList(null);
			return;
		}

		if (renderOnce) {
			renderOnce = false;
			executeVisibilityCalculationTask(new LoadDataTask(envelope,zoom));
		}
	}

	protected GeometryStyle getGeometryStyle(Geometry geom) {
		if (geom instanceof Point) {
			return pointStyle;
		} else if (geom instanceof Line) {
			return lineStyle;
		} else if (geom instanceof Polygon) {
			return polygonStyle;
		}
		return null;
	}

	protected GeometryStyle getGeometryStyle(Geometry geom, String id) {
		List<GeometrySelection> selections = mapView.getSelections();
		for (GeometrySelection set : selections) {
			if (set.isActive() && set.hasData(id)) {
				if (geom instanceof Point) {
					return set.getPointStyle();
				} else if (geom instanceof Line) {
					return set.getLineStyle();
				} else if (geom instanceof Polygon) {
					return set.getPolygonStyle();
				}
			}
		}
		return getGeometryStyle(geom);
	}

	@Override
	public Envelope getDataExtent() {
		return spatialLite.qryDataExtent(dbLayer);
	}

	public int getAutoSimplify() {
		return autoSimplifyPixels;
	}

	public void setAutoSimplify(int autoSimplifyPixels, int screenWidth) {
		this.autoSimplifyPixels = autoSimplifyPixels;
		this.screenWidth = screenWidth;
	}

	protected class LoadDataTask implements Task {
		final Envelope envelope;
		final int zoom;

		LoadDataTask(Envelope envelope, int zoom) {
			this.envelope = envelope;
			this.zoom = zoom;
		}

		@Override
		public void run() {
			loadData(envelope, zoom);
		}

		@Override
		public boolean isCancelable() {
			return true;
		}

		@Override
		public void cancel() {
		}
	}

	public void loadData(Envelope envelope, int zoom) {
		//long time = System.currentTimeMillis();
		
		try {
			MapPos bottomLeft = projection.fromInternal(envelope.getMinX(), envelope.getMinY());
			MapPos topRight = projection.fromInternal(envelope.getMaxX(), envelope.getMaxY());
			Vector<Geometry> objectTemp = spatialLite.qrySpatiaLiteGeom(new Envelope(bottomLeft.x, topRight.x,
					bottomLeft.y, topRight.y), renderAll ? FaimsSettings.MAX_VECTOR_OBJECTS : maxObjects, dbLayer, userColumns, autoSimplifyPixels, screenWidth);

			Vector<Geometry> objects = new Vector<Geometry>();

			// apply styles, create new objects for these
			for(Geometry object: objectTemp){
				GeometryData geomData = null;
				GeometryStyle style = null;

				if (userColumns != null) {
					@SuppressWarnings("unchecked")
					final Map<String, String> userData = (Map<String, String>) object.userData;
					// note: the id column is not unique so adding prepending db path + table name
					String id = dbPath + ":" + tableName + ":" + userData.get(userColumns[0]);
					String label = userData.get(userColumns[1]);
					style = getGeometryStyle(object, id);
					geomData = new GeometryData(id, GeometryData.Type.LEGACY, label, style, layerId);
				} else {
					style = getGeometryStyle(object);
				}

				Geometry newObject = null;

				if(object instanceof Point){
					newObject = new Point(((Point) object).getMapPos(), null, style.toPointStyleSet(), geomData);
				}else if(object instanceof Line){
					newObject = new Line(((Line) object).getVertexList(), null, style.toLineStyleSet(), geomData);
				}else if(object instanceof Polygon){
					newObject = new Polygon(((Polygon) object).getVertexList(), ((Polygon) object).getHolePolygonList(), null, style.toPolygonStyleSet(), geomData);
				}

				newObject.attachToLayer(this);
				newObject.setActiveStyle(zoom);

				objects.add(newObject);
			}

			setVisibleElementsList(objects);
		} catch (Exception e) {
			FLog.e("error rendering spatialite layer", e);
		}

		if (textLayer != null) {
			textLayer.renderOnce();
			textLayer.calculateVisibleElements(envelope, zoom);
		}
		
		//FLog.d("time: " + (System.currentTimeMillis() - time) / 1000);
	}

	public void raiseInvalidLayer() throws MapException {
		Map<String, DBLayer> dbLayers = spatialLite.qrySpatialLayerMetadata();
		DBLayer dbLayer = null;
		for (String layerKey : dbLayers.keySet()) {
			DBLayer layer = dbLayers.get(layerKey);
			if (layer.table.equalsIgnoreCase(getTableName())
					&& layer.geomColumn.equalsIgnoreCase(getGeometryColumn())) {
				dbLayer = layer;
				break;
			}
		}
		if(!spatialLite.checkValidGeometry(dbLayer)){
			throw new MapException("Table contains unsupported geometry");
		}
	}

	public void saveToJSON(JSONObject json) {
		try {
			json.put("name", getName());
			json.put("dbPath", getDbPath());
			json.put("type", "CustomSpatialiteLayer");
			json.put("maxObjects", getMaxObjects());
			JSONObject point = new JSONObject();
			pointStyle.saveToJSON(point);
			json.put("pointStyle", point);
			JSONObject line = new JSONObject();
			pointStyle.saveToJSON(point);
			json.put("lineStyle", line);
			JSONObject polygon = new JSONObject();
			pointStyle.saveToJSON(point);
			json.put("polygonStyle", polygon);
			json.put("tableName", getTableName());
			json.put("idColumn", getIdColumn());
			json.put("labelColumn", getLabelColumn());
			if (getTextLayer() != null) {
				JSONObject textLayer = new JSONObject();
				getTextLayer().saveToJSON(textLayer);
				json.put("textLayer", textLayer);
			}
			json.put("visible", isVisible());
		} catch (JSONException e) {
			FLog.e("Couldn't serialize CustomSpatialiteLayer", e);
		}
	}

}
