package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

import au.org.intersect.faims.android.app.FAIMSApplication;
import au.org.intersect.faims.android.constants.FaimsSettings;
import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.GeometrySelection;
import au.org.intersect.faims.android.util.GeometryUtil;

import com.google.inject.Inject;
import com.nutiteq.components.Envelope;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.projections.Projection;
import com.nutiteq.tasks.Task;
import com.nutiteq.vectorlayers.GeometryLayer;

public class DatabaseLayer extends GeometryLayer {
	
	@Inject
	DatabaseManager databaseManager;

	public enum Type {
		ENTITY,
		RELATIONSHIP,
		GPS_TRACK
	}

	protected DatabaseTextLayer textLayer;
	protected boolean textVisible;
	protected GeometryStyle pointStyle;
	protected GeometryStyle lineStyle;
	protected GeometryStyle polygonStyle;
	protected int maxObjects;
	protected int minZoom;
	protected Type type;
	protected String name;
	protected int layerId;
	protected String queryName;
	protected String querySql;
	protected CustomMapView mapView;
	protected ArrayList<String> hideGeometryList;
	
	protected boolean renderAll;
	protected boolean renderOnce;

	public DatabaseLayer(int layerId, String name, Projection projection, CustomMapView mapView, Type type, String queryName, String querySql,
			int maxObjects, 
			GeometryStyle pointStyle,
			GeometryStyle lineStyle,
			GeometryStyle polygonStyle) {
		super(projection);
		
		FAIMSApplication.getInstance().injectMembers(this);
		
		this.name = name;
		this.layerId = layerId;
		this.mapView = mapView;
		this.queryName = queryName;
		this.querySql = querySql;
		this.type = type;
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

		hideGeometryList = new ArrayList<String>();
	}

	public Type getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public void setName(String layerName) {
		this.name = layerName;
	}

	public int getLayerId() {
		return this.layerId;
	}

	public String getQueryName() {
		return queryName;
	}

	public String getQuerySQL() {
		return querySql;
	}

	public DatabaseTextLayer getTextLayer() {
		return textLayer;
	}

	public void setTextLayer(DatabaseTextLayer textLayer) {
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

	public void renderAllVectors(boolean value) {
		renderAll = value;
	}
	
	public void renderOnce() {
		renderOnce = true;
	}

	@Override
	public void calculateVisibleElements(Envelope envelope, int zoom) {
		if (zoom < minZoom) {
			setVisibleElementsList(null);
			return;
		}
		
		if (renderOnce) {
			renderOnce = false;
			executeVisibilityCalculationTask(new LoadDataTask(envelope,zoom));
		}
	}

	public void createElementsInLayer(int zoom, Vector<Geometry> objectTemp,
			Vector<Geometry> objects, GeometryData.Type dataType) {

		// apply styles, create new objects for these
		for(Geometry object: objectTemp){

			Geometry newObject = null;
			String[] userData = (String[]) object.userData;
			GeometryStyle style = getGeometryStyle(object, userData[0]);
			GeometryData geomData = new GeometryData(userData[0], dataType, userData[1], style, layerId);

			if (hideGeometryList.contains(geomData.id)) continue; 

			if(object instanceof Point){
				newObject = new Point(((Point) object).getMapPos(), null, style.toPointStyleSet(), geomData);
			}else if(object instanceof Line){
				newObject = new Line(((Line) object).getVertexList(), null, style.toLineStyleSet(), geomData);
			}else if(object instanceof Polygon){
				newObject = new Polygon(((Polygon) object).getVertexList(), ((Polygon) object).getHolePolygonList(), null, style.toPolygonStyleSet(), geomData);
			}

			Geometry transformedObject = GeometryUtil.convertGeometryFromWgs84(newObject);

			transformedObject.attachToLayer(this);
			transformedObject.setActiveStyle(zoom);

			objects.add(transformedObject);
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

	public void hideGeometry(String id) {
		hideGeometryList.add(id);
	}

	public void clearHiddenList() {
		hideGeometryList.clear();
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
			ArrayList<MapPos> pts = mapView.getMapBoundaryPts();
			Vector<Geometry> objectTemp = null;
			Vector<Geometry> objects = new Vector<Geometry>();

			GeometryData.Type dataType;
			if (type == Type.ENTITY) {
				dataType = GeometryData.Type.ENTITY;
				objectTemp = databaseManager.fetchRecord().fetchAllVisibleEntityGeometry(pts, querySql, renderAll ? FaimsSettings.MAX_VECTOR_OBJECTS : maxObjects);
			} else if (type == Type.RELATIONSHIP) {
				dataType = GeometryData.Type.RELATIONSHIP;
				objectTemp = databaseManager.fetchRecord().fetchAllVisibleRelationshipGeometry(pts, querySql, renderAll ? FaimsSettings.MAX_VECTOR_OBJECTS : maxObjects);
			}else {
				throw new Exception("database layer has no type");
			}

			createElementsInLayer(zoom, objectTemp, objects, dataType);

			setVisibleElementsList(objects);
			
		} catch (Exception e) {
			FLog.e("error rendering database layer", e);
		}
		
		if (textLayer != null) {
			textLayer.renderOnce();
			textLayer.calculateVisibleElements(envelope, zoom);
		}
		
		//FLog.d("time: " + (System.currentTimeMillis() - time) / 1000);
	}
	
	public void saveToJSON(JSONObject json) {
		try {
			json.put("name", getName());
			json.put("type", "DatabaseLayer");
			json.put("queryName", getQueryName());
			json.put("querySql", getQuerySQL());
			json.put("isEntity", getType() == Type.ENTITY);
			JSONObject point = new JSONObject();
			pointStyle.saveToJSON(point);
			json.put("pointStyle", point);
			JSONObject line = new JSONObject();
			pointStyle.saveToJSON(point);
			json.put("lineStyle", line);
			JSONObject polygon = new JSONObject();
			pointStyle.saveToJSON(point);
			json.put("polygonStyle", polygon);
			json.put("visible", isVisible());
			if (getTextLayer() != null) {
				JSONObject textLayer = new JSONObject();
				getTextLayer().saveToJSON(textLayer);
				json.put("textLayer", textLayer);
			}
		} catch (JSONException e) {
			FLog.e("Couldn't serialize DatabaseLayer", e);
		}
	}

}
