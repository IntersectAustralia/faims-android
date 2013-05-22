package au.org.intersect.faims.android.nutiteq;

import java.util.Vector;

import au.org.intersect.faims.android.database.DatabaseManager;
import au.org.intersect.faims.android.log.FLog;
import au.org.intersect.faims.android.ui.map.CustomMapView;

import com.nutiteq.components.Envelope;
import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.projections.Projection;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.vectorlayers.GeometryLayer;

public class DatabaseLayer extends GeometryLayer {
	
	private static final int BOUNDARY_PADDING = 20;
	
	public enum Type {
		ENTITY,
		RELATIONSHIP
	}

	private DatabaseTextLayer textLayer;
	private boolean textVisible;
	private DatabaseManager dbmgr;
	private StyleSet<PointStyle> pointStyleSet;
	private StyleSet<LineStyle> lineStyleSet;
	private StyleSet<PolygonStyle> polygonStyleSet;
	private int maxObjects;
	private int minZoom;
	private Type type;
	private String name;
	private int layerId;
	private String queryName;
	private String querySql;
	private CustomMapView mapView;

	public DatabaseLayer(int layerId, String name, Projection projection, CustomMapView mapView, Type type, String queryName, String querySql, DatabaseManager dbmgr,
			int maxObjects, StyleSet<PointStyle> pointStyleSet,
			StyleSet<LineStyle> lineStyleSet,
			StyleSet<PolygonStyle> polygonStyleSet) {
		super(projection);
		this.name = name;
		this.layerId = layerId;
		this.mapView = mapView;
		this.queryName = queryName;
		this.querySql = querySql;
		this.type = type;
		this.dbmgr = dbmgr;
		this.pointStyleSet = pointStyleSet;
	    this.lineStyleSet = lineStyleSet;
	    this.polygonStyleSet = polygonStyleSet;
	    this.maxObjects = maxObjects;
		if (pointStyleSet != null) {
	      minZoom = pointStyleSet.getFirstNonNullZoomStyleZoom();
	    }
	    if (lineStyleSet != null) {
	      minZoom = lineStyleSet.getFirstNonNullZoomStyleZoom();
	    }
	    if (polygonStyleSet != null) {
	      minZoom = polygonStyleSet.getFirstNonNullZoomStyleZoom();
	    }
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
	
	@Override
	public void calculateVisibleElements(Envelope envelope, int zoom) {
		if (zoom < minZoom) {
	        setVisibleElementsList(null);
	        return;
	    }
		
		try {
			MapPos min = GeometryUtil.convertToWgs84(GeometryUtil.transformVertex(new MapPos(-BOUNDARY_PADDING, -BOUNDARY_PADDING), mapView, false));
			MapPos max = GeometryUtil.convertToWgs84(GeometryUtil.transformVertex(new MapPos(mapView.getWidth() + BOUNDARY_PADDING, mapView.getHeight() + BOUNDARY_PADDING), mapView, false));
			Vector<Geometry> objectTemp = null;
			Vector<Geometry> objects = new Vector<Geometry>();
			
			if (type == Type.ENTITY) {
				 objectTemp = dbmgr.fetchAllVisibleEntityGeometry(min, max, querySql, maxObjects);
			} else if (type == Type.RELATIONSHIP) {
				// TODO
			} else {
				throw new Exception("database layer has no type");
			}
			
		    // apply styles, create new objects for these
		    for(Geometry object: objectTemp){
		        
		        Geometry newObject = null;
		        
		        if(object instanceof Point){
		            newObject = new Point(((Point) object).getMapPos(), null, pointStyleSet, object.userData);
		        }else if(object instanceof Line){
		            newObject = new Line(((Line) object).getVertexList(), null, lineStyleSet, object.userData);
		        }else if(object instanceof Polygon){
		            newObject = new Polygon(((Polygon) object).getVertexList(), ((Polygon) object).getHolePolygonList(), null, polygonStyleSet, object.userData);
		        }
		        
		        Geometry transformedObject = GeometryUtil.convertGeometryFromWgs84(newObject);
		        
		        transformedObject.attachToLayer(this);
		        transformedObject.setActiveStyle(zoom);
		        
		        objects.add(transformedObject);
		    }
		    
		    FLog.d("visible objects " + objects.size());
		    
		    setVisibleElementsList(objects);
		} catch (Exception e) {
			FLog.e("error rendering database layer", e);
		}
	}

}
