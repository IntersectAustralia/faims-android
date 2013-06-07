package au.org.intersect.faims.android.nutiteq;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import au.org.intersect.faims.android.ui.map.CustomMapView;
import au.org.intersect.faims.android.ui.map.GeometrySelection;

import com.nutiteq.components.Envelope;
import com.nutiteq.components.MapPos;
import com.nutiteq.db.DBLayer;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.layers.vector.SpatialLiteDb;
import com.nutiteq.log.Log;
import com.nutiteq.projections.Projection;
import com.nutiteq.vectorlayers.GeometryLayer;

public class CustomSpatialiteLayer extends GeometryLayer {

	private String name;
	private int layerId;
	private String dbPath;
	private String tableName;
	private SpatialiteTextLayer textLayer;
	private boolean textVisible;
	
	private SpatialLiteDb spatialLite;
	private DBLayer dbLayer;

	private GeometryStyle pointStyle;
	private GeometryStyle lineStyle;
	private GeometryStyle polygonStyle;

	private int minZoom;
	private int maxObjects;
	private String[] userColumns;
	
	private CustomMapView mapView;

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

	    spatialLite = new SpatialLiteDb(dbPath);
	    Vector<DBLayer> dbLayers = spatialLite.qrySpatialLayerMetadata();
	    for (DBLayer dbLayer : dbLayers) {
	      if (dbLayer.table.compareTo(tableName) == 0 && dbLayer.geomColumn.compareTo(geomColumnName) == 0) {
	        this.dbLayer = dbLayer;
	        break;
	      }
	    }

	    if (this.dbLayer == null) {
	      Log.error("SpatialiteLayer: Could not find a matching layer " + tableName + "." + geomColumnName);
	    }
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
	
	  public void add(Geometry element) {
	    throw new UnsupportedOperationException();
	  }
	
	  public void remove(Geometry element) {
	    throw new UnsupportedOperationException();
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
	
	    MapPos bottomLeft = projection.fromInternal(envelope.getMinX(), envelope.getMinY());
	    MapPos topRight = projection.fromInternal(envelope.getMaxX(), envelope.getMaxY());
	    Vector<Geometry> objectTemp = spatialLite.qrySpatiaLiteGeom(new Envelope(bottomLeft.x, topRight.x,
	        bottomLeft.y, topRight.y), maxObjects, dbLayer, userColumns);
	    
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
		        geomData = new GeometryData(id, label, style, layerId);
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

}
