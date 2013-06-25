package au.org.intersect.faims.android.nutiteq;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.GeometrySelection;

import com.nutiteq.components.Envelope;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.projections.Projection;
import com.nutiteq.vectorlayers.GeometryLayer;

public class DatabaseLayer extends GeometryLayer {
	
	protected static final int BOUNDARY_PADDING = 20;
	
	public enum Type {
		ENTITY,
		RELATIONSHIP,
		GPS_TRACK
	}

	protected DatabaseTextLayer textLayer;
	protected boolean textVisible;
	protected DatabaseManager dbmgr;
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
	protected String userid;
	private ArrayList<String> hideGeometryList;

	public DatabaseLayer(int layerId, String name, Projection projection, CustomMapView mapView, Type type, String queryName, String querySql, DatabaseManager dbmgr,
			int maxObjects, GeometryStyle pointStyle,
			GeometryStyle lineStyle,
			GeometryStyle polygonStyle) {
		super(projection);
		this.name = name;
		this.layerId = layerId;
		this.mapView = mapView;
		this.queryName = queryName;
		this.querySql = querySql;
		this.type = type;
		this.dbmgr = dbmgr;
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
	
	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	private void updateTextLayer() {
		if (this.isVisible() && this.textVisible) {
			this.textLayer.setVisible(true);
		} else {
			this.textLayer.setVisible(false);
		}
	}
	
	@Override
	public void calculateVisibleElements(Envelope envelope, int zoom) {
		if (zoom < minZoom) {
	        setVisibleElementsList(null);
	        return;
	    }
		
		try {
			ArrayList<MapPos> pts = getMapBoundaries();
			Vector<Geometry> objectTemp = null;
			Vector<Geometry> objects = new Vector<Geometry>();
			
			GeometryData.Type dataType;
			if (type == Type.ENTITY) {
				dataType = GeometryData.Type.ENTITY;
				objectTemp = dbmgr.fetchAllVisibleEntityGeometry(pts, querySql, maxObjects);
			} else if (type == Type.RELATIONSHIP) {
				dataType = GeometryData.Type.RELATIONSHIP;
				objectTemp = dbmgr.fetchAllVisibleRelationshipGeometry(pts, querySql, maxObjects);
			}else {
				throw new Exception("database layer has no type");
			}
			
		    createElementsInLayer(zoom, objectTemp, objects, dataType);
		    
		    //FLog.d("visible objects " + objects.size());
		    
		    setVisibleElementsList(objects);
		} catch (Exception e) {
			FLog.e("error rendering database layer", e);
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
	
	protected ArrayList<MapPos> getMapBoundaries() {
			MapPos p1 = GeometryUtil.convertToWgs84(GeometryUtil.transformVertex(new MapPos(-BOUNDARY_PADDING, -BOUNDARY_PADDING), mapView, false));
			MapPos p2 = GeometryUtil.convertToWgs84(GeometryUtil.transformVertex(new MapPos(mapView.getWidth() + BOUNDARY_PADDING, -BOUNDARY_PADDING), mapView, false));
			MapPos p3 = GeometryUtil.convertToWgs84(GeometryUtil.transformVertex(new MapPos(mapView.getWidth() + BOUNDARY_PADDING, mapView.getHeight() + BOUNDARY_PADDING), mapView, false));
			MapPos p4 = GeometryUtil.convertToWgs84(GeometryUtil.transformVertex(new MapPos(-BOUNDARY_PADDING, mapView.getHeight() + BOUNDARY_PADDING), mapView, false));
			MapPos p5 = p1;
			ArrayList<MapPos> pts = new ArrayList<MapPos>();
			pts.add(p1);
			pts.add(p2);
			pts.add(p3);
			pts.add(p4);
			pts.add(p5);
			return pts;
		}

	public void hideGeometry(String id) {
		hideGeometryList.add(id);
	}
	
	public void clearHiddenList() {
		hideGeometryList.clear();
	}

}
